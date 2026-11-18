package app.jellylauncher.data

import android.content.Context
import java.util.UUID

/** Stores the server address and the access token (never the password). */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("jelly_launcher", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = sp.getString("server", "") ?: ""
        set(value) = sp.edit().putString("server", value).apply()

    var token: String?
        get() = sp.getString("token", null)
        set(value) = sp.edit().putString("token", value).apply()

    var userId: String?
        get() = sp.getString("userId", null)
        set(value) = sp.edit().putString("userId", value).apply()

    var userName: String
        get() = sp.getString("userName", "") ?: ""
        set(value) = sp.edit().putString("userName", value).apply()

    val deviceId: String
        get() {
            sp.getString("deviceId", null)?.let { return it }
            val id = UUID.randomUUID().toString()
            sp.edit().putString("deviceId", id).apply()
            return id
        }

    val isLoggedIn: Boolean
        get() = !token.isNullOrEmpty() && !userId.isNullOrEmpty() && serverUrl.isNotEmpty()

    fun signOut() {
        sp.edit().remove("token").remove("userId").apply()
    }
}
