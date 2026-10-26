package com.readyplayerai.cassetto.data.remote

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Transport and protocol failures, in the only shapes the UI needs to
 * distinguish: "your credentials are wrong", "you are rate limited", "the
 * server said no", "we could not reach it", "the response was garbage".
 *
 * The API key must never appear in these messages.
 */
sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** HTTP 401, or a tRPC `UNAUTHORIZED` code. */
    data object Unauthorized : ApiException("Unauthorized")

    /** HTTP 4xx that is not auth/rate limiting, or a tRPC validation error. */
    data class BadRequest(val detail: String?) : ApiException(detail ?: "Bad request")

    /** HTTP 429 or a tRPC `TOO_MANY_REQUESTS` code. */
    data object RateLimited : ApiException("Rate limited")

    /** HTTP 5xx, or any tRPC code we do not special-case. */
    data class ServerError(val httpStatus: Int, val detail: String?) :
        ApiException("Server error $httpStatus")

    /** DNS failure, refused connection, timeout. */
    class Network(cause: Throwable) : ApiException("Network unavailable", cause)

    /** A response body that could not be parsed as the expected shape. */
    class Malformed(cause: Throwable?) : ApiException("Malformed response", cause)

    /** Anything unclassified. */
    class Unknown(cause: Throwable?) : ApiException(cause?.message ?: "Unknown error", cause)
}

/** Maps a Kotlin/Java exception onto the [ApiException] taxonomy. */
internal fun Throwable.toApiException(): ApiException = when (this) {
    is ApiException -> this
    is SerializationException -> ApiException.Malformed(this)
    is java.io.IOException -> ApiException.Network(this)
    else -> ApiException.Unknown(this)
}

/** Maps a bare HTTP status (no parsable body) onto the taxonomy. */
internal fun httpStatusError(httpStatus: Int, body: String): ApiException {
    val snippet = body.take(200).trim().ifBlank { null }
    return when {
        httpStatus == 401 -> ApiException.Unauthorized
        httpStatus == 429 -> ApiException.RateLimited
        httpStatus in 400..499 -> ApiException.BadRequest(snippet)
        else -> ApiException.ServerError(httpStatus, snippet)
    }
}

/**
 * Extracts `result.data.json` from a tRPC success envelope.
 * @return null when the envelope does not have that shape at all.
 */
internal fun parseTrpcSuccess(json: Json, body: String): JsonElement? {
    val root = try {
        json.parseToJsonElement(body)
    } catch (e: Exception) {
        return null
    }
    val result = (root as? JsonObject)?.get("result") as? JsonObject ?: return null
    val data = result["data"] as? JsonObject ?: return null
    return data["json"]
}

/**
 * Turns a non-2xx tRPC response into the matching [ApiException], preferring
 * `error.json.data.code` over the HTTP status and over the (useless) `message`.
 */
internal fun parseTrpcError(json: Json, httpStatus: Int, body: String): ApiException {
    val root = try {
        json.parseToJsonElement(body)
    } catch (e: Exception) {
        return httpStatusError(httpStatus, body)
    }
    val error = (root as? JsonObject)?.get("error") as? JsonObject ?: return httpStatusError(httpStatus, body)
    val inner = error["json"] as? JsonObject ?: return httpStatusError(httpStatus, body)
    val data = inner["data"] as? JsonObject
    val code = data?.get("code")?.jsonPrimitiveOrNull()?.contentOrNull
    val message = inner["message"]?.jsonPrimitiveOrNull()?.contentOrNull
    return when (code) {
        "UNAUTHORIZED" -> ApiException.Unauthorized
        "BAD_REQUEST", "PARSE_ERROR", "METHOD_NOT_SUPPORTED", "UNPROCESSABLE_CONTENT" ->
            ApiException.BadRequest(zodDetail(data?.get("zodError")) ?: message)
        "TOO_MANY_REQUESTS" -> ApiException.RateLimited
        null -> httpStatusError(httpStatus, body)
        else -> ApiException.ServerError(httpStatus, message ?: code)
    }
}

/** Pulls the first human-readable message out of a zod error tree. */
private fun zodDetail(element: JsonElement?): String? {
    val issues: List<JsonElement> = when (element) {
        is JsonArray -> element.toList()
        is JsonObject -> ((element["issues"] as? JsonArray)?.toList() ?: listOf(element))
        else -> return null
    }
    return issues.firstOrNull()
        ?.let { it as? JsonObject }
        ?.get("message")
        ?.jsonPrimitiveOrNull()
        ?.contentOrNull
}

private fun JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive

/** True for the `{"json":null}` payload that mutations returning void produce. */
internal fun JsonElement?.isVoidPayload(): Boolean = this == null || this == JsonNull
