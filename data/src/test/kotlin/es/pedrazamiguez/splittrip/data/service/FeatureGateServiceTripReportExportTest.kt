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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
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

    private val subscriptionGatingEnabled = MutableStateFlow(true)
    private val proAllowlistEmailHashes = MutableStateFlow<Set<String>>(emptySet())

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        appConfigRepository = mockk(relaxed = true)
        userRepository = mockk()
        groupRepository = mockk()
        userPreferenceRepository = mockk(relaxed = true)

        every { appConfigRepository.subscriptionGatingEnabled } returns subscriptionGatingEnabled
        every { appConfigRepository.proAllowlistEmailHashes } returns proAllowlistEmailHashes
        every { authenticationService.currentUserEmail() } returns null

        service = FeatureGateServiceImpl(
            authenticationService = authenticationService,
            appConfigRepository = appConfigRepository,
            userRepository = userRepository,
            groupRepository = groupRepository,
            userPreferenceRepository = userPreferenceRepository,
            yearMonthProvider = { YearMonth.of(2026, 11) }
        )
    }

    @Test
    fun `isFeatureEnabled TRIP_REPORT_EXPORT returns true for acting PRO user even in Free group`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "acting_pro", email = "pro@test.com", tier = SubscriptionTier.PRO)
            )
            coEvery { groupRepository.getGroupById("group_free") } returns Group(
                id = "group_free",
                createdBy = "creator_free"
            )
            coEvery { userRepository.getUsersByIds(listOf("creator_free")) } returns mapOf(
                "creator_free" to User(userId = "creator_free", email = "free@test.com", tier = SubscriptionTier.FREE)
            )

            assertTrue(service.isFeatureEnabled(GatedFeature.TRIP_REPORT_EXPORT, "group_free").first())
        }

    @Test
    fun `isFeatureEnabled TRIP_REPORT_EXPORT returns true for acting FREE user when group creator is PRO`() =
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

            assertTrue(service.isFeatureEnabled(GatedFeature.TRIP_REPORT_EXPORT, "group_pro").first())
        }

    @Test
    fun `isFeatureEnabled TRIP_REPORT_EXPORT returns false for acting FREE user when group creator is FREE`() =
        runTest {
            coEvery { authenticationService.isAnonymous() } returns false
            every { userRepository.observeCurrentUserProfile() } returns flowOf(
                User(userId = "acting_free", email = "free@test.com", tier = SubscriptionTier.FREE)
            )
            coEvery { groupRepository.getGroupById("group_free") } returns Group(
                id = "group_free",
                createdBy = "creator_free"
            )
            coEvery { userRepository.getUsersByIds(listOf("creator_free")) } returns mapOf(
                "creator_free" to User(
                    userId = "creator_free",
                    email = "creator@test.com",
                    tier = SubscriptionTier.FREE
                )
            )

            assertFalse(service.isFeatureEnabled(GatedFeature.TRIP_REPORT_EXPORT, "group_free").first())
        }
}
