package app.jellylauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuthExpiredException : IOException("Session expired")

class JellyfinApi(private val prefs: Prefs) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun authHeader(token: String?): String = buildString {
        append("MediaBrowser Client=\"Jelly Launcher\", Device=\"Android TV\", ")
        append("DeviceId=\"${prefs.deviceId}\", Version=\"1.0\"")
        if (token != null) append(", Token=\"$token\"")
    }

    fun normalizeUrl(raw: String): String {
        var url = raw.trim().trimEnd('/')
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "http://$url"
        }
        return url
    }

    /** Signs in and stores server, token and user id. Throws IOException with a readable message. */
    suspend fun login(server: String, username: String, password: String) {
        val base = normalizeUrl(server)
        val body = buildJsonObject {
            put("Username", username)
            put("Pw", password)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$base/Users/AuthenticateByName".toHttpUrl())
            .header("Authorization", authHeader(null))
            .post(body)
            .build()

        val result = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                when {
                    resp.code == 401 -> throw IOException("Wrong username or password")
                    !resp.isSuccessful -> throw IOException("Server answered with HTTP ${resp.code}: ${text.take(150)}")
                    else -> json.decodeFromString<AuthResult>(text)
                }
            }
        }

        prefs.serverUrl = base
        prefs.token = result.accessToken
        prefs.userId = result.user.id
        prefs.userName = result.user.name
    }

    /** Items that are partly watched, most recently played first (Jellyfin's "Continue Watching"). */
    suspend fun continueWatching(): List<JfItem> = withContext(Dispatchers.IO) {
        val userId = prefs.userId ?: throw AuthExpiredException()
        val url = "${prefs.serverUrl}/Users/$userId/Items/Resume".toHttpUrl().newBuilder()
            .addQueryParameter("Limit", "20")
            .addQueryParameter("MediaTypes", "Video")
            .addQueryParameter("Fields", "Overview,Genres")
            .addQueryParameter("EnableUserData", "true")
            .addQueryParameter("EnableImageTypes", "Primary,Backdrop,Thumb")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", authHeader(prefs.token))
            .build()

        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            when {
                resp.code == 401 -> throw AuthExpiredException()
                !resp.isSuccessful -> throw IOException("Server answered with HTTP ${resp.code}: ${text.take(150)}")
                else -> json.decodeFromString<ItemsResponse>(text).items
            }
        }
    }

    // ---- mapping to UI models ----

    fun toCard(item: JfItem): ResumeCard {
        val isEpisode = item.type == "Episode"
        val title = if (isEpisode) item.seriesName ?: item.name else item.name
        val episodeLabel =
            if (item.seasonNumber != null && item.episodeNumber != null) "S${item.seasonNumber}:E${item.episodeNumber}" else null
        val genres = item.genres.take(3).joinToString(" · ")
        val subtitle = if (isEpisode) {
            listOfNotNull(episodeLabel, item.name).joinToString(" · ")
        } else {
            genres.ifEmpty { item.year?.toString().orEmpty() }
        }
        val kicker = if (isEpisode) {
            listOfNotNull("EPISODE", episodeLabel).joinToString(" · ")
        } else {
            listOfNotNull("MOVIE", item.year?.toString()).joinToString(" · ")
        }

        val total = item.runTimeTicks ?: 0L
        val position = item.userData?.playbackPositionTicks ?: 0L
        val progress = if (total > 0) (position.toFloat() / total).coerceIn(0f, 1f) else 0f

        return ResumeCard(
            id = item.id,
            title = title,
            subtitle = subtitle,
            overview = item.overview.orEmpty(),
            progress = progress,
            remaining = remainingText(total - position),
            cardImage = cardImage(item),
            heroImage = heroImage(item),
            poster = poster(item),
            kicker = kicker,
            positionMs = position / 10_000,
        )
    }

    /** Tall poster: the series poster for episodes, the item's own poster for movies. */
    private fun poster(item: JfItem): String? {
        val seriesId = item.seriesId
        val seriesTag = item.seriesPrimaryImageTag
        if (item.type == "Episode" && seriesId != null && seriesTag != null) {
            return image(seriesId, "Primary", seriesTag, 400)
        }
        item.imageTags["Primary"]?.let { return image(item.id, "Primary", it, 400) }
        return null
    }

    private fun remainingText(ticksLeft: Long): String {
        if (ticksLeft <= 0) return ""
        val minutes = (ticksLeft / TICKS_PER_MINUTE).toInt().coerceAtLeast(1)
        return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m left" else "$minutes min left"
    }

    private fun image(itemId: String, type: String, tag: String?, maxWidth: Int): String {
        val path = if (type == "Backdrop") "Backdrop/0" else type
        val tagPart = if (tag != null) "&tag=$tag" else ""
        return "${prefs.serverUrl}/Items/$itemId/Images/$path?maxWidth=$maxWidth$tagPart"
    }

    /** 16:9 picture for the card: thumb, then backdrop (own or series), then primary. */
    private fun cardImage(item: JfItem): String? {
        item.imageTags["Thumb"]?.let { return image(item.id, "Thumb", it, 640) }
        item.backdropImageTags.firstOrNull()?.let { return image(item.id, "Backdrop", it, 640) }
        val parent = item.parentBackdropItemId
        if (parent != null && item.parentBackdropImageTags.isNotEmpty()) {
            return image(parent, "Backdrop", item.parentBackdropImageTags.first(), 640)
        }
        item.imageTags["Primary"]?.let { return image(item.id, "Primary", it, 640) }
        return null
    }

    /** Wide backdrop for the hero area behind the rows. */
    private fun heroImage(item: JfItem): String? {
        item.backdropImageTags.firstOrNull()?.let { return image(item.id, "Backdrop", it, 1920) }
        val parent = item.parentBackdropItemId
        if (parent != null && item.parentBackdropImageTags.isNotEmpty()) {
            return image(parent, "Backdrop", item.parentBackdropImageTags.first(), 1920)
        }
        item.imageTags["Thumb"]?.let { return image(item.id, "Thumb", it, 1920) }
        return null
    }

    private companion object {
        const val TICKS_PER_MINUTE = 600_000_000L
    }
}
