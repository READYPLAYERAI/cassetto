package com.readyplayerai.cassetto.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The minimal tRPC client Cassetto needs: non-batched mutations in superjson's
 * `{"json": …}` envelope. Queries are not used — everything else goes through
 * the REST v1 API.
 */
@Singleton
class TrpcClient @Inject constructor(
    private val http: HttpClient,
    private val json: Json,
) {

    /**
     * @return the `result.data.json` payload.
     * @throws ApiException for every transport/protocol failure.
     */
    suspend fun mutate(
        serverAddress: String,
        procedure: String,
        input: JsonElement,
        bearer: String? = null,
    ): JsonElement {
        val body = buildJsonObject { put("json", input) }.toString()

        val (status, responseBody) = try {
            val response = http.post("$serverAddress/api/trpc/$procedure") {
                contentType(ContentType.Application.Json)
                if (bearer != null) header(HttpHeaders.Authorization, "Bearer $bearer")
                setBody(body)
            }
            response.status.value to response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.toApiException()
        }

        val success = status in 200..299
        if (!success) throw parseTrpcError(json, status, responseBody)

        val payload = parseTrpcSuccess(json, responseBody)
            ?: throw ApiException.Malformed(null)
        return payload
    }
}
