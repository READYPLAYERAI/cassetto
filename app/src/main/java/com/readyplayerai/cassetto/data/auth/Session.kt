package com.readyplayerai.cassetto.data.auth

import com.readyplayerai.cassetto.data.remote.dto.UserMeDto

/** The app-wide authentication state. Exactly one of these is true at a time. */
sealed interface Session {
    /** Credentials are being read from disk / verified against the server. */
    data object Loading : Session

    data object LoggedOut : Session

    data class LoggedIn(val user: UserMeDto, val serverAddress: String) : Session
}

/**
 * Why an authentication operation failed, in the only distinctions the UI needs.
 * Deliberately carries no user-facing copy: string resources live in the UI layer.
 */
class AuthException(
    val reason: Reason,
    val detail: String? = null,
    cause: Throwable? = null,
) : Exception(detail ?: reason.name, cause) {

    enum class Reason {
        /** Email/password rejected by `apiKeys.exchange`. */
        WRONG_CREDENTIALS,

        /** A pasted API key was rejected by `apiKeys.validate`. */
        INVALID_API_KEY,

        /** A stored key stopped working; credentials have been cleared. */
        SESSION_EXPIRED,

        /** The address the user typed cannot be parsed as an http(s) origin. */
        INVALID_SERVER_ADDRESS,

        /** DNS failure, refused connection, timeout. */
        CONNECTION,

        /** `apiKeys.exchange` is limited to 10 requests per 15 minutes. */
        RATE_LIMITED,

        /** The server answered, but not usefully. */
        SERVER,

        /** Anything unclassified. */
        UNKNOWN,
    }
}
