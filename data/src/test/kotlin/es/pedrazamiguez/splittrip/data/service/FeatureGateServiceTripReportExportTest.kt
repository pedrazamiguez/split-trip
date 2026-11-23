package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.Group
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeatureGateServiceTripReportExportTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var userRepository: UserRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var service: FeatureGateServiceImpl

    private val fixedMonth = YearMonth.of(2026, 11)
    private val subscriptionGatingEnabled = MutableStateFlow(true)
    private val reportExportMonthlyLimitFree = MutableStateFlow(1)
    private val monthlyReportExportCount = MutableStateFlow(0)
    private val proAllowlistEmailHashes = MutableStateFlow<Set<String>>(emptySet())

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        appConfigRepository = mockk(relaxed = true)
        userRepository = mockk()
        groupRepository = mockk()
        userPreferenceRepository = mockk(relaxed = true)

        every { appConfigRepository.subscriptionGatingEnabled } returns subscriptionGatingEnabled
        every { appConfigRepository.reportExportMonthlyLimitFree } returns reportExportMonthlyLimitFree
        every { appConfigRepository.proAllowlistEmailHashes } returns proAllowlistEmailHashes
        every {
            userPreferenceRepository.getMonthlyReportExportCount(fixedMonth)
        } returns monthlyReportExportCount
        every { authenticationService.currentUserEmail() } returns null

        subscriptionGatingEnabled.value = true
        reportExportMonthlyLimitFree.value = 1
        monthlyReportExportCount.value = 0
        proAllowlistEmailHashes.value = emptySet()

        service = FeatureGateServiceImpl(
            authenticationService = authenticationService,
            appConfigRepository = appConfigRepository,
            userRepository = userRepository,
            groupRepository = groupRepository,
            userPreferenceRepository = userPreferenceRepository,
            yearMonthProvider = { fixedMonth }
        )
    }

    @Test
    fun `observeReportExportQuota grants unlimited PDF CSV and itemised details to acting PRO user`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "acting_pro", email = "pro@test.com", tier = SubscriptionTier.PRO)
        )
        monthlyReportExportCount.value = 5

        val quota = service.observeReportExportQuota().first()

        assertTrue(quota.isUnlimited)
        assertTrue(quota.canExportPdf)
        assertTrue(quota.canExportCsv)
        assertTrue(quota.includesItemisedDetails)
        assertFalse(quota.requiresUpgrade)
        assertNull(quota.monthlyLimit)
        assertNull(quota.remainingExports)
        assertEquals(5, quota.usedExports)
    }

    @Test
    fun `observeReportExportQuota grants 1 free summary PDF per month and locks CSV even in PRO-created group`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "acting_free", email = "free@test.com", tier = SubscriptionTier.FREE)
            )
            coEvery { groupRepository.getGroupById("group_pro") } returns Group(
                id = "group_pro",
                createdBy = "creator_pro"
            )
            coEvery { userRepository.getUsersByIds(listOf("creator_pro")) } returns mapOf(
                "creator_pro" to User(userId = "creator_pro", email = "pro@test.com", tier = SubscriptionTier.PRO)
            )

            val quotaBefore = service.observeReportExportQuota().first()
            assertFalse(quotaBefore.isUnlimited)
            assertTrue(quotaBefore.canExportPdf)
            assertFalse(quotaBefore.canExportCsv)
            assertFalse(quotaBefore.includesItemisedDetails)
            assertEquals(1, quotaBefore.monthlyLimit)
            assertEquals(1, quotaBefore.remainingExports)
            assertTrue(service.isFeatureEnabled(GatedFeature.TRIP_REPORT_EXPORT, "group_pro").first())

            // After using the 1 monthly free PDF export, Free user is blocked even in a Pro-created group
            monthlyReportExportCount.value = 1

            val quotaAfter = service.observeReportExportQuota().first()
            assertFalse(quotaAfter.canExportPdf)
            assertFalse(quotaAfter.canExportCsv)
            assertTrue(quotaAfter.requiresUpgrade)
            assertEquals(0, quotaAfter.remainingExports)
            assertFalse(service.isFeatureEnabled(GatedFeature.TRIP_REPORT_EXPORT, "group_pro").first())
        }

    @Test
    fun `checkLimit REPORT_EXPORT_MONTHLY_COUNT enforces Free limit and allows unlimited for PRO`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "acting_free", email = "free@test.com", tier = SubscriptionTier.FREE)
        )

        assertEquals(
            LimitResult.Allowed,
            service.checkLimit(GatedLimit.REPORT_EXPORT_MONTHLY_COUNT, currentCount = 0).first()
        )
        assertEquals(
            LimitResult.Blocked(GatedLimit.REPORT_EXPORT_MONTHLY_COUNT, upgradeRequired = true),
            service.checkLimit(GatedLimit.REPORT_EXPORT_MONTHLY_COUNT, currentCount = 1).first()
        )

        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "acting_pro", email = "pro@test.com", tier = SubscriptionTier.PRO)
        )
        assertEquals(
            LimitResult.Allowed,
            service.checkLimit(GatedLimit.REPORT_EXPORT_MONTHLY_COUNT, currentCount = 99).first()
        )
    }
}
