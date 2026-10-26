package com.readyplayerai.cassetto.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readyplayerai.cassetto.data.auth.AuthException
import com.readyplayerai.cassetto.data.auth.AuthRepository
import com.readyplayerai.cassetto.data.settings.ServerUrl
import com.readyplayerai.cassetto.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.serverAddress.collect { saved ->
                _uiState.update { state ->
                    if (state.isEditingServerAddress) state
                    else state.copy(serverAddress = saved, serverAddressDraft = saved)
                }
            }
        }
        // A key that expired (or a cold start that could not reach the server)
        // leaves a message behind for the user; surface it once.
        auth.restoreError.value?.let { restoreError ->
            _uiState.update { state -> state.copy(error = restoreError.toLoginError()) }
            auth.clearRestoreError()
        }
    }

    fun onModeChange(mode: LoginMode) {
        _uiState.update { it.copy(mode = mode, error = null, connectionStatus = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKey = value, error = null) }
    }

    fun onStartServerAddressEdit() {
        _uiState.update {
            it.copy(
                isEditingServerAddress = true,
                serverAddressDraft = it.serverAddress,
                serverAddressError = null,
                connectionStatus = null,
            )
        }
    }

    fun onServerAddressDraftChange(value: String) {
        _uiState.update { it.copy(serverAddressDraft = value, serverAddressError = null) }
    }

    fun onCancelServerAddressEdit() {
        _uiState.update {
            it.copy(isEditingServerAddress = false, serverAddressError = null)
        }
    }

    fun onSaveServerAddress() {
        val draft = _uiState.value.serverAddressDraft
        val normalized = ServerUrl.normalize(draft)
        val validation = ServerUrl.validationError(normalized)
        if (validation != null) {
            _uiState.update { it.copy(serverAddressError = validation) }
            return
        }
        viewModelScope.launch {
            try {
                settings.setServerAddress(normalized)
                _uiState.update {
                    it.copy(
                        serverAddress = normalized,
                        serverAddressDraft = normalized,
                        isEditingServerAddress = false,
                        serverAddressError = null,
                        // A failure from the previous server says nothing about this one.
                        error = null,
                        connectionStatus = null,
                    )
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(serverAddressError = e.message ?: validation) }
            }
        }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                when (state.mode) {
                    LoginMode.PASSWORD ->
                        auth.loginWithPassword(state.serverAddress, state.email, state.password)
                    LoginMode.API_KEY ->
                        auth.loginWithApiKey(state.serverAddress, state.apiKey)
                }
                // Success: the nav host reacts to Session.LoggedIn. Wipe secrets.
                _uiState.update { it.copy(isSubmitting = false, password = "", apiKey = "") }
            } catch (e: AuthException) {
                _uiState.update { it.copy(isSubmitting = false, error = e.toLoginError()) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = LoginError(AuthException.Reason.UNKNOWN, e.message),
                    )
                }
            }
        }
    }

    fun onTestConnection() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, connectionStatus = null) }
            val result = auth.testConnection(state.serverAddress)
            val status = result.fold(
                onSuccess = { ConnectionStatus.Success },
                onFailure = { error ->
                    val authError = error as? AuthException
                    ConnectionStatus.Failure(
                        authError?.reason ?: AuthException.Reason.UNKNOWN,
                        authError?.detail ?: error.message,
                    )
                },
            )
            _uiState.update { it.copy(isTestingConnection = false, connectionStatus = status) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun AuthException.toLoginError() = LoginError(reason, detail)
}
