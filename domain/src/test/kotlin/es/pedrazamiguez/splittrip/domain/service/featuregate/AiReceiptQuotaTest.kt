package es.pedrazamiguez.splittrip.domain.service.featuregate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiReceiptQuotaTest {

    @Test
    fun `free user with remaining scans can scan and is free trial eligible without requiring upgrade`() {
        val quota = AiReceiptQuota(usedScans = 1, monthlyLimit = 3, isProTier = false)

        assertEquals(2, quota.remainingScans)
        assertTrue(quota.canScan)
        assertTrue(quota.isFreeTrialEligible)
        assertFalse(quota.requiresUpgrade)
    }

    @Test
    fun `free user who exhausted quota cannot scan and requires upgrade`() {
        val quota = AiReceiptQuota(usedScans = 3, monthlyLimit = 3, isProTier = false)

        assertEquals(0, quota.remainingScans)
        assertFalse(quota.canScan)
        assertTrue(quota.isFreeTrialEligible)
        assertTrue(quota.requiresUpgrade)
    }

    @Test
    fun `free user with zero monthly limit cannot scan is not free trial eligible and requires upgrade`() {
        val quota = AiReceiptQuota(usedScans = 0, monthlyLimit = 0, isProTier = false)

        assertEquals(0, quota.remainingScans)
        assertFalse(quota.canScan)
        assertFalse(quota.isFreeTrialEligible)
        assertTrue(quota.requiresUpgrade)
    }

    @Test
    fun `usedScans exceeding monthlyLimit clamps remainingScans to zero`() {
        val quota = AiReceiptQuota(usedScans = 5, monthlyLimit = 3, isProTier = false)

        assertEquals(0, quota.remainingScans)
        assertFalse(quota.canScan)
    }

    @Test
    fun `pro user who exhausted quota cannot scan and does not require upgrade`() {
        val quota = AiReceiptQuota(usedScans = 100, monthlyLimit = 100, isProTier = true)

        assertEquals(0, quota.remainingScans)
        assertFalse(quota.canScan)
        assertFalse(quota.isFreeTrialEligible)
        assertFalse(quota.requiresUpgrade)
    }

    @Test
    fun `gating disabled always allows scan regardless of zero remaining scans`() {
        val quota = AiReceiptQuota(
            usedScans = 3,
            monthlyLimit = 3,
            isProTier = false,
            isGatingEnabled = false
        )

        assertTrue(quota.canScan)
        assertFalse(quota.isFreeTrialEligible)
        assertFalse(quota.requiresUpgrade)
    }
}
