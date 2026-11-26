package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.features.expense.R
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LocalAiPackBannerUiMapperTest {

    private val usLocaleProvider = object : LocaleProvider {
        override fun getCurrentLocale(): Locale = Locale.US
    }

    private val compatibleResult = DeviceCompatibilityResult(
        isCompatible = true,
        reason = null,
        availableRamMb = 8192L,
        requiredRamMb = 4096,
        availableStorageBytes = 4_000_000_000L,
        requiredStorageBytes = 1_073_741_824L,
        supportedAbis = listOf("arm64-v8a")
    )

    private val mapper = LocalAiPackBannerUiMapper(usLocaleProvider)

    @Test
    fun `returns null when isOptInBannerEligible is false`() {
        val freeState = LocalAiModelPackState(
            status = ModelPackStatus.NotInstalled("1.0.0", 524_288_000L),
            compatibility = compatibleResult,
            isEntitled = false,
            isWifiOnly = true,
            isAiCoreSupported = false,
            activeEngine = AiEngineType.AI_CORE_GEMMA_4
        )
        assertNull(mapper.map(freeState))

        val installedState = freeState.copy(
            isEntitled = true,
            status = ModelPackStatus.Installed("1.0.0", 524_288_000L)
        )
        assertNull(mapper.map(installedState))
    }

    @Test
    fun `returns enable-offline-AI copy when AICore is unavailable`() {
        val state = LocalAiModelPackState(
            status = ModelPackStatus.NotInstalled("1.0.0", 524_288_000L),
            compatibility = compatibleResult,
            isEntitled = true,
            isWifiOnly = true,
            isAiCoreSupported = false,
            activeEngine = AiEngineType.AI_CORE_GEMMA_4
        )

        val banner = mapper.map(state)

        assertNotNull(banner)
        assertEquals(UiText.StringResource(R.string.expense_local_ai_banner_enable_title), banner?.title)
        assertEquals(UiText.StringResource(R.string.expense_local_ai_banner_enable_description), banner?.description)
        assertEquals("500 MB", banner?.sizeBadge)
    }

    @Test
    fun `returns null when AICore is supported`() {
        val state = LocalAiModelPackState(
            status = ModelPackStatus.NotInstalled("1.0.0", 1_610_612_736L),
            compatibility = compatibleResult,
            isEntitled = true,
            isWifiOnly = true,
            isAiCoreSupported = true,
            activeEngine = AiEngineType.AI_CORE_GEMMA_4
        )

        val banner = mapper.map(state)

        assertNull(banner)
    }
}
