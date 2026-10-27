package com.readyplayerai.cassetto.data.remote

import com.readyplayerai.cassetto.data.remote.dto.ApiKeyDto
import com.readyplayerai.cassetto.data.remote.dto.BookmarksPageDto
import com.readyplayerai.cassetto.data.remote.dto.ExchangeRequest
import com.readyplayerai.cassetto.data.remote.dto.HealthDto
import com.readyplayerai.cassetto.data.remote.dto.RevokeRequest
import com.readyplayerai.cassetto.data.remote.dto.UserMeDto
import com.readyplayerai.cassetto.data.remote.dto.ValidateRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Everything Cassetto needs from a Karakeep server to sign in. The server
 * address is always a parameter: it is user-configurable and must never be
 * hardcoded here.
 */
@Singleton
class KarakeepApi @Inject constructor(
    private val http: HttpClient,
    private val trpc: TrpcClient,
    private val json: Json,
) {

    /** Exchanges email + password for a freshly minted API key. */
    suspend fun exchangePassword(
        serverAddress: String,
        email: String,
        password: String,
        keyName: String,
    ): ApiKeyDto {
        val payload = trpc.mutate(
            serverAddress = serverAddress,
            procedure = "apiKeys.exchange",
            input = json.encodeToJsonElement(ExchangeRequest(keyName, email, password)),
        )
        return try {
            json.decodeFromJsonElement(ApiKeyDto.serializer(), payload)
        } catch (e: Exception) {
            throw ApiException.Malformed(e)
        }
    }

    /** @throws ApiException.Unauthorized when the key is not valid. */
    suspend fun validateApiKey(serverAddress: String, apiKey: String) {
        trpc.mutate(
            serverAddress = serverAddress,
            procedure = "apiKeys.validate",
            input = json.encodeToJsonElement(ValidateRequest(apiKey)),
            bearer = apiKey,
        )
        // Payload is {"success":true}; nothing useful to return.
    }

    /** Best effort: keys may only revoke themselves, which is exactly our case. */
    suspend fun revokeApiKey(serverAddress: String, apiKey: String, keyId: String) {
        trpc.mutate(
            serverAddress = serverAddress,
            procedure = "apiKeys.revoke",
            input = json.encodeToJsonElement(RevokeRequest(keyId)),
            bearer = apiKey,
        )
    }

    suspend fun getCurrentUser(serverAddress: String, apiKey: String): UserMeDto {
        val (status, body) = getText("$serverAddress/api/v1/users/me", apiKey)
        if (status !in 200..299) throw httpStatusError(status, body)
        return try {
            json.decodeFromString(UserMeDto.serializer(), body)
        } catch (e: Exception) {
            throw ApiException.Malformed(e)
        }
    }

    suspend fun health(serverAddress: String): HealthDto {
        val (status, body) = getText("$serverAddress/api/health", null)
        if (status !in 200..299) throw httpStatusError(status, body)
        return try {
            json.decodeFromString(HealthDto.serializer(), body)
        } catch (e: Exception) {
            throw ApiException.Malformed(e)
        }
    }

    /** Lists bookmarks without content, newest first. */
    suspend fun listBookmarks(
        serverAddress: String,
        apiKey: String,
        cursor: String? = null,
        limit: Int = 50,
    ): BookmarksPageDto {
        val parameters = linkedMapOf<String, Any?>(
            "includeContent" to false,
            "sortOrder" to "desc",
            "limit" to limit,
        )
        if (cursor != null) parameters["cursor"] = cursor
        return getBookmarksPage("$serverAddress/api/v1/bookmarks", apiKey, parameters)
    }

    /** Full-text search, newest first by default. */
    suspend fun searchBookmarks(
        serverAddress: String,
        apiKey: String,
        query: String,
        cursor: String? = null,
        limit: Int = 50,
        sortOrder: String = "desc",
    ): BookmarksPageDto {
        val parameters = linkedMapOf<String, Any?>(
            "q" to query,
            "searchMode" to "fts",
            "includeContent" to false,
            "sortOrder" to sortOrder,
            "limit" to limit,
        )
        if (cursor != null) parameters["cursor"] = cursor
        return getBookmarksPage("$serverAddress/api/v1/bookmarks/search", apiKey, parameters)
    }

    private suspend fun getBookmarksPage(
        url: String,
        apiKey: String,
        parameters: Map<String, Any?>,
    ): BookmarksPageDto {
        val (status, body) = getText(url, apiKey, parameters)
        if (status !in 200..299) throw httpStatusError(status, body)
        return try {
            json.decodeFromString(BookmarksPageDto.serializer(), body)
        } catch (e: Exception) {
            throw ApiException.Malformed(e)
        }
    }

    private suspend fun getText(
        url: String,
        bearer: String?,
        parameters: Map<String, Any?> = emptyMap(),
    ): Pair<Int, String> {
        return try {
            val response = http.get(url) {
                if (bearer != null) header(HttpHeaders.Authorization, "Bearer $bearer")
                parameters.forEach { (key, value) ->
                    if (value != null) parameter(key, value)
                }
            }
            response.status.value to response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.toApiException()
        }
    }
}
