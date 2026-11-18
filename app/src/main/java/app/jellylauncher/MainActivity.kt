package app.jellylauncher

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.jellylauncher.data.JELLYFIN_PACKAGE
import app.jellylauncher.data.LaunchableApp
import app.jellylauncher.data.ResumeCard
import app.jellylauncher.ui.HomeScreen
import app.jellylauncher.ui.JellyTheme
import app.jellylauncher.ui.LoginScreen

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JellyTheme {
                val state by viewModel.state.collectAsState()

                if (state.showLogin) {
                    LoginScreen(
                        initialServer = state.serverUrl,
                        error = state.loginError,
                        busy = state.loggingIn,
                        canCancel = state.serverUrl.isNotEmpty() && !state.loading,
                        onSubmit = viewModel::login,
                        onCancel = viewModel::closeSettings,
                    )
                } else {
                    HomeScreen(
                        state = state,
                        onPlay = ::openInJellyfin,
                        onLaunchApp = ::launchApp,
                        onSettings = viewModel::openSettings,
                        onRetry = viewModel::refresh,
                    )
                }
            }
        }
    }

    // Home was pressed (or we returned from another app): refresh Continue Watching and the app list.
    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    /**
     * Opens the item's screen in the official Jellyfin Android TV app.
     *
     * Uses the same intent that home-automation setups report working with that app: VIEW with the
     * item id as data, aimed at StartupActivity. It lands on the item's details screen (press Play
     * there); it does not autoplay. If that fails, the Jellyfin app is simply opened.
     */
    private fun openInJellyfin(card: ResumeCard) {
        val direct = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            component = ComponentName(JELLYFIN_PACKAGE, "$JELLYFIN_PACKAGE.ui.startup.StartupActivity")
            data = Uri.parse(card.id)
            putExtra("source", "30")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(direct)
            return
        } catch (e: ActivityNotFoundException) {
            // Jellyfin missing or its activity was renamed; fall through to a plain launch.
        } catch (e: SecurityException) {
        }

        val fallback = packageManager.getLeanbackLaunchIntentForPackage(JELLYFIN_PACKAGE)
            ?: packageManager.getLaunchIntentForPackage(JELLYFIN_PACKAGE)
        if (fallback == null) {
            Toast.makeText(this, "Install the Jellyfin app for Android TV to open this", Toast.LENGTH_LONG).show()
            return
        }
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(fallback)
    }

    private fun launchApp(app: LaunchableApp) {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
            ?: packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent == null) {
            Toast.makeText(this, "Can't open ${app.label}", Toast.LENGTH_SHORT).show()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
