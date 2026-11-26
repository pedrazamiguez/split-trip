package es.pedrazamiguez.splittrip.data.service

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason

internal class LocalAiDeviceCompatibilityProvider(
    private val context: Context,
    private val totalRamMbProvider: () -> Long = { readDeviceTotalRamMb(context) },
    private val availableStorageBytesProvider: () -> Long = { readAvailableStorageBytes(context) },
    private val supportedAbisProvider: () -> List<String> = { Build.SUPPORTED_ABIS.toList() }
) {
    fun evaluate(
        config: ModelPackConfig,
        requireStorageCheck: Boolean = true
    ): DeviceCompatibilityResult {
        val availableRamMb = totalRamMbProvider().coerceAtLeast(0L)
        val availableStorageBytes = availableStorageBytesProvider().coerceAtLeast(0L)
        val supportedAbis = supportedAbisProvider()
        val requiredStorageBytes = maxOf(config.minFreeStorageBytes, config.sizeBytes * STORAGE_HEADROOM_MULTIPLIER)

        val reason = when {
            !config.isEnabled -> ModelPackIncompatibilityReason.PACK_DISABLED
            REQUIRED_ABI !in supportedAbis -> ModelPackIncompatibilityReason.UNSUPPORTED_ABI
            availableRamMb < config.minRamMb -> ModelPackIncompatibilityReason.INSUFFICIENT_RAM
            requireStorageCheck && availableStorageBytes < requiredStorageBytes ->
                ModelPackIncompatibilityReason.INSUFFICIENT_STORAGE
            else -> null
        }

        return DeviceCompatibilityResult(
            isCompatible = reason == null,
            reason = reason,
            availableRamMb = availableRamMb,
            requiredRamMb = config.minRamMb,
            availableStorageBytes = availableStorageBytes,
            requiredStorageBytes = requiredStorageBytes,
            supportedAbis = supportedAbis
        )
    }

    private companion object {
        private const val REQUIRED_ABI = "arm64-v8a"
        private const val BYTES_PER_MIB = 1_048_576L
        private const val STORAGE_HEADROOM_MULTIPLIER = 2L

        private fun readDeviceTotalRamMb(context: Context): Long {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return 0L
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            return memoryInfo.totalMem / BYTES_PER_MIB
        }

        private fun readAvailableStorageBytes(context: Context): Long {
            val targetDir = context.filesDir ?: return 0L
            return runCatching { StatFs(targetDir.absolutePath).availableBytes }.getOrDefault(0L)
        }
    }
}
