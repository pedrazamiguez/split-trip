package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeatureGateServicePlanAllowancesTest {

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
    private val aiReceiptMonthlyLimitPro = MutableStateFlow(100)
    private val manualReceiptMonthlyLimitFree = MutableStateFlow(5)
    private val reportExportMonthlyLimitFree = MutableStateFlow(1)
    private val maxSubunitsPerGroupFree = MutableStateFlow(1)
    private val maxSubunitsPerGroupPro = MutableStateFlow(-1)
    private val monthlyScanCount = MutableStateFlow(0)
    private val monthlyManualReceiptCount = MutableStateFlow(0)
    private val monthlyReportExportCount = MutableStateFlow(0)
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
        every { appConfigRepository.reportExportMonthlyLimitFree } returns reportExportMonthlyLimitFree
        every { appConfigRepository.maxSubunitsPerGroupFree } returns maxSubunitsPerGroupFree
        every { appConfigRepository.maxSubunitsPerGroupPro } returns maxSubunitsPerGroupPro
        every { appConfigRepository.proAllowlistEmailHashes } returns proAllowlistEmailHashes
        every { userPreferenceRepository.getMonthlyAiReceiptScanCount(fixedYearMonth) } returns monthlyScanCount
        every {
            userPreferenceRepository.getMonthlyManualReceiptAttachmentCount(fixedYearMonth)
        } returns monthlyManualReceiptCount
        every {
            userPreferenceRepository.getMonthlyReportExportCount(fixedYearMonth)
        } returns monthlyReportExportCount
        every { userRepository.observeCurrentUserProfile() } returns flowOf(null)
        every { groupRepository.getAllGroupsFlow() } returns flowOf(emptyList())
        every { authenticationService.currentUserId() } returns "user-1"
        every { authenticationService.currentUserEmail() } returns null

        subscriptionGatingEnabled.value = true
        maxOwnedGroupsFree.value = 1
        maxOwnedGroupsPro.value = -1
        maxMembersPerGroupFree.value = 4
        maxMembersPerGroupPro.value = 20
        aiReceiptMonthlyLimitFree.value = 3
        aiReceiptMonthlyLimitPro.value = 100
        manualReceiptMonthlyLimitFree.value = 5
        reportExportMonthlyLimitFree.value = 1
        maxSubunitsPerGroupFree.value = 1
        maxSubunitsPerGroupPro.value = -1
        monthlyScanCount.value = 0
        monthlyManualReceiptCount.value = 0
        monthlyReportExportCount.value = 0
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
    fun `observePlanAllowances returns Free tier quotas, 1 subunit allowance, and locked ad-free perk`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { authenticationService.currentUserId() } returns "user-1"
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "user-1", email = "free@test.com", tier = SubscriptionTier.FREE)
            )
            every { groupRepository.getAllGroupsFlow() } returns flowOf(
                listOf(
                    Group(id = "g-1", createdBy = "user-1"),
                    Group(id = "g-2", createdBy = "other-user")
                )
            )
            monthlyScanCount.value = 1
            monthlyManualReceiptCount.value = 2

            val allowances = service.observePlanAllowances().first()

            assertEquals(SubscriptionTier.FREE, allowances.tier)
            assertTrue(allowances.isGatingEnabled)
            assertEquals(1, allowances.ownedGroupsUsed)
            assertEquals(1, allowances.maxOwnedGroups)
            assertEquals(4, allowances.maxMembersPerGroup)
            assertEquals(1, allowances.maxSubunitsPerGroup)
            assertEquals(1, allowances.aiReceiptQuota.usedScans)
            assertEquals(3, allowances.aiReceiptQuota.monthlyLimit)
            assertEquals(2, allowances.mediaUploadQuota.usedUploads)
            assertEquals(5, allowances.mediaUploadQuota.monthlyLimit)
            assertEquals(0, allowances.reportExportQuota.usedExports)
            assertEquals(1, allowances.reportExportQuota.monthlyLimit)
            assertFalse(allowances.isAdFreeEnabled)
            assertTrue(allowances.showUpgradeCta)
        }

    @Test
    fun `observePlanAllowances returns Pro tier quotas, unlimited media and subunits, and ad-free perk`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { authenticationService.currentUserId() } returns "pro-user"
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
            )
            every { groupRepository.getAllGroupsFlow() } returns flowOf(
                listOf(
                    Group(id = "g-1", createdBy = "pro-user"),
                    Group(id = "g-2", createdBy = "pro-user")
                )
            )
            monthlyScanCount.value = 12
            monthlyManualReceiptCount.value = 18

            val allowances = service.observePlanAllowances().first()

            assertEquals(SubscriptionTier.PRO, allowances.tier)
            assertTrue(allowances.isProTier)
            assertEquals(2, allowances.ownedGroupsUsed)
            assertEquals(null, allowances.maxOwnedGroups)
            assertEquals(null, allowances.ownedGroupsRemaining)
            assertEquals(20, allowances.maxMembersPerGroup)
            assertEquals(null, allowances.maxSubunitsPerGroup)
            assertEquals(100, allowances.aiReceiptQuota.monthlyLimit)
            assertTrue(allowances.mediaUploadQuota.isUnlimited)
            assertEquals(null, allowances.mediaUploadQuota.monthlyLimit)
            assertTrue(allowances.reportExportQuota.isUnlimited)
            assertEquals(null, allowances.reportExportQuota.monthlyLimit)
            assertTrue(allowances.isAdFreeEnabled)
            assertFalse(allowances.showUpgradeCta)
        }

    @Test
    fun `observePlanAllowances grants Pro allowances to allowlisted non-anonymous Free user`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserId() } returns "vip-user"
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "vip-user", email = "vip@example.com", tier = SubscriptionTier.FREE)
        )
        proAllowlistEmailHashes.value = setOf(User.hashEmail("vip@example.com"))

        val allowances = service.observePlanAllowances().first()

        assertEquals(SubscriptionTier.PRO, allowances.tier)
        assertEquals(null, allowances.maxOwnedGroups)
        assertEquals(null, allowances.ownedGroupsRemaining)
        assertEquals(20, allowances.maxMembersPerGroup)
        assertEquals(null, allowances.maxSubunitsPerGroup)
        assertTrue(allowances.mediaUploadQuota.isUnlimited)
        assertTrue(allowances.isAdFreeEnabled)
        assertFalse(allowances.showUpgradeCta)
    }

    @Test
    fun `observePlanAllowances handles ungated state when subscriptionGatingEnabled is false`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns true
        subscriptionGatingEnabled.value = false

        val allowances = service.observePlanAllowances().first()

        assertFalse(allowances.isGatingEnabled)
        assertEquals(null, allowances.maxOwnedGroups)
        assertEquals(null, allowances.ownedGroupsRemaining)
        assertEquals(null, allowances.maxSubunitsPerGroup)
        assertTrue(allowances.mediaUploadQuota.isUnlimited)
        assertTrue(allowances.isAdFreeEnabled)
        assertFalse(allowances.showUpgradeCta)
    }

    @Test
    fun `observePlanAllowances exposes positive maxOwnedGroupsPro override when configured`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserId() } returns "pro-user"
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
        )
        every { groupRepository.getAllGroupsFlow() } returns flowOf(
            listOf(
                Group(id = "g-1", createdBy = "pro-user"),
                Group(id = "g-2", createdBy = "pro-user")
            )
        )
        maxOwnedGroupsPro.value = 50

        val allowances = service.observePlanAllowances().first()

        assertEquals(50, allowances.maxOwnedGroups)
        assertEquals(48, allowances.ownedGroupsRemaining)
    }

    @Test
    fun `observePlanAllowances returns zero owned groups when currentUserId is null or blank`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "user-1", email = "free@test.com", tier = SubscriptionTier.FREE)
        )
        every { groupRepository.getAllGroupsFlow() } returns flowOf(
            listOf(Group(id = "g-1", createdBy = "user-1"))
        )

        every { authenticationService.currentUserId() } returns null
        assertEquals(0, service.observePlanAllowances().first().ownedGroupsUsed)

        every { authenticationService.currentUserId() } returns "   "
        assertEquals(0, service.observePlanAllowances().first().ownedGroupsUsed)
    }

    @Test
    fun `observePlanAllowances emits reactively when groups, AI scan count, or media count updates`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { authenticationService.currentUserId() } returns "user-1"
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "user-1", email = "free@test.com", tier = SubscriptionTier.FREE)
            )
            val groupsFlow = MutableStateFlow(emptyList<Group>())
            every { groupRepository.getAllGroupsFlow() } returns groupsFlow

            val emissions = mutableListOf<PlanAllowances>()
            val job = launch {
                service.observePlanAllowances()
                    .take(4)
                    .toList(emissions)
            }
            testScheduler.runCurrent()

            groupsFlow.value = listOf(Group(id = "g-1", createdBy = "user-1"))
            testScheduler.runCurrent()

            monthlyScanCount.value = 2
            testScheduler.runCurrent()

            monthlyManualReceiptCount.value = 3
            testScheduler.runCurrent()
            job.join()

            assertEquals(4, emissions.size)
            assertEquals(0, emissions[0].ownedGroupsUsed)
            assertEquals(1, emissions[1].ownedGroupsUsed)
            assertEquals(2, emissions[2].aiReceiptQuota.usedScans)
            assertEquals(3, emissions[3].mediaUploadQuota.usedUploads)
        }
}
