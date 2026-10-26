package com.readyplayerai.cassetto.data.auth

import com.readyplayerai.cassetto.data.remote.ApiException
import com.readyplayerai.cassetto.data.remote.KarakeepApi
import com.readyplayerai.cassetto.data.settings.ServerUrl
import com.readyplayerai.cassetto.data.settings.SettingsRepository
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Owns everything about "who is signed in".
 *
 * The session starts as [Session.Loading] and resolves on its own from the
 * stored credentials, so the app shell can simply gate on it. Every failure is
 * surfaced as an [AuthException] with a [AuthException.Reason]; this class never
 * produces user-facing copy, and never logs or embeds the API key.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val settings: SettingsRepository,
    private val api: KarakeepApi,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _session = MutableStateFlow<Session>(Session.Loading)
    val session: StateFlow<Session> = _session.asStateFlow()

    /**
     * Set when a stored session could not be restored (expired key, server
     * unreachable). The login screen shows this, then clears it.
     */
    private val _restoreError = MutableStateFlow<AuthException?>(null)
    val restoreError: StateFlow<AuthException?> = _restoreError.asStateFlow()

    init {
        scope.launch { restoreSession() }
    }

    fun clearRestoreError() {
        _restoreError.value = null
    }

    /**
     * Signs in with email + password by minting a new API key, exactly like the
     * official client ("homemade OAuth"): POST /api/trpc/apiKeys.exchange.
     *
     * @throws AuthException on any failure; on failure nothing is left stored.
     */
    suspend fun loginWithPassword(serverAddress: String, email: String, password: String) {
        val normalized = requireUsableAddress(serverAddress)
        settings.setServerAddress(normalized)

        val minted = try {
            api.exchangePassword(
                serverAddress = normalized,
                email = email.trim(),
                password = password,
                keyName = newKeyName(),
            )
        } catch (e: ApiException) {
            throw e.toAuthException(AuthException.Reason.WRONG_CREDENTIALS)
        }

        settings.saveApiKey(minted.key, minted.id)
        completeLogin(normalized, minted.key)
    }

    /**
     * Signs in with a key the user pasted from the web UI.
     *
     * @throws AuthException on any failure; on failure nothing is left stored.
     */
    suspend fun loginWithApiKey(serverAddress: String, apiKey: String) {
        val normalized = requireUsableAddress(serverAddress)
        val key = apiKey.trim()
        if (key.isEmpty()) {
            throw AuthException(AuthException.Reason.INVALID_API_KEY)
        }
        settings.setServerAddress(normalized)

        try {
            api.validateApiKey(normalized, key)
        } catch (e: ApiException) {
            throw e.toAuthException(AuthException.Reason.INVALID_API_KEY)
        }

        // Pasted keys have no known id, so logout cannot revoke them server-side.
        settings.saveApiKey(key, keyId = null)
        completeLogin(normalized, key)
    }

    /**
     * Revokes the key when its id is known, then clears the local credentials.
     * Never throws: signing out must always succeed on this device.
     */
    suspend fun logout() {
        withContext(NonCancellable) {
            val address = settings.serverAddress.first()
            val key = settings.apiKey.first()
            val keyId = settings.apiKeyId.first()
            if (key != null && keyId != null) {
                // Best effort, short leash: a slow server must not trap the user.
                withTimeoutOrNull(REVOKE_TIMEOUT_MS) {
                    try {
                        api.revokeApiKey(address, key, keyId)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Best effort: revoking is a courtesy, not a requirement.
                    }
                }
            }
            settings.clearApiKey()
            _session.value = Session.LoggedOut
            _restoreError.value = null
        }
    }

    /** Diagnostics for the login screen's "Test connection" affordance. */
    suspend fun testConnection(serverAddress: String): Result<String> {
        val normalized = ServerUrl.normalize(serverAddress)
        ServerUrl.validationError(normalized)?.let {
            return Result.failure(AuthException(AuthException.Reason.INVALID_SERVER_ADDRESS, it))
        }
        return try {
            val health = api.health(normalized)
            if (health.status == "ok") {
                val host = runCatching { URI(normalized).host }.getOrNull() ?: normalized
                Result.success("Connected to $host (status: ok)")
            } else {
                Result.failure(
                    AuthException(
                        AuthException.Reason.SERVER,
                        "Server reported status: ${health.status}",
                    ),
                )
            }
        } catch (e: ApiException) {
            Result.failure(e.toAuthException(AuthException.Reason.UNKNOWN))
        }
    }

    private suspend fun restoreSession() {
        val address = settings.serverAddress.first()
        val key = settings.apiKey.first()
        if (key == null) {
            _session.value = Session.LoggedOut
            return
        }
        try {
            val user = api.getCurrentUser(address, key)
            // A logout may have landed while this request was in flight; never
            // resurrect a session whose credential is already gone.
            if (settings.apiKey.first() == key) {
                _session.value = Session.LoggedIn(user, address)
            } else {
                _session.value = Session.LoggedOut
            }
        } catch (e: ApiException) {
            if (e is ApiException.Unauthorized) {
                settings.clearApiKey()
                _restoreError.value = AuthException(AuthException.Reason.SESSION_EXPIRED)
            } else {
                // Keep the credential: a flaky network must not sign the user out
                // for good. The login screen offers a retry.
                _restoreError.value = e.toAuthException(AuthException.Reason.UNKNOWN)
            }
            _session.value = Session.LoggedOut
        }
    }

    private suspend fun completeLogin(serverAddress: String, apiKey: String) {
        try {
            val user = api.getCurrentUser(serverAddress, apiKey)
            if (settings.apiKey.first() == apiKey) {
                _session.value = Session.LoggedIn(user, serverAddress)
                _restoreError.value = null
            } else {
                // Signed out mid-flight; honour the logout.
                _session.value = Session.LoggedOut
            }
        } catch (e: ApiException) {
            // Do not leave a half-written credential behind.
            settings.clearApiKey()
            _session.value = Session.LoggedOut
            throw e.toAuthException(AuthException.Reason.INVALID_API_KEY)
        }
    }

    private fun requireUsableAddress(raw: String): String {
        val normalized = ServerUrl.normalize(raw)
        val error = ServerUrl.validationError(normalized)
        if (error != null) {
            throw AuthException(AuthException.Reason.INVALID_SERVER_ADDRESS, error)
        }
        return normalized
    }

    private fun newKeyName(): String = "Cassetto (${randomSuffix()})"

    private fun randomSuffix(): String {
        val alphabet = ('a'..'z') + ('0'..'9')
        return (1..5).map { alphabet[Random.nextInt(alphabet.size)] }.joinToString("")
    }

    private fun ApiException.toAuthException(unauthorizedReason: AuthException.Reason): AuthException =
        when (this) {
            is ApiException.Unauthorized -> AuthException(unauthorizedReason, cause = this)
            is ApiException.BadRequest -> AuthException(AuthException.Reason.SERVER, detail, this)
            is ApiException.RateLimited -> AuthException(AuthException.Reason.RATE_LIMITED, cause = this)
            is ApiException.ServerError -> AuthException(
                AuthException.Reason.SERVER,
                detail ?: "Server error $httpStatus",
                this,
            )
            is ApiException.Network -> AuthException(AuthException.Reason.CONNECTION, cause = this)
            is ApiException.Malformed -> AuthException(AuthException.Reason.SERVER, cause = this)
            is ApiException.Unknown -> AuthException(AuthException.Reason.UNKNOWN, message, this)
        }

    private companion object {
        const val REVOKE_TIMEOUT_MS = 5_000L
    }
}
