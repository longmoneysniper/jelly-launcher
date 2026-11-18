package app.jellylauncher.data

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val JELLYFIN_PACKAGE = "org.jellyfin.androidtv"

/** Lists the apps installed on the TV so the launcher can show them in the Apps row. */
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

                val banner = info.activityInfo.loadBanner(pm)
                    ?: info.activityInfo.applicationInfo.loadBanner(pm)
                val image = try {
                    if (banner != null) banner.toBitmap(320, 180) else info.loadIcon(pm).toBitmap(160, 160)
                } catch (e: Exception) {
                    continue
                }

                entries += Entry(
                    app = LaunchableApp(
                        label = info.loadLabel(pm).toString(),
                        packageName = pkg,
                        image = image.asImageBitmap(),
                        isBanner = banner != null,
                    ),
                    leanback = isLeanback,
                )
            }
        }

        // Jellyfin first, then real TV apps, then everything else; alphabetical inside each group.
        entries
            .sortedWith(
                compareBy<Entry>(
                    { it.app.packageName != JELLYFIN_PACKAGE },
                    { !it.leanback },
                    { it.app.label.lowercase() },
                )
            )
            .map { it.app }
    }
}
