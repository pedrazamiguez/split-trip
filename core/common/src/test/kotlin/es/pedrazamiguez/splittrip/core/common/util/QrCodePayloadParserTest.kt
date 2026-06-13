package es.pedrazamiguez.splittrip.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QrCodePayloadParserTest {

    @Test
    fun testCreateSharePayload() {
        val payload = QrCodePayloadParser.createSharePayload("test@example.com", "user-123")
        assertEquals("splittrip://user/share?email=test%40example.com&userId=user-123", payload)
    }

    @Test
    fun testParseSharePayloadSuccess() {
        val payloadUri = "splittrip://user/share?email=test%40example.com&userId=user-123"
        val result = QrCodePayloadParser.parseSharePayload(payloadUri)
        assertNotNull(result)
        assertEquals("test@example.com", result?.email)
        assertEquals("user-123", result?.userId)
    }

    @Test
    fun testParseSharePayloadInvalidUri() {
        assertNull(QrCodePayloadParser.parseSharePayload("invalid-uri"))
        assertNull(
            QrCodePayloadParser.parseSharePayload("splittrip://other/share?email=test%40example.com&userId=user-123")
        )
        assertNull(
            QrCodePayloadParser.parseSharePayload("splittrip://user/other?email=test%40example.com&userId=user-123")
        )
        assertNull(QrCodePayloadParser.parseSharePayload("http://user/share?email=test%40example.com&userId=user-123"))
    }

    @Test
    fun testParseSharePayloadMissingParams() {
        assertNull(QrCodePayloadParser.parseSharePayload("splittrip://user/share?email=test%40example.com"))
        assertNull(QrCodePayloadParser.parseSharePayload("splittrip://user/share?userId=user-123"))
    }

    @Test
    fun testParseSharePayloadBlankParams() {
        assertNull(QrCodePayloadParser.parseSharePayload("splittrip://user/share?email=&userId=user-123"))
        assertNull(QrCodePayloadParser.parseSharePayload("splittrip://user/share?email=test%40example.com&userId="))
        assertNull(QrCodePayloadParser.parseSharePayload("splittrip://user/share?email= &userId=user-123"))
    }
}
