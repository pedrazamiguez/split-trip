package es.pedrazamiguez.splittrip.data.service

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalog
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason

internal data class EvaluatedCatalogPack(
    val config: ModelPackConfig,
    val compatibility: DeviceCompatibilityResult,
    val isRecommended: Boolean
)

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

    fun evaluateCatalog(
        catalog: ModelPackCatalog,
        installedPackId: String? = null
    ): List<EvaluatedCatalogPack> {
        val enabledPacks = catalog.enabledPacks
        if (enabledPacks.isEmpty()) return emptyList()

        val sortedPacks = enabledPacks.sortedWith(
            compareBy<ModelPackConfig> { it.priority }
                .thenBy { it.tier.rank }
                .thenBy { it.packId }
        )

        val evaluatedPacks = sortedPacks.map { pack ->
            EvaluatedCatalogPack(
                config = pack,
                compatibility = evaluate(
                    config = pack,
                    requireStorageCheck = pack.packId != installedPackId
                ),
                isRecommended = false
            )
        }

        val recommendedPackId = evaluatedPacks
            .filter { it.compatibility.isCompatible }
            .maxWithOrNull(
                compareBy<EvaluatedCatalogPack> { it.config.priority }
                    .thenBy { it.config.tier.rank }
                    .thenBy { it.config.minRamMb }
                    .thenByDescending { it.config.packId }
            )
            ?.config
            ?.packId

        return evaluatedPacks.map { entry ->
            entry.copy(isRecommended = entry.config.packId == recommendedPackId)
        }
    }

    fun selectAggregateCompatibility(
        evaluated: List<EvaluatedCatalogPack>,
        installedPackId: String? = null
    ): DeviceCompatibilityResult {
        if (evaluated.isEmpty()) {
            return DeviceCompatibilityResult(
                isCompatible = false,
                reason = ModelPackIncompatibilityReason.PACK_DISABLED,
                availableRamMb = totalRamMbProvider().coerceAtLeast(0L),
                requiredRamMb = 0,
                availableStorageBytes = availableStorageBytesProvider().coerceAtLeast(0L),
                requiredStorageBytes = 0L,
                supportedAbis = supportedAbisProvider()
            )
        }

        evaluated.firstOrNull { it.isRecommended && it.compatibility.isCompatible }?.let {
            return it.compatibility
        }

        if (installedPackId != null) {
            evaluated.firstOrNull {
                it.config.packId == installedPackId && it.compatibility.isCompatible
            }?.let {
                return it.compatibility
            }
        }

        return evaluated.minWith(
            compareBy<EvaluatedCatalogPack> { it.config.minRamMb }
                .thenBy { it.config.minFreeStorageBytes }
                .thenBy { it.config.priority }
        ).compatibility
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
