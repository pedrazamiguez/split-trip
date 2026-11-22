package es.pedrazamiguez.splittrip.domain.service.featuregate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManualReceiptQuotaTest {

    @Test
    fun `free user below limit can attach and has remaining count`() {
        val quota = ManualReceiptQuota(
            usedAttachments = 2,
            monthlyFreeLimit = 5,
            isProTier = false,
            isGatingEnabled = true
        )

        assertEquals(3, quota.remainingAttachments)
        assertTrue(quota.canAttach)
        assertTrue(quota.isFreeQuotaTracked)
        assertFalse(quota.requiresUpgrade)
    }

    @Test
    fun `free user at limit cannot attach and requires upgrade`() {
        val quota = ManualReceiptQuota(
            usedAttachments = 5,
            monthlyFreeLimit = 5,
            isProTier = false,
            isGatingEnabled = true
        )

        assertEquals(0, quota.remainingAttachments)
        assertFalse(quota.canAttach)
        assertTrue(quota.isFreeQuotaTracked)
        assertTrue(quota.requiresUpgrade)
    }

    @Test
    fun `free user exceeding limit has clamped zero remaining and requires upgrade`() {
        val quota = ManualReceiptQuota(
            usedAttachments = 7,
            monthlyFreeLimit = 5,
            isProTier = false,
            isGatingEnabled = true
        )

        assertEquals(0, quota.remainingAttachments)
        assertFalse(quota.canAttach)
        assertTrue(quota.requiresUpgrade)
    }

    @Test
    fun `pro user is unlimited regardless of attachment count`() {
        val quota = ManualReceiptQuota(
            usedAttachments = 50,
            monthlyFreeLimit = 5,
            isProTier = true,
            isGatingEnabled = true
        )

        assertNull(quota.remainingAttachments)
        assertTrue(quota.canAttach)
        assertFalse(quota.isFreeQuotaTracked)
        assertFalse(quota.requiresUpgrade)
    }

    @Test
    fun `gating disabled allows free user at limit without upgrade`() {
        val quota = ManualReceiptQuota(
            usedAttachments = 5,
            monthlyFreeLimit = 5,
            isProTier = false,
            isGatingEnabled = false
        )

        assertNull(quota.remainingAttachments)
        assertTrue(quota.canAttach)
        assertFalse(quota.isFreeQuotaTracked)
        assertFalse(quota.requiresUpgrade)
    }
}
