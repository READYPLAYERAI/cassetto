package com.readyplayerai.cassetto.data.remote

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TrpcErrorMappingTest {

    @Test
    fun `http 401 maps to Unauthorized`() {
        assertTrue(httpStatusError(401, "nope") is ApiException.Unauthorized)
    }

    @Test
    fun `http 429 maps to RateLimited`() {
        assertTrue(httpStatusError(429, "slow down") is ApiException.RateLimited)
    }

    @Test
    fun `http 404 maps to BadRequest with detail`() {
        val error = httpStatusError(404, "Not found") as ApiException.BadRequest
        assertEquals("Not found", error.detail)
    }

    @Test
    fun `http 503 maps to ServerError`() {
        val error = httpStatusError(503, "down") as ApiException.ServerError
        assertEquals(503, error.httpStatus)
        assertEquals("down", error.detail)
    }

    @Test
    fun `io exception maps to Network`() {
        assertTrue(java.io.IOException("boom").toApiException() is ApiException.Network)
    }

    @Test
    fun `serialization exception maps to Malformed`() {
        assertTrue(SerializationException("bad").toApiException() is ApiException.Malformed)
    }

    @Test
    fun `api exception maps to itself`() {
        val original = ApiException.Unauthorized
        assertSame(original, original.toApiException())
    }

    @Test
    fun `unclassified exceptions map to Unknown`() {
        val mapped = IllegalStateException("weird").toApiException()
        assertTrue(mapped is ApiException.Unknown)
    }
}
