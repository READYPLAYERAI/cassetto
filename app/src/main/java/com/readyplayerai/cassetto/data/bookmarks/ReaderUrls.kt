package com.readyplayerai.cassetto.data.bookmarks

import java.net.URI

internal object ReaderUrls {
    private val linkSchemes = setOf("http", "https", "mailto")
    private val imageSchemes = setOf("http", "https")

    fun resolve(baseUrl: String?, raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val normalized = value.replace(" ", "%20")
        if (normalized.startsWith("//")) {
            val scheme = baseUrl?.let { runCatching { URI(it).scheme }.getOrNull() } ?: "https"
            return "$scheme:$normalized"
        }
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return null
        if (uri.isAbsolute) return uri.toString()
        val base = baseUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { URI(base).resolve(uri).toString() }.getOrNull()
    }

    fun resolveLink(baseUrl: String?, raw: String?): String? =
        resolve(baseUrl, raw)?.takeIf(::isSafeLink)

    fun resolveImage(baseUrl: String?, raw: String?): String? =
        resolve(baseUrl, raw)?.takeIf(::isSafeImage)

    fun isSafeLink(url: String): Boolean = schemeOf(url) in linkSchemes

    fun isSafeImage(url: String): Boolean = schemeOf(url) in imageSchemes

    fun host(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return runCatching { URI(url).host }.getOrNull()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
    }

    private fun schemeOf(url: String): String? =
        runCatching { URI(url).scheme?.lowercase() }.getOrNull()
}
