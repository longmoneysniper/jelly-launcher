package app.jellylauncher.data

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val JELLYFIN_PACKAGE = "org.jellyfin.androidtv"
const val WHOLPHIN_PACKAGE = "com.github.damontecres.wholphin"

/** Lists the apps installed on the TV so the launcher can show them in the dock and the All apps screen. */
class AppsRepository(private val context: Context) {

    private class Entry(val app: LaunchableApp, val leanback: Boolean)

    suspend fun load(): List<LaunchableApp> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val leanback = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0
        )
        val regular = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )

        val seen = mutableSetOf(context.packageName)
        val entries = mutableListOf<Entry>()

        for ((isLeanback, list) in listOf(true to leanback, false to regular)) {
            for (info in list) {
                val pkg = info.activityInfo.packageName
                if (!seen.add(pkg)) continue

                val icon = try {
                    info.loadIcon(pm).toBitmap(128, 128)
                } catch (e: Exception) {
                    continue
                }
                val banner = try {
                    val drawable = info.activityInfo.loadBanner(pm)
                        ?: info.activityInfo.applicationInfo.loadBanner(pm)
                    drawable?.toBitmap(320, 180)
                } catch (e: Exception) {
                    null
                }

                entries += Entry(
                    app = LaunchableApp(
                        label = info.loadLabel(pm).toString(),
                        packageName = pkg,
                        icon = icon.asImageBitmap(),
                        banner = banner?.asImageBitmap(),
                    ),
                    leanback = isLeanback,
                )
            }
        }

        // Jellyfin first, then real TV apps, then everything else; alphabetical inside each group.
        entries
            .sortedWith(
                compareBy<Entry>(
                    { it.app.packageName != WHOLPHIN_PACKAGE },
                    { it.app.packageName != JELLYFIN_PACKAGE },
                    { !it.leanback },
                    { it.app.label.lowercase() },
                )
            )
            .map { it.app }
    }
}
