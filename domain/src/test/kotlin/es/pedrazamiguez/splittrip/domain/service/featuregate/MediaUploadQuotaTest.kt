package es.pedrazamiguez.splittrip.domain.service.featuregate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaUploadQuotaTest {

    @Test
    fun `free user under limit has remaining uploads and canUpload true`() {
        val quota = MediaUploadQuota(
            usedUploads = 2,
            monthlyFreeLimit = 5,
            isProTier = false
        )

        assertFalse(quota.isUnlimited)
        assertEquals(5, quota.monthlyLimit)
        assertEquals(3, quota.remainingUploads)
        assertTrue(quota.canUpload)
        assertTrue(quota.isFreeQuotaTracked)
        assertFalse(quota.requiresUpgrade)
    }

    @Test
    fun `free user at limit has zero remaining uploads and requiresUpgrade true`() {
        val quota = MediaUploadQuota(
            usedUploads = 5,
            monthlyFreeLimit = 5,
            isProTier = false
        )

        assertEquals(0, quota.remainingUploads)
        assertFalse(quota.canUpload)
        assertTrue(quota.requiresUpgrade)
    }

    @Test
    fun `free user over limit clamps remaining uploads at zero and requiresUpgrade true`() {
        val quota = MediaUploadQuota(
            usedUploads = 7,
            monthlyFreeLimit = 5,
            isProTier = false
        )

        assertEquals(0, quota.remainingUploads)
        assertFalse(quota.canUpload)
        assertTrue(quota.requiresUpgrade)
    }

    @Test
    fun `pro user is unlimited and does not require upgrade`() {
        val quota = MediaUploadQuota(
            usedUploads = 50,
            monthlyFreeLimit = 5,
            isProTier = true
        )

        assertTrue(quota.isUnlimited)
        assertNull(quota.monthlyLimit)
        assertNull(quota.remainingUploads)
        assertTrue(quota.canUpload)
        assertFalse(quota.isFreeQuotaTracked)
        assertFalse(quota.requiresUpgrade)
    }

    @Test
    fun `gating disabled makes free user unlimited and canUpload true`() {
        val quota = MediaUploadQuota(
            usedUploads = 5,
            monthlyFreeLimit = 5,
            isProTier = false,
            isGatingEnabled = false
        )

        assertTrue(quota.isUnlimited)
        assertNull(quota.monthlyLimit)
        assertNull(quota.remainingUploads)
        assertTrue(quota.canUpload)
        assertFalse(quota.requiresUpgrade)
    }
}
