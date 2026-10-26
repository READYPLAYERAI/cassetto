package com.readyplayerai.cassetto.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerUrlTest {

    @Test
    fun `default address passes through unchanged`() {
        assertEquals(
            ServerUrl.DEFAULT,
            ServerUrl.normalize(ServerUrl.DEFAULT),
        )
    }

    @Test
    fun `trailing slash is removed`() {
        assertEquals("https://host", ServerUrl.normalize("https://host/"))
    }

    @Test
    fun `multiple trailing slashes are removed`() {
        assertEquals("https://host", ServerUrl.normalize("https://host///"))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("https://host", ServerUrl.normalize("  https://host  "))
    }

    @Test
    fun `api suffix is stripped`() {
        assertEquals("https://host", ServerUrl.normalize("https://host/api"))
    }

    @Test
    fun `api v1 suffix is stripped`() {
        assertEquals("https://host", ServerUrl.normalize("https://host/api/v1"))
    }

    @Test
    fun `api suffix with trailing slash is stripped`() {
        assertEquals("https://host", ServerUrl.normalize("https://host/api/"))
    }

    @Test
    fun `non api subpath is preserved`() {
        assertEquals("https://example.com/karakeep", ServerUrl.normalize("https://example.com/karakeep"))
    }

    @Test
    fun `blank input is rejected as required`() {
        val normalized = ServerUrl.normalize("")
        assertEquals("Server address is required", ServerUrl.validationError(normalized))
    }

    @Test
    fun `whitespace only input is rejected as required`() {
        val normalized = ServerUrl.normalize("   ")
        assertEquals("Server address is required", ServerUrl.validationError(normalized))
    }

    @Test
    fun `missing scheme is rejected`() {
        val normalized = ServerUrl.normalize("host.example.com")
        assertEquals(
            "Server address must start with http:// or https://",
            ServerUrl.validationError(normalized),
        )
    }

    @Test
    fun `ftp scheme is rejected`() {
        val normalized = ServerUrl.normalize("ftp://host")
        assertEquals(
            "Server address must start with http:// or https://",
            ServerUrl.validationError(normalized),
        )
    }

    @Test
    fun `missing host is rejected`() {
        assertEquals("Server address must include a host name", ServerUrl.validationError("https://"))
    }

    @Test
    fun `valid address with port is accepted`() {
        val normalized = ServerUrl.normalize("http://192.168.1.10:3000")
        assertNull(ServerUrl.validationError(normalized))
    }

    @Test
    fun `valid https address is accepted after normalisation`() {
        val normalized = ServerUrl.normalize("https://cloud.karakeep.app/")
        assertNull(ServerUrl.validationError(normalized))
    }

    @Test
    fun `scheme separator is never stripped`() {
        assertEquals("https://", ServerUrl.normalize("https://"))
        assertEquals("http://", ServerUrl.normalize("http://"))
    }

    @Test
    fun `address without host is rejected as missing host`() {
        assertEquals(
            "Server address must include a host name",
            ServerUrl.validationError(ServerUrl.normalize("https://")),
        )
    }

    @Test
    fun `value without scheme separator is reported as a scheme problem`() {
        assertEquals(
            "Server address must start with http:// or https://",
            ServerUrl.validationError(ServerUrl.normalize("https:")),
        )
    }
}
