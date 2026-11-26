package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
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
}
