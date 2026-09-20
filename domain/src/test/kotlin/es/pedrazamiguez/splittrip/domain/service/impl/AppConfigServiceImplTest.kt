package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppConfigServiceImplTest {

    @Test
    fun `exposes flows from repository correctly`() {
        // Given
        val repository = mockk<AppConfigRepository>()
        val defaultCurrencyFlow = MutableStateFlow("EUR")
        val debounceFlow = MutableStateFlow(300L)
        val maxMembersFlow = MutableStateFlow(20)
        val subscriptionGatingEnabledFlow = MutableStateFlow(true)
        val maxOwnedGroupsFreeFlow = MutableStateFlow(1)
        val maxOwnedGroupsProFlow = MutableStateFlow(100)
        val maxMembersPerGroupFreeFlow = MutableStateFlow(4)
        val maxMembersPerGroupProFlow = MutableStateFlow(20)
        val aiReceiptMonthlyLimitFreeFlow = MutableStateFlow(0)
        val aiReceiptMonthlyLimitProFlow = MutableStateFlow(100)
        val extractedDateMaxFutureDaysFlow = MutableStateFlow(30)
        val supportEmailAddressFlow = MutableStateFlow("support@splittrip.eu")
        val settlementNudgeRateLimitHoursFlow = MutableStateFlow(24L)
        every { repository.defaultCurrencyCode } returns defaultCurrencyFlow
        every { repository.balanceComputationDebounceMs } returns debounceFlow
        every { repository.maxMembersPerGroup } returns maxMembersFlow
        every { repository.subscriptionGatingEnabled } returns subscriptionGatingEnabledFlow
        every { repository.maxOwnedGroupsFree } returns maxOwnedGroupsFreeFlow
        every { repository.maxOwnedGroupsPro } returns maxOwnedGroupsProFlow
        every { repository.maxMembersPerGroupFree } returns maxMembersPerGroupFreeFlow
        every { repository.maxMembersPerGroupPro } returns maxMembersPerGroupProFlow
        every { repository.aiReceiptMonthlyLimitFree } returns aiReceiptMonthlyLimitFreeFlow
        every { repository.aiReceiptMonthlyLimitPro } returns aiReceiptMonthlyLimitProFlow
        every { repository.extractedDateMaxFutureDays } returns extractedDateMaxFutureDaysFlow
        every { repository.supportEmailAddress } returns supportEmailAddressFlow
        every { repository.settlementNudgeRateLimitHours } returns settlementNudgeRateLimitHoursFlow

        // When
        val service = AppConfigServiceImpl(repository)

        // Then
        assertEquals(defaultCurrencyFlow, service.defaultCurrencyCode)
        assertEquals(debounceFlow, service.balanceComputationDebounceMs)
        assertEquals(maxMembersFlow, service.maxMembersPerGroup)
        assertEquals(subscriptionGatingEnabledFlow, service.subscriptionGatingEnabled)
        assertEquals(maxOwnedGroupsFreeFlow, service.maxOwnedGroupsFree)
        assertEquals(maxOwnedGroupsProFlow, service.maxOwnedGroupsPro)
        assertEquals(maxMembersPerGroupFreeFlow, service.maxMembersPerGroupFree)
        assertEquals(maxMembersPerGroupProFlow, service.maxMembersPerGroupPro)
        assertEquals(aiReceiptMonthlyLimitFreeFlow, service.aiReceiptMonthlyLimitFree)
        assertEquals(aiReceiptMonthlyLimitProFlow, service.aiReceiptMonthlyLimitPro)
        assertEquals(extractedDateMaxFutureDaysFlow, service.extractedDateMaxFutureDays)
        assertEquals(supportEmailAddressFlow, service.supportEmailAddress)
        assertEquals(settlementNudgeRateLimitHoursFlow, service.settlementNudgeRateLimitHours)
        assertEquals("EUR", service.defaultCurrencyCode.value)
        assertEquals(300L, service.balanceComputationDebounceMs.value)
        assertEquals(20, service.maxMembersPerGroup.value)
        assertEquals(true, service.subscriptionGatingEnabled.value)
        assertEquals(1, service.maxOwnedGroupsFree.value)
        assertEquals(100, service.maxOwnedGroupsPro.value)
        assertEquals(4, service.maxMembersPerGroupFree.value)
        assertEquals(20, service.maxMembersPerGroupPro.value)
        assertEquals(0, service.aiReceiptMonthlyLimitFree.value)
        assertEquals(100, service.aiReceiptMonthlyLimitPro.value)
        assertEquals(30, service.extractedDateMaxFutureDays.value)
        assertEquals("support@splittrip.eu", service.supportEmailAddress.value)
        assertEquals(24L, service.settlementNudgeRateLimitHours.value)
    }
}
