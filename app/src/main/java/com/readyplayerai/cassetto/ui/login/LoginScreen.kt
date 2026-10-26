package com.readyplayerai.cassetto.ui.login

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readyplayerai.cassetto.R
import com.readyplayerai.cassetto.data.auth.AuthException
import com.readyplayerai.cassetto.ui.theme.CassettoTheme

@Composable
fun LoginScreen(
    state: LoginUiState,
    onModeChange: (LoginMode) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onStartServerAddressEdit: () -> Unit,
    onServerAddressDraftChange: (String) -> Unit,
    onCancelServerAddressEdit: () -> Unit,
    onSaveServerAddress: () -> Unit,
    onSubmit: () -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))

                CredentialCard(
                    state = state,
                    onModeChange = onModeChange,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onApiKeyChange = onApiKeyChange,
                    onSubmit = onSubmit,
                )

                Spacer(Modifier.height(16.dp))

                ServerRow(
                    state = state,
                    onStartServerAddressEdit = onStartServerAddressEdit,
                )

                if (state.isEditingServerAddress) {
                    Spacer(Modifier.height(8.dp))
                    ServerAddressEditor(
                        state = state,
                        onServerAddressDraftChange = onServerAddressDraftChange,
                        onCancelServerAddressEdit = onCancelServerAddressEdit,
                        onSaveServerAddress = onSaveServerAddress,
                    )
                }

                Spacer(Modifier.height(8.dp))

                ConnectionTest(
                    state = state,
                    onTestConnection = onTestConnection,
                )
            }
        }
    }
}

