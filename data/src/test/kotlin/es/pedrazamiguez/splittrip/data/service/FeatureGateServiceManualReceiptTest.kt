package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeatureGateServiceManualReceiptTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var userRepository: UserRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var service: FeatureGateServiceImpl

    private val fixedYearMonth = YearMonth.of(2026, 11)
    private val subscriptionGatingEnabled = MutableStateFlow(true)
    private val manualReceiptMonthlyLimitFree = MutableStateFlow(5)
    private val monthlyManualReceiptCount = MutableStateFlow(0)
    private val proAllowlistEmailHashes = MutableStateFlow<Set<String>>(emptySet())

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        appConfigRepository = mockk(relaxed = true)
        userRepository = mockk()
        groupRepository = mockk()
        userPreferenceRepository = mockk(relaxed = true)

        every { appConfigRepository.subscriptionGatingEnabled } returns subscriptionGatingEnabled
        every { appConfigRepository.manualReceiptMonthlyLimitFree } returns manualReceiptMonthlyLimitFree
        every { appConfigRepository.proAllowlistEmailHashes } returns proAllowlistEmailHashes
        every {
            userPreferenceRepository.getMonthlyManualReceiptAttachmentCount(fixedYearMonth)
        } returns monthlyManualReceiptCount
        every { userRepository.observeCurrentUserProfile() } returns flowOf(null)
        every { authenticationService.currentUserEmail() } returns null

        service = FeatureGateServiceImpl(
            authenticationService = authenticationService,
            appConfigRepository = appConfigRepository,
            userRepository = userRepository,
            groupRepository = groupRepository,
            userPreferenceRepository = userPreferenceRepository,
            yearMonthProvider = { fixedYearMonth }
        )
    }

    @Test
    fun `observeManualReceiptQuota tracks free limit and becomes blocked when reached`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )
        monthlyManualReceiptCount.value = 2

        val underLimit = service.observeManualReceiptQuota().first()
        assertTrue(underLimit.canAttach)
        assertEquals(3, underLimit.remainingAttachments)
        assertFalse(underLimit.requiresUpgrade)

        monthlyManualReceiptCount.value = 5
        val atLimit = service.observeManualReceiptQuota().first()
        assertFalse(atLimit.canAttach)
        assertEquals(0, atLimit.remainingAttachments)
        assertTrue(atLimit.requiresUpgrade)
    }

    @Test
    fun `observeManualReceiptQuota is unlimited for Pro and when gating disabled`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
        )
        monthlyManualReceiptCount.value = 50

        val proQuota = service.observeManualReceiptQuota().first()
        assertTrue(proQuota.canAttach)
        assertEquals(null, proQuota.remainingAttachments)
        assertFalse(proQuota.requiresUpgrade)

        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )
        subscriptionGatingEnabled.value = false
        val ungatedQuota = service.observeManualReceiptQuota().first()
        assertTrue(ungatedQuota.canAttach)
        assertEquals(null, ungatedQuota.remainingAttachments)
        assertFalse(ungatedQuota.requiresUpgrade)
    }

    @Test
    fun `checkLimit MANUAL_RECEIPT_MONTHLY_ATTACHMENTS enforces free limit and allows pro or ungated`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )

        assertEquals(
            LimitResult.Allowed,
            service.checkLimit(GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS, 4).first()
        )
        assertEquals(
            LimitResult.Blocked(GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS, upgradeRequired = true),
            service.checkLimit(GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS, 5).first()
        )

        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
        )
        assertEquals(
            LimitResult.Allowed,
            service.checkLimit(GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS, Int.MAX_VALUE).first()
        )

        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )
        subscriptionGatingEnabled.value = false
        assertEquals(
            LimitResult.Allowed,
            service.checkLimit(GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS, 10).first()
        )
    }
}
