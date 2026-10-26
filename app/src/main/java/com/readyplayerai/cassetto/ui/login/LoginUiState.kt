package com.readyplayerai.cassetto.ui.login

import com.readyplayerai.cassetto.data.auth.AuthException
import com.readyplayerai.cassetto.data.settings.ServerUrl

/** Which credential the form is collecting. */
enum class LoginMode { PASSWORD, API_KEY }

/** A failure the form should show, already reduced to a reason the screen can map to copy. */
data class LoginError(
    val reason: AuthException.Reason,
    val detail: String? = null,
)

/** Result of the "Test connection" affordance. Copy lives in the screen. */
sealed interface ConnectionStatus {
    data object Success : ConnectionStatus
    data class Failure(val reason: AuthException.Reason, val detail: String?) : ConnectionStatus
}

data class LoginUiState(
    val serverAddress: String = ServerUrl.DEFAULT,
    val serverAddressDraft: String = ServerUrl.DEFAULT,
    val isEditingServerAddress: Boolean = false,
    val serverAddressError: String? = null,
    val mode: LoginMode = LoginMode.PASSWORD,
    val email: String = "",
    val password: String = "",
    val apiKey: String = "",
    val isSubmitting: Boolean = false,
    val error: LoginError? = null,
    val isTestingConnection: Boolean = false,
    val connectionStatus: ConnectionStatus? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && serverAddress.isNotBlank() &&
            when (mode) {
                LoginMode.PASSWORD -> email.isNotBlank() && password.isNotBlank()
                LoginMode.API_KEY -> apiKey.isNotBlank()
            }
}
