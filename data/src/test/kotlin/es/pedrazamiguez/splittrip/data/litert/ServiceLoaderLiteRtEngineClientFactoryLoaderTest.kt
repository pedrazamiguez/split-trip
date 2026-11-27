package es.pedrazamiguez.splittrip.data.litert

import android.content.Context
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.util.Collections
import java.util.Enumeration
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServiceLoaderLiteRtEngineClientFactoryLoaderTest {

    @Test
    fun `loadFactory returns factory instance when valid SPI service descriptor is present`() {
        val loader = ServiceLoaderLiteRtEngineClientFactoryLoader(
            classLoaderProvider = {
                InMemoryServiceClassLoader(
                    parent = javaClass.classLoader,
                    serviceResourceContent = FakeLiteRtEngineClientFactory::class.java.name + "\n"
                )
            }
        )

        val factory = loader.loadFactory()

        assertTrue(factory is FakeLiteRtEngineClientFactory)
    }

    @Test
    fun `loadFactory returns null when no SPI service descriptor exists`() {
        val loader = ServiceLoaderLiteRtEngineClientFactoryLoader(
            classLoaderProvider = {
                InMemoryServiceClassLoader(
                    parent = javaClass.classLoader,
                    serviceResourceContent = null
                )
            }
        )

        assertNull(loader.loadFactory())
    }

    @Test
    fun `loadFactory returns null when SPI descriptor references a missing class`() {
        val loader = ServiceLoaderLiteRtEngineClientFactoryLoader(
            classLoaderProvider = {
                InMemoryServiceClassLoader(
                    parent = javaClass.classLoader,
                    serviceResourceContent = "es.pedrazamiguez.splittrip.NonExistentFactoryImpl\n"
                )
            }
        )

        assertNull(loader.loadFactory())
    }

    class FakeLiteRtEngineClientFactory : LiteRtEngineClientFactory {
        override fun create(context: Context): LiteRtEngineClient = mockk(relaxed = true)
    }

    private class InMemoryServiceClassLoader(
        parent: ClassLoader,
        private val serviceResourceContent: String?
    ) : ClassLoader(parent) {

        override fun getResources(name: String): Enumeration<URL> {
            if (name == SERVICE_RESOURCE_PATH && serviceResourceContent != null) {
                val bytes = serviceResourceContent.toByteArray(Charsets.UTF_8)
                val handler = object : URLStreamHandler() {
                    override fun openConnection(u: URL): URLConnection = object : URLConnection(u) {
                        override fun connect() = Unit
                        override fun getInputStream(): InputStream = ByteArrayInputStream(bytes)
                    }
                }

                @Suppress("DEPRECATION")
                val url = URL("memory", "", -1, "/$SERVICE_RESOURCE_PATH", handler)
                return Collections.enumeration(listOf(url))
            }
            return Collections.emptyEnumeration()
        }

        private companion object {
            const val SERVICE_RESOURCE_PATH =
                "META-INF/services/es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClientFactory"
        }
    }
}
