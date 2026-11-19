package es.pedrazamiguez.splittrip.domain.usecase.update.impl

import es.pedrazamiguez.splittrip.domain.model.AppUpdateDecision
import es.pedrazamiguez.splittrip.domain.model.AppUpdatePolicy
import es.pedrazamiguez.splittrip.domain.model.OptionalUpdateDismissal
import es.pedrazamiguez.splittrip.domain.model.SemanticVersion
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AppUpdatePolicyService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ResolveAppUpdateDecisionUseCaseImplTest {

    private lateinit var appConfigService: AppConfigService
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var appUpdatePolicyService: AppUpdatePolicyService

    @BeforeEach
    fun setUp() {
        appConfigService = mockk()
        userPreferenceRepository = mockk()
        appUpdatePolicyService = mockk()

        every { appConfigService.inAppUpdatesEnabled } returns MutableStateFlow(true)
        every { appConfigService.minSupportedVersionCode } returns MutableStateFlow(999_000L)
        every { appConfigService.autoForcePatchUpdates } returns MutableStateFlow(true)
        every { appConfigService.optionalUpdateVersionCodes } returns MutableStateFlow(setOf(1_002_000L))
        every { appConfigService.optionalUpdateCooldownHours } returns MutableStateFlow(24L)
    }

    @Test
    fun `builds AppUpdatePolicy from AppConfigService flows, loads last dismissal, and delegates to policy service`() =
        runTest {
            val fixedNow = 1_700_000_000_000L
            val dismissal = OptionalUpdateDismissal(
                versionCode = 1_001_000L,
                dismissedAtTimestampMs = fixedNow - 1_000L
            )
            val expectedPolicy = AppUpdatePolicy(
                inAppUpdatesEnabled = true,
                minSupportedVersionCode = 999_000L,
                autoForcePatchUpdates = true,
                optionalUpdateVersionCodes = setOf(1_002_000L),
                optionalUpdateCooldownHours = 24L
            )
            val expectedDecision = AppUpdateDecision.Optional(
                availableVersionCode = 1_002_000L,
                availableVersion = SemanticVersion(1, 2, 0)
            )
            coEvery { userPreferenceRepository.getOptionalUpdateDismissal() } returns dismissal
            every {
                appUpdatePolicyService.resolveDecision(
                    installedVersionCode = 1_000_000L,
                    availableVersionCode = 1_002_000L,
                    policy = expectedPolicy,
                    lastDismissal = dismissal,
                    nowTimestampMs = fixedNow
                )
            } returns expectedDecision

            val useCase = ResolveAppUpdateDecisionUseCaseImpl(
                appConfigService = appConfigService,
                userPreferenceRepository = userPreferenceRepository,
                appUpdatePolicyService = appUpdatePolicyService,
                currentTimeMillis = { fixedNow }
            )

            val result = useCase(
                installedVersionCode = 1_000_000L,
                availableVersionCode = 1_002_000L
            )

            assertEquals(expectedDecision, result)
            verify(exactly = 1) {
                appUpdatePolicyService.resolveDecision(
                    installedVersionCode = 1_000_000L,
                    availableVersionCode = 1_002_000L,
                    policy = expectedPolicy,
                    lastDismissal = dismissal,
                    nowTimestampMs = fixedNow
                )
            }
        }

    @Test
    fun `passes null dismissal when user has never dismissed an optional update`() = runTest {
        coEvery { userPreferenceRepository.getOptionalUpdateDismissal() } returns null
        every {
            appUpdatePolicyService.resolveDecision(
                installedVersionCode = 1_000_000L,
                availableVersionCode = 1_001_000L,
                policy = any(),
                lastDismissal = null,
                nowTimestampMs = any()
            )
        } returns AppUpdateDecision.Optional(1_001_000L, SemanticVersion(1, 1, 0))

        val useCase = ResolveAppUpdateDecisionUseCaseImpl(
            appConfigService = appConfigService,
            userPreferenceRepository = userPreferenceRepository,
            appUpdatePolicyService = appUpdatePolicyService
        )

        val result = useCase(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_001_000L
        )

        assertEquals(AppUpdateDecision.Optional(1_001_000L, SemanticVersion(1, 1, 0)), result)
        verify(exactly = 1) {
            appUpdatePolicyService.resolveDecision(
                installedVersionCode = 1_000_000L,
                availableVersionCode = 1_001_000L,
                policy = any(),
                lastDismissal = null,
                nowTimestampMs = any()
            )
        }
    }
}
