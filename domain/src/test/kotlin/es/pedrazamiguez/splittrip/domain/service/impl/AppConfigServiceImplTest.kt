package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppConfigServiceImplTest {

    @Test
    fun `exposes general configuration flows from repository correctly`() {
        val repository = mockk<AppConfigRepository>(relaxed = true)
        val defaultCurrencyFlow = MutableStateFlow("EUR")
        val debounceFlow = MutableStateFlow(300L)
        val maxMembersFlow = MutableStateFlow(20)
        val extractedDateMaxFutureDaysFlow = MutableStateFlow(30)
        val supportEmailAddressFlow = MutableStateFlow("support@splittrip.eu")
        val settlementNudgeRateLimitHoursFlow = MutableStateFlow(24L)

        every { repository.defaultCurrencyCode } returns defaultCurrencyFlow
        every { repository.balanceComputationDebounceMs } returns debounceFlow
        every { repository.maxMembersPerGroup } returns maxMembersFlow
        every { repository.extractedDateMaxFutureDays } returns extractedDateMaxFutureDaysFlow
        every { repository.supportEmailAddress } returns supportEmailAddressFlow
        every { repository.settlementNudgeRateLimitHours } returns settlementNudgeRateLimitHoursFlow

        val service = AppConfigServiceImpl(repository)

        assertEquals(defaultCurrencyFlow, service.defaultCurrencyCode)
        assertEquals(debounceFlow, service.balanceComputationDebounceMs)
        assertEquals(maxMembersFlow, service.maxMembersPerGroup)
        assertEquals(extractedDateMaxFutureDaysFlow, service.extractedDateMaxFutureDays)
        assertEquals(supportEmailAddressFlow, service.supportEmailAddress)
        assertEquals(settlementNudgeRateLimitHoursFlow, service.settlementNudgeRateLimitHours)
        assertEquals("EUR", service.defaultCurrencyCode.value)
        assertEquals(300L, service.balanceComputationDebounceMs.value)
        assertEquals(20, service.maxMembersPerGroup.value)
        assertEquals(30, service.extractedDateMaxFutureDays.value)
        assertEquals("support@splittrip.eu", service.supportEmailAddress.value)
        assertEquals(24L, service.settlementNudgeRateLimitHours.value)
    }

    @Test
    fun `exposes tier limits and subscription flows from repository correctly`() {
        val repository = mockk<AppConfigRepository>(relaxed = true)
        val subscriptionGatingEnabledFlow = MutableStateFlow(true)
        val billingSimulationEnabledFlow = MutableStateFlow(false)
        val maxOwnedGroupsFreeFlow = MutableStateFlow(1)
        val maxOwnedGroupsProFlow = MutableStateFlow(100)
        val maxMembersPerGroupFreeFlow = MutableStateFlow(4)
        val maxMembersPerGroupProFlow = MutableStateFlow(20)
        val aiReceiptMonthlyLimitFreeFlow = MutableStateFlow(0)
        val aiReceiptMonthlyLimitProFlow = MutableStateFlow(100)

        every { repository.subscriptionGatingEnabled } returns subscriptionGatingEnabledFlow
        every { repository.billingSimulationEnabled } returns billingSimulationEnabledFlow
        every { repository.maxOwnedGroupsFree } returns maxOwnedGroupsFreeFlow
        every { repository.maxOwnedGroupsPro } returns maxOwnedGroupsProFlow
        every { repository.maxMembersPerGroupFree } returns maxMembersPerGroupFreeFlow
        every { repository.maxMembersPerGroupPro } returns maxMembersPerGroupProFlow
        every { repository.aiReceiptMonthlyLimitFree } returns aiReceiptMonthlyLimitFreeFlow
        every { repository.aiReceiptMonthlyLimitPro } returns aiReceiptMonthlyLimitProFlow

        val service = AppConfigServiceImpl(repository)

        assertEquals(subscriptionGatingEnabledFlow, service.subscriptionGatingEnabled)
        assertEquals(billingSimulationEnabledFlow, service.billingSimulationEnabled)
        assertEquals(maxOwnedGroupsFreeFlow, service.maxOwnedGroupsFree)
        assertEquals(maxOwnedGroupsProFlow, service.maxOwnedGroupsPro)
        assertEquals(maxMembersPerGroupFreeFlow, service.maxMembersPerGroupFree)
        assertEquals(maxMembersPerGroupProFlow, service.maxMembersPerGroupPro)
        assertEquals(aiReceiptMonthlyLimitFreeFlow, service.aiReceiptMonthlyLimitFree)
        assertEquals(aiReceiptMonthlyLimitProFlow, service.aiReceiptMonthlyLimitPro)
        assertEquals(true, service.subscriptionGatingEnabled.value)
        assertEquals(false, service.billingSimulationEnabled.value)
        assertEquals(1, service.maxOwnedGroupsFree.value)
        assertEquals(100, service.maxOwnedGroupsPro.value)
        assertEquals(4, service.maxMembersPerGroupFree.value)
        assertEquals(20, service.maxMembersPerGroupPro.value)
        assertEquals(0, service.aiReceiptMonthlyLimitFree.value)
        assertEquals(100, service.aiReceiptMonthlyLimitPro.value)
    }

    @Test
    fun `exposes developerAllowlistEmailHashes flow from repository correctly`() {
        val repository = mockk<AppConfigRepository>(relaxed = true)
        val allowlistFlow = MutableStateFlow(setOf("abc123"))

        every { repository.developerAllowlistEmailHashes } returns allowlistFlow

        val service = AppConfigServiceImpl(repository)

        assertEquals(allowlistFlow, service.developerAllowlistEmailHashes)
        assertEquals(setOf("abc123"), service.developerAllowlistEmailHashes.value)
    }

    @Test
    fun `exposes proAllowlistEmailHashes flow from repository correctly`() {
        val repository = mockk<AppConfigRepository>(relaxed = true)
        val allowlistFlow = MutableStateFlow(setOf("abc123"))

        every { repository.proAllowlistEmailHashes } returns allowlistFlow

        val service = AppConfigServiceImpl(repository)

        assertEquals(allowlistFlow, service.proAllowlistEmailHashes)
        assertEquals(setOf("abc123"), service.proAllowlistEmailHashes.value)
    }

    @Test
    fun `exposes app update policy and initial fetch settled flows from repository correctly`() {
        val repository = mockk<AppConfigRepository>(relaxed = true)
        val inAppUpdatesEnabledFlow = MutableStateFlow(true)
        val minSupportedVersionCodeFlow = MutableStateFlow(1_000_000L)
        val autoForcePatchUpdatesFlow = MutableStateFlow(false)
        val optionalUpdateVersionCodesFlow = MutableStateFlow(setOf(1_001_000L, 1_002_001L))
        val optionalUpdateCooldownHoursFlow = MutableStateFlow(48L)
        val isInitialFetchSettledFlow = MutableStateFlow(true)

        every { repository.inAppUpdatesEnabled } returns inAppUpdatesEnabledFlow
        every { repository.minSupportedVersionCode } returns minSupportedVersionCodeFlow
        every { repository.autoForcePatchUpdates } returns autoForcePatchUpdatesFlow
        every { repository.optionalUpdateVersionCodes } returns optionalUpdateVersionCodesFlow
        every { repository.optionalUpdateCooldownHours } returns optionalUpdateCooldownHoursFlow
        every { repository.isInitialFetchSettled } returns isInitialFetchSettledFlow

        val service = AppConfigServiceImpl(repository)

        assertEquals(inAppUpdatesEnabledFlow, service.inAppUpdatesEnabled)
        assertEquals(minSupportedVersionCodeFlow, service.minSupportedVersionCode)
        assertEquals(autoForcePatchUpdatesFlow, service.autoForcePatchUpdates)
        assertEquals(optionalUpdateVersionCodesFlow, service.optionalUpdateVersionCodes)
        assertEquals(optionalUpdateCooldownHoursFlow, service.optionalUpdateCooldownHours)
        assertEquals(isInitialFetchSettledFlow, service.isInitialFetchSettled)
        assertEquals(true, service.inAppUpdatesEnabled.value)
        assertEquals(1_000_000L, service.minSupportedVersionCode.value)
        assertEquals(false, service.autoForcePatchUpdates.value)
        assertEquals(setOf(1_001_000L, 1_002_001L), service.optionalUpdateVersionCodes.value)
        assertEquals(48L, service.optionalUpdateCooldownHours.value)
        assertEquals(true, service.isInitialFetchSettled.value)
    }
}
