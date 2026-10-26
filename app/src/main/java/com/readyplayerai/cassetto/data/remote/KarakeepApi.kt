package com.readyplayerai.cassetto.data.remote

import com.readyplayerai.cassetto.data.remote.dto.ApiKeyDto
import com.readyplayerai.cassetto.data.remote.dto.ExchangeRequest
import com.readyplayerai.cassetto.data.remote.dto.HealthDto
import com.readyplayerai.cassetto.data.remote.dto.RevokeRequest
import com.readyplayerai.cassetto.data.remote.dto.UserMeDto
import com.readyplayerai.cassetto.data.remote.dto.ValidateRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
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

    private suspend fun getText(url: String, bearer: String?): Pair<Int, String> {
        return try {
            val response = http.get(url) {
                if (bearer != null) header(HttpHeaders.Authorization, "Bearer $bearer")
            }
            response.status.value to response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.toApiException()
        }
    }
}
