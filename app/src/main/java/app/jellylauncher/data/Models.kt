package app.jellylauncher.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---- Jellyfin API payloads (only the fields we use) ----

@Serializable
data class AuthResult(
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("User") val user: JfUser,
)

@Serializable
data class JfUser(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String = "",
)

@Serializable
data class ItemsResponse(
    @SerialName("Items") val items: List<JfItem> = emptyList(),
)

@Serializable
data class JfItem(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String = "",
    @SerialName("Type") val type: String = "",
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("ParentIndexNumber") val seasonNumber: Int? = null,
    @SerialName("IndexNumber") val episodeNumber: Int? = null,
    @SerialName("ProductionYear") val year: Int? = null,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("ImageTags") val imageTags: Map<String, String> = emptyMap(),
    @SerialName("BackdropImageTags") val backdropImageTags: List<String> = emptyList(),
    @SerialName("ParentBackdropItemId") val parentBackdropItemId: String? = null,
    @SerialName("ParentBackdropImageTags") val parentBackdropImageTags: List<String> = emptyList(),
    @SerialName("UserData") val userData: JfUserData? = null,
)

@Serializable
data class JfUserData(
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0,
)

// ---- UI models ----

data class ResumeCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val overview: String,
    val progress: Float,
    val remaining: String,
    val cardImage: String?,
    val heroImage: String?,
)

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val image: ImageBitmap,
    val isBanner: Boolean,
)
