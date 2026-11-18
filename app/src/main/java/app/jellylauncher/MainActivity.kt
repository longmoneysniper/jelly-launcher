package app.jellylauncher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.jellylauncher.data.WHOLPHIN_PACKAGE
import app.jellylauncher.data.LaunchableApp
import app.jellylauncher.data.ResumeCard
import app.jellylauncher.ui.AllAppsScreen
import app.jellylauncher.ui.HomeScreen
import app.jellylauncher.ui.JellyTheme
import app.jellylauncher.ui.LoginScreen
import app.jellylauncher.ui.SetupScreen
import android.provider.Settings

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
                } else if (state.showSetup) {
                    SetupScreen(
                        overrideOn = state.overrideOn,
                        onAccessibility = { openSettingsScreen(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
                        onAppInfo = { openSettingsScreen(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, true) },
                        onHomeSettings = { openSettingsScreen(Settings.ACTION_HOME_SETTINGS) },
                        onDone = viewModel::closeSetup,
                    )
                } else if (state.showAllApps) {
                    AllAppsScreen(
                        apps = state.apps,
                        onLaunch = ::launchApp,
                        onClose = viewModel::closeAllApps,
                    )
                } else {
                    HomeScreen(
                        state = state,
                        onPlay = ::openInWholphin,
                        onLaunchApp = ::launchApp,
                        onAllApps = viewModel::openAllApps,
                        onSettings = viewModel::openSettings,
                        onSetup = viewModel::openSetup,
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
        viewModel.checkOverride()
    }

    // Pressing Home while the launcher is already open brings you back to the main screen.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.closeAllApps()
    }

    /**
     * Plays the item in Wholphin. Uses Wholphin's documented link: wholphin://play?itemId=<id>&position=<ms>
     * (position resumes where you left off). If that fails, Wholphin is simply opened.
     */
    private fun openInWholphin(card: ResumeCard) {
        val direct = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("wholphin://play?itemId=${card.id}&position=${card.positionMs}")
            setPackage(WHOLPHIN_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(direct)
            return
        } catch (e: ActivityNotFoundException) {
        } catch (e: SecurityException) {
        }

        val fallback = packageManager.getLeanbackLaunchIntentForPackage(WHOLPHIN_PACKAGE)
            ?: packageManager.getLaunchIntentForPackage(WHOLPHIN_PACKAGE)
        if (fallback == null) {
            Toast.makeText(this, "Install Wholphin to play this", Toast.LENGTH_LONG).show()
            return
        }
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(fallback)
    }

    private fun openSettingsScreen(action: String, withPackage: Boolean = false) {
        val i = Intent(action).apply {
            if (withPackage) data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { startActivity(i) } catch (e: Exception) {
            Toast.makeText(this, "This TV doesn't have that settings page", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchApp(app: LaunchableApp) {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
            ?: packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent == null) {
            Toast.makeText(this, "Can't open ${app.label}", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.closeAllApps()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
