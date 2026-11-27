package es.pedrazamiguez.splittrip.dynamic.litertlm

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LiteRtLmEngineClientFactoryImplTest {

    @Test
    fun `create returns LiteRtLmEngineClientImpl bound to applicationContext`() {
        val context = mockk<Context>()
        val applicationContext = mockk<Context>()
        every { context.applicationContext } returns applicationContext

        val factory = LiteRtLmEngineClientFactoryImpl()
        val client = factory.create(context)

        assertTrue(client is LiteRtLmEngineClientImpl)
        verify(exactly = 1) { context.applicationContext }
    }

    @Test
    fun `SPI descriptor resource resolves to LiteRtLmEngineClientFactoryImpl`() {
        val resourcePath =
            "META-INF/services/es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClientFactory"
        val stream = javaClass.classLoader?.getResourceAsStream(resourcePath)

        assertNotNull(stream, "Expected SPI resource at $resourcePath")
        val content = stream!!.bufferedReader().use { it.readText() }.trim()
        assertEquals(LiteRtLmEngineClientFactoryImpl::class.java.name, content)
    }
}
