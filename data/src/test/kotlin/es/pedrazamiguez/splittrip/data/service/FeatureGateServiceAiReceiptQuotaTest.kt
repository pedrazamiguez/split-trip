package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
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

class FeatureGateServiceAiReceiptQuotaTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var userRepository: UserRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var service: FeatureGateServiceImpl

    private val fixedYearMonth = YearMonth.of(2026, 11)
    private val subscriptionGatingEnabled = MutableStateFlow(true)
    private val maxOwnedGroupsFree = MutableStateFlow(1)
    private val maxOwnedGroupsPro = MutableStateFlow(-1)
    private val maxMembersPerGroupFree = MutableStateFlow(4)
    private val maxMembersPerGroupPro = MutableStateFlow(20)
    private val aiReceiptMonthlyLimitFree = MutableStateFlow(3)
    private val aiReceiptMonthlyLimitPro = MutableStateFlow(-1)
    private val manualReceiptMonthlyLimitFree = MutableStateFlow(5)
    private val maxSubunitsPerGroupFree = MutableStateFlow(1)
    private val maxSubunitsPerGroupPro = MutableStateFlow(-1)
    private val monthlyScanCount = MutableStateFlow(0)
    private val monthlyManualReceiptCount = MutableStateFlow(0)
    private val proAllowlistEmailHashes = MutableStateFlow<Set<String>>(emptySet())

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        appConfigRepository = mockk()
        userRepository = mockk()
        groupRepository = mockk()
        userPreferenceRepository = mockk()

        every { appConfigRepository.subscriptionGatingEnabled } returns subscriptionGatingEnabled
        every { appConfigRepository.maxOwnedGroupsFree } returns maxOwnedGroupsFree
        every { appConfigRepository.maxOwnedGroupsPro } returns maxOwnedGroupsPro
        every { appConfigRepository.maxMembersPerGroupFree } returns maxMembersPerGroupFree
        every { appConfigRepository.maxMembersPerGroupPro } returns maxMembersPerGroupPro
        every { appConfigRepository.aiReceiptMonthlyLimitFree } returns aiReceiptMonthlyLimitFree
        every { appConfigRepository.aiReceiptMonthlyLimitPro } returns aiReceiptMonthlyLimitPro
        every { appConfigRepository.manualReceiptMonthlyLimitFree } returns manualReceiptMonthlyLimitFree
        every { appConfigRepository.maxSubunitsPerGroupFree } returns maxSubunitsPerGroupFree
        every { appConfigRepository.maxSubunitsPerGroupPro } returns maxSubunitsPerGroupPro
        every { appConfigRepository.proAllowlistEmailHashes } returns proAllowlistEmailHashes
        every { userPreferenceRepository.getMonthlyAiReceiptScanCount(fixedYearMonth) } returns monthlyScanCount
        every {
            userPreferenceRepository.getMonthlyManualReceiptAttachmentCount(fixedYearMonth)
        } returns monthlyManualReceiptCount
        every { userRepository.observeCurrentUserProfile() } returns flowOf(null)
        every { authenticationService.currentUserEmail() } returns null

        subscriptionGatingEnabled.value = true
        aiReceiptMonthlyLimitFree.value = 3
        aiReceiptMonthlyLimitPro.value = -1
        monthlyScanCount.value = 0
        proAllowlistEmailHashes.value = emptySet()

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
    fun `observeAiReceiptQuota emits unlimited Pro quota when aiReceiptMonthlyLimitPro is -1`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
        )
        aiReceiptMonthlyLimitPro.value = -1
        monthlyScanCount.value = 250

        val quota = service.observeAiReceiptQuota().first()

        assertTrue(quota.isUnlimited)
        assertEquals(null, quota.monthlyLimit)
        assertEquals(null, quota.remainingScans)
        assertTrue(quota.canScan)
        assertFalse(quota.requiresUpgrade)
    }

    @Test
    fun `isFeatureEnabled AI_RECEIPT_SCANNING returns true for Pro user when aiReceiptMonthlyLimitPro is -1`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
            )
            aiReceiptMonthlyLimitPro.value = -1
            monthlyScanCount.value = 500

            assertTrue(service.isFeatureEnabled(GatedFeature.AI_RECEIPT_SCANNING).first())
        }

    @Test
    fun `checkLimit for AI_RECEIPT_MONTHLY_SCANS allows any count for Pro user when aiReceiptMonthlyLimitPro is -1`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
            )
            aiReceiptMonthlyLimitPro.value = -1

            assertEquals(LimitResult.Allowed, service.checkLimit(GatedLimit.AI_RECEIPT_MONTHLY_SCANS, 500).first())
        }
}
