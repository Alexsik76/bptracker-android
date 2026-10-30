package ua.vn.home.bptracker.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.OperationUiState
import ua.vn.home.bptracker.data.api.UserApi
import ua.vn.home.bptracker.data.dto.UserUpdateRequest

data class ProfileState(
    val email: String = "",
    val name: String = "",
    val isLoading: Boolean = true,
    val saveState: OperationUiState = OperationUiState.Idle
)

class ProfileViewModel(
    private val userApi: UserApi = ServiceLocator.userApi
) : ViewModel() {

    private fun getString(@androidx.annotation.StringRes resId: Int): String {
        return try {
            ServiceLocator.applicationContext.getString(resId)
        } catch (_: Exception) {
            "Error"
        }
    }

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val me = userApi.me()
                _state.update { 
                    it.copy(
                        email = me.email,
                        name = me.displayName ?: "",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { 
                    it.copy(
                        isLoading = false,
                        saveState = OperationUiState.Error(getString(R.string.error_load_profile_failed))
                    )
                }
            }
        }
    }

    fun onNameChange(newName: String) {
        _state.update { 
            it.copy(
                name = newName,
                saveState = if (it.saveState is OperationUiState.Success || it.saveState is OperationUiState.Error) {
                    OperationUiState.Idle
                } else {
                    it.saveState
                }
            )
        }
    }

    fun save() {
        val currentName = _state.value.name
        viewModelScope.launch {
            try {
                _state.update { it.copy(saveState = OperationUiState.InProgress) }
                val trimmed = currentName.trim()
                val updated = userApi.updateMe(UserUpdateRequest(displayName = trimmed.ifEmpty { null }))
                _state.update { 
                    it.copy(
                        name = updated.displayName ?: "",
                        saveState = OperationUiState.Success
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { 
                    it.copy(saveState = OperationUiState.Error(getString(R.string.error_save_profile_failed)))
                }
            }
        }
    }
}
