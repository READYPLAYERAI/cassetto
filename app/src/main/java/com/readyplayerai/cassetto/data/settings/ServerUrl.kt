package com.readyplayerai.cassetto.data.settings

import java.net.URI

/**
 * Normalisation and validation of a Karakeep server address.
 *
 * Users paste addresses in every shape imaginable: with a trailing slash, with
 * the API path still attached, with whitespace from a copy-paste. Everything
 * downstream (Ktor calls, the tRPC client) assumes a bare origin, so the rules
 * live here once and are unit-tested.
 */
object ServerUrl {

    /** Karakeep's hosted instance, offered as the default in the login screen. */
    const val DEFAULT: String = "https://cloud.karakeep.app"

    private const val REQUIRED = "Server address is required"
    private const val SCHEME = "Server address must start with http:// or https://"
    private const val HOST = "Server address must include a host name"

    /**
     * Trims whitespace, drops trailing slashes, and strips a trailing `/api` or
     * `/api/v1` path so a pasted API endpoint becomes a usable origin.
     * Any other path is preserved — Karakeep is often served from a subpath.
     */
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        val schemeIndex = trimmed.indexOf("://")
        if (schemeIndex < 0) {
            // No scheme present: hand the value to validation untouched (minus
            // stray slashes) so it can report the scheme problem itself.
            return trimmed.trimEnd('/')
        }
        val head = trimmed.substring(0, schemeIndex + 3)
        var rest = trimmed.substring(schemeIndex + 3).trimEnd('/')
        rest = when {
            rest.endsWith("/api/v1") -> rest.removeSuffix("/api/v1")
            rest.endsWith("/api") -> rest.removeSuffix("/api")
            else -> rest
        }
        return head + rest.trimEnd('/')
    }

    /**
     * @return null when [normalized] is a usable http(s) origin, otherwise a
     * message suitable for showing directly to the user.
     */
    fun validationError(normalized: String): String? {
        if (normalized.isBlank()) return REQUIRED
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) return SCHEME
        // `java.net.URI` rejects a missing authority outright (e.g. "https://"),
        // so detect that case before parsing to report it as a missing host.
        if (normalized.substringAfter("://").isEmpty()) return HOST
        val uri = try {
            URI(normalized)
        } catch (e: Exception) {
            return SCHEME
        }
        if (uri.scheme != "http" && uri.scheme != "https") return SCHEME
        if (uri.host.isNullOrBlank()) return HOST
        return null
    }
}
