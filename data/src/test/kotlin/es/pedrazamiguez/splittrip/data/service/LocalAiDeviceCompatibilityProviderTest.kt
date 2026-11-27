package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalog
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalAiDeviceCompatibilityProviderTest {

    private val context: Context = mockk(relaxed = true)

    private val enabledConfig = ModelPackConfig(
        packId = "gemma-litert-receipt-v1",
        version = "1.0.0",
        downloadUrl = "https://example.com/model.litertlm",
        sha256Checksum = "abc123",
        sizeBytes = 500_000_000L,
        minRamMb = 4096,
        minFreeStorageBytes = 1_073_741_824L,
        isEnabled = true
    )

    private val compactPack = ModelPackConfig(
        packId = "qwen3-0.6b-litert-v1",
        displayName = "Compact (Qwen 3 0.6B)",
        tier = ModelPackTier.COMPACT,
        priority = 10,
        version = "1.0.0",
        downloadUrl = "https://models.splittrip.eu/compact.litertlm",
        sha256Checksum = "a".repeat(64),
        sizeBytes = 343_490_560L,
        minRamMb = 2048,
        minFreeStorageBytes = 1_073_741_824L,
        isEnabled = true
    )

    private val balancedPack = ModelPackConfig(
        packId = "qwen2.5-1.5b-litert-v1",
        displayName = "Balanced (Qwen 2.5 1.5B)",
        tier = ModelPackTier.BALANCED,
        priority = 20,
        version = "1.0.0",
        downloadUrl = "https://models.splittrip.eu/balanced.litertlm",
        sha256Checksum = "b".repeat(64),
        sizeBytes = 1_597_922_856L,
        minRamMb = 3072,
        minFreeStorageBytes = 3_221_225_472L,
        isEnabled = true
    )

    private val highQualityPack = ModelPackConfig(
        packId = "gemma4-e2b-litert-v1",
        displayName = "High Quality (Gemma 4 E2B)",
        tier = ModelPackTier.HIGH_QUALITY,
        priority = 30,
        version = "1.0.0",
        downloadUrl = "https://models.splittrip.eu/high.litertlm",
        sha256Checksum = "c".repeat(64),
        sizeBytes = 2_588_147_712L,
        minRamMb = 6144,
        minFreeStorageBytes = 5_368_709_120L,
        isEnabled = true
    )

    private val fullCatalog = ModelPackCatalog(packs = listOf(highQualityPack, compactPack, balancedPack))

    @Test
    fun `returns compatible when pack enabled, arm64-v8a present, RAM and storage sufficient`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 8192L },
            availableStorageBytesProvider = { 4_000_000_000L },
            supportedAbisProvider = { listOf("arm64-v8a", "armeabi-v7a") }
        )

        val result = provider.evaluate(enabledConfig)

        assertTrue(result.isCompatible)
        assertNull(result.reason)
        assertEquals(8192L, result.availableRamMb)
        assertEquals(4096, result.requiredRamMb)
    }

    @Test
    fun `returns PACK_DISABLED when config isEnabled is false`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 8192L },
            availableStorageBytesProvider = { 4_000_000_000L },
            supportedAbisProvider = { listOf("arm64-v8a") }
        )

        val result = provider.evaluate(enabledConfig.copy(isEnabled = false))

        assertFalse(result.isCompatible)
        assertEquals(ModelPackIncompatibilityReason.PACK_DISABLED, result.reason)
    }

    @Test
    fun `returns UNSUPPORTED_ABI when arm64-v8a is absent`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 8192L },
            availableStorageBytesProvider = { 4_000_000_000L },
            supportedAbisProvider = { listOf("x86_64", "armeabi-v7a") }
        )

        val result = provider.evaluate(enabledConfig)

        assertFalse(result.isCompatible)
        assertEquals(ModelPackIncompatibilityReason.UNSUPPORTED_ABI, result.reason)
    }

    @Test
    fun `returns INSUFFICIENT_RAM when device RAM is below minRamMb`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 3072L },
            availableStorageBytesProvider = { 4_000_000_000L },
            supportedAbisProvider = { listOf("arm64-v8a") }
        )

        val result = provider.evaluate(enabledConfig)

        assertFalse(result.isCompatible)
        assertEquals(ModelPackIncompatibilityReason.INSUFFICIENT_RAM, result.reason)
    }

    @Test
    fun `returns INSUFFICIENT_STORAGE when free storage is below required threshold`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 8192L },
            availableStorageBytesProvider = { 500_000_000L },
            supportedAbisProvider = { listOf("arm64-v8a") }
        )

        val result = provider.evaluate(enabledConfig)

        assertFalse(result.isCompatible)
        assertEquals(ModelPackIncompatibilityReason.INSUFFICIENT_STORAGE, result.reason)
    }

    @Test
    fun `skips storage check when requireStorageCheck is false for already installed pack`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 8192L },
            availableStorageBytesProvider = { 100_000L },
            supportedAbisProvider = { listOf("arm64-v8a") }
        )

        val result = provider.evaluate(enabledConfig, requireStorageCheck = false)

        assertTrue(result.isCompatible)
        assertNull(result.reason)
    }

    @Test
    fun `evaluateCatalog ranks highest-priority compatible pack as recommended on high-RAM device`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 8192L },
            availableStorageBytesProvider = { 10_000_000_000L },
            supportedAbisProvider = { listOf("arm64-v8a") }
        )

        val evaluated = provider.evaluateCatalog(fullCatalog)

        assertEquals(
            listOf("qwen3-0.6b-litert-v1", "qwen2.5-1.5b-litert-v1", "gemma4-e2b-litert-v1"),
            evaluated.map { it.config.packId }
        )
        assertEquals("gemma4-e2b-litert-v1", evaluated.first { it.isRecommended }.config.packId)
        assertTrue(evaluated.all { it.compatibility.isCompatible })
    }

    @Test
    fun `evaluateCatalog marks high-tier pack INSUFFICIENT_RAM and recommends balanced pack on mid-RAM device`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 4096L },
            availableStorageBytesProvider = { 10_000_000_000L },
            supportedAbisProvider = { listOf("arm64-v8a") }
        )

        val evaluated = provider.evaluateCatalog(fullCatalog)

        assertEquals("qwen2.5-1.5b-litert-v1", evaluated.first { it.isRecommended }.config.packId)
        val high = evaluated.first { it.config.packId == "gemma4-e2b-litert-v1" }
        assertFalse(high.compatibility.isCompatible)
        assertEquals(ModelPackIncompatibilityReason.INSUFFICIENT_RAM, high.compatibility.reason)
    }

    @Test
    fun `evaluateCatalog recommends compact on 2 GB RAM device while balanced and high are INSUFFICIENT_RAM`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 2048L },
            availableStorageBytesProvider = { 10_000_000_000L },
            supportedAbisProvider = { listOf("arm64-v8a") }
        )

        val evaluated = provider.evaluateCatalog(fullCatalog)

        assertEquals("qwen3-0.6b-litert-v1", evaluated.first { it.isRecommended }.config.packId)
        assertEquals(
            ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
            evaluated.first { it.config.packId == "qwen2.5-1.5b-litert-v1" }.compatibility.reason
        )
        assertEquals(
            ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
            evaluated.first { it.config.packId == "gemma4-e2b-litert-v1" }.compatibility.reason
        )
    }

    @Test
    fun `evaluateCatalog recommends compact pack when mid and high tiers fail storage check`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 8192L },
            availableStorageBytesProvider = { 1_500_000_000L },
            supportedAbisProvider = { listOf("arm64-v8a") }
        )

        val evaluated = provider.evaluateCatalog(fullCatalog)

        assertEquals("qwen3-0.6b-litert-v1", evaluated.first { it.isRecommended }.config.packId)
        assertEquals(
            ModelPackIncompatibilityReason.INSUFFICIENT_STORAGE,
            evaluated.first { it.config.packId == "qwen2.5-1.5b-litert-v1" }.compatibility.reason
        )
        assertEquals(
            ModelPackIncompatibilityReason.INSUFFICIENT_STORAGE,
            evaluated.first { it.config.packId == "gemma4-e2b-litert-v1" }.compatibility.reason
        )
    }

    @Test
    fun `evaluateCatalog skips storage check only for already installed pack`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 8192L },
            availableStorageBytesProvider = { 100_000L },
            supportedAbisProvider = { listOf("arm64-v8a") }
        )

        val evaluated = provider.evaluateCatalog(fullCatalog, installedPackId = "qwen2.5-1.5b-litert-v1")

        assertTrue(evaluated.first { it.config.packId == "qwen2.5-1.5b-litert-v1" }.compatibility.isCompatible)
        assertFalse(evaluated.first { it.config.packId == "qwen3-0.6b-litert-v1" }.compatibility.isCompatible)
        assertFalse(evaluated.first { it.config.packId == "gemma4-e2b-litert-v1" }.compatibility.isCompatible)
        val aggregate = provider.selectAggregateCompatibility(evaluated, installedPackId = "qwen2.5-1.5b-litert-v1")
        assertTrue(aggregate.isCompatible)
        assertEquals(3072, aggregate.requiredRamMb)
    }

    @Test
    fun `selectAggregateCompatibility returns lowest-requirement pack failure when no pack is compatible`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 1536L },
            availableStorageBytesProvider = { 10_000_000_000L },
            supportedAbisProvider = { listOf("arm64-v8a") }
        )

        val evaluated = provider.evaluateCatalog(fullCatalog)
        val aggregate = provider.selectAggregateCompatibility(evaluated)

        assertFalse(aggregate.isCompatible)
        assertEquals(ModelPackIncompatibilityReason.INSUFFICIENT_RAM, aggregate.reason)
        assertEquals(2048, aggregate.requiredRamMb)
    }

    @Test
    fun `evaluateCatalog returns empty and selectAggregateCompatibility returns PACK_DISABLED when all disabled`() {
        val provider = LocalAiDeviceCompatibilityProvider(
            context = context,
            totalRamMbProvider = { 8192L },
            availableStorageBytesProvider = { 10_000_000_000L },
            supportedAbisProvider = { listOf("arm64-v8a") }
        )
        val disabledCatalog = ModelPackCatalog(packs = fullCatalog.packs.map { it.copy(isEnabled = false) })

        val evaluated = provider.evaluateCatalog(disabledCatalog)
        val aggregate = provider.selectAggregateCompatibility(evaluated)

        assertTrue(evaluated.isEmpty())
        assertFalse(aggregate.isCompatible)
        assertEquals(ModelPackIncompatibilityReason.PACK_DISABLED, aggregate.reason)
        assertEquals(0, aggregate.requiredRamMb)
        assertEquals(0L, aggregate.requiredStorageBytes)
    }
}
