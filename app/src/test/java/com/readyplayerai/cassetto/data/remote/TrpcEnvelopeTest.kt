package com.readyplayerai.cassetto.data.remote

import com.readyplayerai.cassetto.data.remote.dto.ApiKeyDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrpcEnvelopeTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `success envelope with meta values decodes`() {
        val body =
            """{"result":{"data":{"json":{"id":"abc","name":"Cassetto (ab12c)","key":"kk_live_123","createdAt":"2026-10-26T22:19:34.000Z","scopes":["fullaccess"]},"meta":{"values":{"createdAt":["Date"]}}}}}"""

        val payload = parseTrpcSuccess(json, body)
        val obj = payload!!.jsonObject
        assertEquals("kk_live_123", obj["key"]!!.jsonPrimitive.content)

        val dto = json.decodeFromJsonElement(ApiKeyDto.serializer(), payload)
        assertEquals("abc", dto.id)
        assertEquals(listOf("fullaccess"), dto.scopes)
    }

    @Test
    fun `success envelope without meta decodes`() {
        val body =
            """{"result":{"data":{"json":{"id":"abc","name":"Cassetto (ab12c)","key":"kk_live_123","createdAt":"2026-10-26T22:19:34.000Z","scopes":["fullaccess"]}}}}"""

        val payload = parseTrpcSuccess(json, body)
        val dto = json.decodeFromJsonElement(ApiKeyDto.serializer(), payload!!)
        assertEquals("abc", dto.id)
        assertEquals("kk_live_123", dto.key)
    }

    @Test
    fun `void payload is recognised`() {
        val body = """{"result":{"data":{"json":null}}}"""
        assertTrue(parseTrpcSuccess(json, body).isVoidPayload())
    }

    @Test
    fun `unparsable body yields null`() {
        assertEquals(null, parseTrpcSuccess(json, "<html>gateway error</html>"))
    }

    @Test
    fun `unauthorized code maps to Unauthorized`() {
        val body =
            """{"error":{"json":{"message":"UNAUTHORIZED","code":-32001,"data":{"code":"UNAUTHORIZED","httpStatus":401,"path":"apiKeys.validate","zodError":null}}}}"""
        assertTrue(parseTrpcError(json, 401, body) is ApiException.Unauthorized)
    }

    @Test
    fun `zod bad request surfaces its first message`() {
        val body =
            """{"error":{"json":{"message":"[...]","code":-32600,"data":{"code":"BAD_REQUEST","httpStatus":400,"path":"apiKeys.exchange","zodError":[{"code":"invalid_type","path":["email"],"message":"Invalid email"}]}}}}"""
        val error = parseTrpcError(json, 400, body) as ApiException.BadRequest
        assertEquals("Invalid email", error.detail)
    }

    @Test
    fun `too many requests code maps to RateLimited`() {
        val body =
            """{"error":{"json":{"message":"rate limited","code":-32029,"data":{"code":"TOO_MANY_REQUESTS","httpStatus":429,"path":"apiKeys.exchange","zodError":null}}}}"""
        assertTrue(parseTrpcError(json, 429, body) is ApiException.RateLimited)
    }

    @Test
    fun `plain text 401 maps to Unauthorized`() {
        assertTrue(parseTrpcError(json, 401, "Unauthorized") is ApiException.Unauthorized)
    }

    @Test
    fun `plain text 502 maps to ServerError`() {
        val error = parseTrpcError(json, 502, "bad gateway") as ApiException.ServerError
        assertEquals(502, error.httpStatus)
    }
}
