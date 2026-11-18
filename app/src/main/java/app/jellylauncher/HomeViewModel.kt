package app.jellylauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.jellylauncher.data.AppsRepository
import app.jellylauncher.data.AuthExpiredException
import app.jellylauncher.data.JellyfinApi
import app.jellylauncher.data.LaunchableApp
import app.jellylauncher.data.Prefs
import app.jellylauncher.data.ResumeCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val showLogin: Boolean = false,
    val loggingIn: Boolean = false,
    val showAllApps: Boolean = false,
    val showSetup: Boolean = false,
    val overrideOn: Boolean = false,
    val loginError: String? = null,
    val cards: List<ResumeCard> = emptyList(),
    val apps: List<LaunchableApp> = emptyList(),
    val error: String? = null,
    val userName: String = "",
    val serverUrl: String = "",
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = Prefs(application)
    private val api = JellyfinApi(prefs)
    private val appsRepository = AppsRepository(application)

    private val _state = MutableStateFlow(
        HomeUiState(
            showLogin = !prefs.isLoggedIn,
            loading = prefs.isLoggedIn,
            userName = prefs.userName,
            serverUrl = prefs.serverUrl,
        )
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val setupPrefs = application.getSharedPreferences("setup", 0)

    init {
        if (!setupPrefs.getBoolean("seen", false)) _state.update { it.copy(showSetup = true, overrideOn = isOverrideOn()) }
    }

    private var refreshJob: Job? = null

    /** Reloads installed apps and Continue Watching. Called every time the home screen comes back. */
    fun refresh() {
        viewModelScope.launch {
            try {
                val apps = appsRepository.load()
                _state.update { it.copy(apps = apps) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep whatever app list we already have.
            }
        }

        if (!prefs.isLoggedIn) {
            _state.update { it.copy(showLogin = true, loading = false) }
            return
        }

        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                val items = api.continueWatching()
                _state.update {
                    it.copy(
                        cards = items.map(api::toCard),
                        loading = false,
                        error = null,
                        userName = prefs.userName,
                        serverUrl = prefs.serverUrl,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthExpiredException) {
                prefs.signOut()
                _state.update {
                    it.copy(
                        showLogin = true,
                        loading = false,
                        cards = emptyList(),
                        loginError = "Your session expired. Please sign in again.",
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = "Can't reach your Jellyfin server (${e.message ?: e.javaClass.simpleName})",
                    )
                }
            }
        }
    }

    fun login(server: String, username: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(loggingIn = true, loginError = null) }
            try {
                api.login(server, username, password)
                _state.update {
                    it.copy(
                        loggingIn = false,
                        showLogin = false,
                        loading = true,
                        error = null,
                        userName = prefs.userName,
                        serverUrl = prefs.serverUrl,
                    )
                }
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loggingIn = false,
                        loginError = e.message ?: "Couldn't sign in",
                    )
                }
            }
        }
    }

    fun openSettings() = _state.update { it.copy(showLogin = true, loginError = null) }

    fun openSetup() = _state.update { it.copy(showSetup = true, overrideOn = isOverrideOn()) }

    fun closeSetup() {
        setupPrefs.edit().putBoolean("seen", true).apply()
        _state.update { it.copy(showSetup = false) }
    }

    fun checkOverride() = _state.update { it.copy(overrideOn = isOverrideOn()) }

    private fun isOverrideOn(): Boolean {
        val app = getApplication<Application>()
        val enabled = android.provider.Settings.Secure.getString(
            app.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.contains(app.packageName + "/", ignoreCase = true) && enabled.contains("HomeOverrideService")
    }

    fun openAllApps() = _state.update { it.copy(showAllApps = true) }

    fun closeAllApps() = _state.update { it.copy(showAllApps = false) }

    fun closeSettings() {
        if (prefs.isLoggedIn) _state.update { it.copy(showLogin = false, loginError = null) }
    }
}
