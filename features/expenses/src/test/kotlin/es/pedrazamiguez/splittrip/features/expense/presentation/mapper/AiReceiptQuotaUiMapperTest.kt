package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.domain.service.featuregate.AiReceiptQuota
import es.pedrazamiguez.splittrip.features.expense.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AiReceiptQuotaUiMapperTest {

    private lateinit var resourceProvider: ResourceProvider
    private lateinit var mapper: AiReceiptQuotaUiMapper

    @BeforeEach
    fun setUp() {
        resourceProvider = mockk()
        mapper = AiReceiptQuotaUiMapper(resourceProvider)
    }

    @Test
    fun `mapQuotaToUiModel returns formatted quantity string for free user with remaining trial scans`() {
        every {
            resourceProvider.getQuantityString(R.plurals.expense_autofill_free_scans_remaining, 2, 2)
        } returns "2 free scans left this month"

        val result = mapper.mapQuotaToUiModel(
            AiReceiptQuota(usedScans = 1, monthlyLimit = 3, isProTier = false)
        )

        assertFalse(result.isAiGated)
        assertFalse(result.isAiProFeature)
        assertEquals("2 free scans left this month", result.remainingScansLabel)
        verify(exactly = 1) {
            resourceProvider.getQuantityString(R.plurals.expense_autofill_free_scans_remaining, 2, 2)
        }
    }

    @Test
    fun `mapQuotaToUiModel returns exhausted free label and gated pro flags when free trial is exhausted`() {
        every {
            resourceProvider.getString(R.string.expense_autofill_free_scans_exhausted)
        } returns "0 free scans left this month"

        val result = mapper.mapQuotaToUiModel(
            AiReceiptQuota(usedScans = 3, monthlyLimit = 3, isProTier = false)
        )

        assertTrue(result.isAiGated)
        assertTrue(result.isAiProFeature)
        assertEquals("0 free scans left this month", result.remainingScansLabel)
        verify(exactly = 1) {
            resourceProvider.getString(R.string.expense_autofill_free_scans_exhausted)
        }
    }

    @Test
    fun `mapQuotaToUiModel returns null label and gated pro flags when free limit is zero`() {
        val result = mapper.mapQuotaToUiModel(
            AiReceiptQuota(usedScans = 0, monthlyLimit = 0, isProTier = false)
        )

        assertTrue(result.isAiGated)
        assertTrue(result.isAiProFeature)
        assertNull(result.remainingScansLabel)
    }

    @Test
    fun `mapQuotaToUiModel returns null label and ungated flags for pro user with remaining scans`() {
        val result = mapper.mapQuotaToUiModel(
            AiReceiptQuota(usedScans = 10, monthlyLimit = 100, isProTier = true)
        )

        assertFalse(result.isAiGated)
        assertFalse(result.isAiProFeature)
        assertNull(result.remainingScansLabel)
    }

    @Test
    fun `mapQuotaToUiModel returns exhausted label without pro flag when pro user reaches monthly cap`() {
        every {
            resourceProvider.getString(R.string.expense_autofill_monthly_scans_exhausted)
        } returns "Monthly scan limit reached"

        val result = mapper.mapQuotaToUiModel(
            AiReceiptQuota(usedScans = 100, monthlyLimit = 100, isProTier = true)
        )

        assertTrue(result.isAiGated)
        assertFalse(result.isAiProFeature)
        assertEquals("Monthly scan limit reached", result.remainingScansLabel)
        verify(exactly = 1) {
            resourceProvider.getString(R.string.expense_autofill_monthly_scans_exhausted)
        }
    }

    @Test
    fun `mapQuotaToUiModel returns null label and ungated flags when subscription gating is disabled`() {
        val result = mapper.mapQuotaToUiModel(
            AiReceiptQuota(usedScans = 3, monthlyLimit = 3, isProTier = false, isGatingEnabled = false)
        )

        assertFalse(result.isAiGated)
        assertFalse(result.isAiProFeature)
        assertNull(result.remainingScansLabel)
    }

    @Test
    fun `mapBlockedMessage returns monthly limit message for exhausted free trial`() {
        val message = mapper.mapBlockedMessage(
            AiReceiptQuota(usedScans = 3, monthlyLimit = 3, isProTier = false)
        )

        assertEquals(
            UiText.StringResource(R.string.expense_autofill_monthly_limit_reached, 3),
            message
        )
    }

    @Test
    fun `mapBlockedMessage returns pro required message when free limit is zero`() {
        val message = mapper.mapBlockedMessage(
            AiReceiptQuota(usedScans = 0, monthlyLimit = 0, isProTier = false)
        )

        assertEquals(
            UiText.StringResource(R.string.expense_autofill_pro_required),
            message
        )
    }

    @Test
    fun `mapBlockedMessage returns pro monthly limit message when pro cap is reached`() {
        val message = mapper.mapBlockedMessage(
            AiReceiptQuota(usedScans = 100, monthlyLimit = 100, isProTier = true)
        )

        assertEquals(
            UiText.StringResource(R.string.expense_autofill_pro_monthly_limit_reached, 100),
            message
        )
    }

    @Test
    fun `mapQuotaToUiModel returns null label and ungated flags for unlimited pro user with null monthlyLimit`() {
        val result = mapper.mapQuotaToUiModel(
            AiReceiptQuota(usedScans = 150, monthlyLimit = null, isProTier = true)
        )

        assertFalse(result.isAiGated)
        assertFalse(result.isAiProFeature)
        assertNull(result.remainingScansLabel)
    }

    @Test
    fun `mapQuotaToUiModel returns null label and ungated flags for unlimited pro user with negative monthlyLimit`() {
        val result = mapper.mapQuotaToUiModel(
            AiReceiptQuota(usedScans = 50, monthlyLimit = -1, isProTier = true)
        )

        assertFalse(result.isAiGated)
        assertFalse(result.isAiProFeature)
        assertNull(result.remainingScansLabel)
    }
}
