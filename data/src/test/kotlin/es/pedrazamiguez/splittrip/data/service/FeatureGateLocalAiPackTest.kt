package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
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

class FeatureGateLocalAiPackTest {

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
        appConfigRepository = mockk()
        userRepository = mockk()
        groupRepository = mockk()
        userPreferenceRepository = mockk()

        every { appConfigRepository.subscriptionGatingEnabled } returns subscriptionGatingEnabled
        every { appConfigRepository.proAllowlistEmailHashes } returns proAllowlistEmailHashes
        every { userRepository.observeCurrentUserProfile() } returns flowOf(null)
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
    fun `isFeatureEnabled for LOCAL_AI_MODEL_PACK returns true for Pro user`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "pro-user", email = "pro@test.com", tier = SubscriptionTier.PRO)
        )

        assertTrue(service.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK).first())
    }

    @Test
    fun `isFeatureEnabled for LOCAL_AI_MODEL_PACK returns false for Free user when gating enabled`() = runTest {
        coEvery { authenticationService.isAnonymous() } returns false
        every { userRepository.observeCurrentUserProfile() } returns flowOf(
            User(userId = "free-user", email = "free@test.com", tier = SubscriptionTier.FREE)
        )

        assertFalse(service.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK).first())
    }

    @Test
    fun `isFeatureEnabled for LOCAL_AI_MODEL_PACK returns true when gating disabled`() = runTest {
        subscriptionGatingEnabled.value = false
        coEvery { authenticationService.isAnonymous() } returns true

        assertTrue(service.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK).first())
    }
}
