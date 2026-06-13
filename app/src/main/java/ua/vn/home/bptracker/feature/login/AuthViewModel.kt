package ua.vn.home.bptracker.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator

sealed interface AuthState {
    data object Loading : AuthState
    data class LoggedOut(val info: String? = null) : AuthState
    data class LoggedIn(val email: String) : AuthState
}

class AuthViewModel : ViewModel() {

    private val api = ServiceLocator.authApi
    private val tokenStore = ServiceLocator.tokenStore

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            tokenStore.load()
            _state.value = try {
                val me = api.me()
                AuthState.LoggedIn(me.email)
            } catch (e: Exception) {
                // No/invalid token -> backend returns 401 -> Retrofit throws. Expected when signed out.
                AuthState.LoggedOut(info = e.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) { }
            tokenStore.clear()
            _state.value = AuthState.LoggedOut()
        }
    }
}
