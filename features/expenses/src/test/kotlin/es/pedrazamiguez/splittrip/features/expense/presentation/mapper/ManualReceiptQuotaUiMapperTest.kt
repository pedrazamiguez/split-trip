package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.domain.service.featuregate.ManualReceiptQuota
import es.pedrazamiguez.splittrip.features.expense.R
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManualReceiptQuotaUiMapperTest {

    private val resourceProvider: ResourceProvider = mockk()
    private val mapper = ManualReceiptQuotaUiMapper(resourceProvider)

    @Test
    fun `free user with remaining attachments formats quantity label and is not gated`() {
        every {
            resourceProvider.getQuantityString(
                R.plurals.add_expense_receipt_free_attachments_remaining,
                3,
                3
            )
        } returns "3 free receipts left this month"

        val result = mapper.mapQuotaToUiModel(
            ManualReceiptQuota(
                usedAttachments = 2,
                monthlyFreeLimit = 5,
                isProTier = false,
                isGatingEnabled = true
            )
        )

        assertFalse(result.isManualReceiptGated)
        assertFalse(result.isManualReceiptProFeature)
        assertEquals("3 free receipts left this month", result.remainingAttachmentsLabel)
    }

    @Test
    fun `free user with zero remaining attachments is gated and shows exhausted label`() {
        every {
            resourceProvider.getString(R.string.add_expense_receipt_free_attachments_exhausted)
        } returns "0 free receipts left this month"

        val result = mapper.mapQuotaToUiModel(
            ManualReceiptQuota(
                usedAttachments = 5,
                monthlyFreeLimit = 5,
                isProTier = false,
                isGatingEnabled = true
            )
        )

        assertTrue(result.isManualReceiptGated)
        assertTrue(result.isManualReceiptProFeature)
        assertEquals("0 free receipts left this month", result.remainingAttachmentsLabel)
    }

    @Test
    fun `pro user has null label and is not gated`() {
        val result = mapper.mapQuotaToUiModel(
            ManualReceiptQuota(
                usedAttachments = 10,
                monthlyFreeLimit = 5,
                isProTier = true,
                isGatingEnabled = true
            )
        )

        assertFalse(result.isManualReceiptGated)
        assertFalse(result.isManualReceiptProFeature)
        assertNull(result.remainingAttachmentsLabel)
    }

    @Test
    fun `gating disabled has null label and is not gated`() {
        val result = mapper.mapQuotaToUiModel(
            ManualReceiptQuota(
                usedAttachments = 5,
                monthlyFreeLimit = 5,
                isProTier = false,
                isGatingEnabled = false
            )
        )

        assertFalse(result.isManualReceiptGated)
        assertFalse(result.isManualReceiptProFeature)
        assertNull(result.remainingAttachmentsLabel)
    }

    @Test
    fun `mapBlockedMessage returns string resource with monthly free limit`() {
        val message = mapper.mapBlockedMessage(
            ManualReceiptQuota(
                usedAttachments = 5,
                monthlyFreeLimit = 5,
                isProTier = false,
                isGatingEnabled = true
            )
        )

        assertEquals(
            UiText.StringResource(R.string.add_expense_receipt_monthly_limit_reached, 5),
            message
        )
    }
}