@Composable
private fun CredentialCard(
    state: LoginUiState,
    onModeChange: (LoginMode) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.mode == LoginMode.PASSWORD,
                    onClick = { onModeChange(LoginMode.PASSWORD) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {},
                    label = { Text(stringResource(R.string.login_mode_password)) },
                )
                SegmentedButton(
                    selected = state.mode == LoginMode.API_KEY,
                    onClick = { onModeChange(LoginMode.API_KEY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {},
                    label = { Text(stringResource(R.string.login_mode_api_key)) },
                )
            }

            when (state.mode) {
                LoginMode.PASSWORD -> {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = { Text(stringResource(R.string.login_email_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(R.string.login_password_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                LoginMode.API_KEY -> {
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text(stringResource(R.string.login_api_key_label)) },
                        supportingText = {
                            Text(stringResource(R.string.login_api_key_placeholder))
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            state.error?.let { error ->
                ErrorSurface(error)
            }

            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.login_sign_in),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorSurface(error: LoginError) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val message = stringResource(error.reason.messageRes())
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
            )
            val detail = error.detail
            if (!detail.isNullOrBlank() && detail != message) {
                Text(
                    text = detail,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ServerRow(
    state: LoginUiState,
    onStartServerAddressEdit: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.login_server_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = state.serverAddress,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onStartServerAddressEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.login_server_edit),
                )
            }
        }
    }
}

@Composable
private fun ServerAddressEditor(
    state: LoginUiState,
    onServerAddressDraftChange: (String) -> Unit,
    onCancelServerAddressEdit: () -> Unit,
    onSaveServerAddress: () -> Unit,
) {
    OutlinedTextField(
        value = state.serverAddressDraft,
        onValueChange = onServerAddressDraftChange,
        isError = state.serverAddressError != null,
        supportingText = state.serverAddressError?.let { error ->
            { Text(error) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSaveServerAddress() }),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancelServerAddressEdit) {
            Text(stringResource(R.string.login_server_cancel))
        }
        Button(onClick = onSaveServerAddress) {
            Text(stringResource(R.string.login_server_save))
        }
    }
}

@Composable
private fun ConnectionTest(
    state: LoginUiState,
    onTestConnection: () -> Unit,
) {
    TextButton(
        onClick = onTestConnection,
        enabled = !state.isTestingConnection,
    ) {
        Text(
            stringResource(
                if (state.isTestingConnection) R.string.login_testing_connection
                else R.string.login_test_connection,
            ),
        )
    }
    when (val status = state.connectionStatus) {
        is ConnectionStatus.Success -> Text(
            text = stringResource(R.string.login_connection_ok),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )

        is ConnectionStatus.Failure -> {
            val base = stringResource(status.reason.messageRes())
            val text = if (!status.detail.isNullOrBlank()) "$base — ${status.detail}" else base
            Text(
                text = text,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        null -> Unit
    }
}

@Composable
fun LoginRoute(viewModel: LoginViewModel = hiltViewModel()) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    LoginScreen(
        state = state,
        onModeChange = viewModel::onModeChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onApiKeyChange = viewModel::onApiKeyChange,
        onStartServerAddressEdit = viewModel::onStartServerAddressEdit,
        onServerAddressDraftChange = viewModel::onServerAddressDraftChange,
        onCancelServerAddressEdit = viewModel::onCancelServerAddressEdit,
        onSaveServerAddress = viewModel::onSaveServerAddress,
        onSubmit = viewModel::onSubmit,
        onTestConnection = viewModel::onTestConnection,
    )
}

@StringRes
private fun AuthException.Reason.messageRes(): Int = when (this) {
    AuthException.Reason.WRONG_CREDENTIALS -> R.string.login_error_wrong_credentials
    AuthException.Reason.INVALID_API_KEY -> R.string.login_error_invalid_api_key
    AuthException.Reason.SESSION_EXPIRED -> R.string.login_error_session_expired
    AuthException.Reason.INVALID_SERVER_ADDRESS -> R.string.login_error_invalid_address
    AuthException.Reason.CONNECTION -> R.string.login_error_connection
    AuthException.Reason.RATE_LIMITED -> R.string.login_error_rate_limited
    AuthException.Reason.SERVER -> R.string.login_error_server
    AuthException.Reason.UNKNOWN -> R.string.login_error_unknown
}

@Preview(name = "Password / light", showBackground = true)
@Composable
private fun LoginScreenPasswordLightPreview() {
    CassettoTheme(dynamicColor = false) {
        LoginScreenPreviewContent()
    }
}

@Preview(name = "Password / dark", showBackground = true, uiMode = 0x20)
@Composable
private fun LoginScreenPasswordDarkPreview() {
    CassettoTheme(darkTheme = true, dynamicColor = false) {
        LoginScreenPreviewContent()
    }
}

@Preview(name = "API key", showBackground = true)
@Composable
private fun LoginScreenApiKeyPreview() {
    CassettoTheme(dynamicColor = false) {
        LoginScreenPreviewContent(
            state = LoginUiState(
                mode = LoginMode.API_KEY,
                apiKey = "ak_example",
            ),
        )
    }
}

@Preview(name = "Submitting", showBackground = true)
@Composable
private fun LoginScreenSubmittingPreview() {
    CassettoTheme(dynamicColor = false) {
        LoginScreenPreviewContent(
            state = LoginUiState(
                email = "user@example.com",
                password = "hunter2",
                isSubmitting = true,
            ),
        )
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun LoginScreenErrorPreview() {
    CassettoTheme(dynamicColor = false) {
        LoginScreenPreviewContent(
            state = LoginUiState(
                email = "user@example.com",
                password = "hunter2",
                error = LoginError(AuthException.Reason.CONNECTION),
            ),
        )
    }
}

@Composable
private fun LoginScreenPreviewContent(
    state: LoginUiState = LoginUiState(
        email = "user@example.com",
        password = "hunter2",
    ),
) {
    LoginScreen(
        state = state,
        onModeChange = {},
        onEmailChange = {},
        onPasswordChange = {},
        onApiKeyChange = {},
        onStartServerAddressEdit = {},
        onServerAddressDraftChange = {},
        onCancelServerAddressEdit = {},
        onSaveServerAddress = {},
        onSubmit = {},
        onTestConnection = {},
    )
}
