package es.pedrazamiguez.splittrip.data.litert

import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import timber.log.Timber

internal interface LiteRtEngineClientFactoryLoader {
    fun loadFactory(): LiteRtEngineClientFactory?
    fun isFactoryAvailable(): Boolean = loadFactory() != null
}

internal class ServiceLoaderLiteRtEngineClientFactoryLoader(
    private val classLoaderProvider: () -> ClassLoader
) : LiteRtEngineClientFactoryLoader {

    override fun loadFactory(): LiteRtEngineClientFactory? = try {
        val iterator = ServiceLoader
            .load(LiteRtEngineClientFactory::class.java, classLoaderProvider())
            .iterator()
        if (iterator.hasNext()) iterator.next() else null
    } catch (e: ServiceConfigurationError) {
        Timber.w(e, "Failed to load LiteRtEngineClientFactory via ServiceLoader")
        null
    } catch (e: LinkageError) {
        Timber.w(e, "Linkage error loading LiteRtEngineClientFactory via ServiceLoader")
        null
    }
}
