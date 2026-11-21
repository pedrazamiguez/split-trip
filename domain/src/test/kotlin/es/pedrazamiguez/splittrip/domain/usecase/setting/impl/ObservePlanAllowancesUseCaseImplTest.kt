package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.service.featuregate.AiReceiptQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.MediaUploadQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ObservePlanAllowancesUseCaseImplTest {

    private lateinit var featureGateService: FeatureGateService
    private lateinit var useCase: ObservePlanAllowancesUseCaseImpl

    @BeforeEach
    fun setUp() {
        featureGateService = mockk()
        useCase = ObservePlanAllowancesUseCaseImpl(featureGateService = featureGateService)
    }

    @Test
    fun `invoke delegates directly to featureGateService observePlanAllowances`() = runTest {
        val expected = PlanAllowances(
            tier = SubscriptionTier.FREE,
            isGatingEnabled = true,
            aiReceiptQuota = AiReceiptQuota(
                usedScans = 1,
                monthlyLimit = 3,
                isProTier = false,
                isGatingEnabled = true
            ),
            mediaUploadQuota = MediaUploadQuota(
                usedUploads = 2,
                monthlyFreeLimit = 5,
                isProTier = false,
                isGatingEnabled = true
            ),
            ownedGroupsUsed = 1,
            maxOwnedGroups = 1,
            maxMembersPerGroup = 4,
            maxSubunitsPerGroup = 1,
            isAdFreeEnabled = false
        )
        every { featureGateService.observePlanAllowances() } returns flowOf(expected)

        val actual = useCase().first()

        assertEquals(expected, actual)
        verify(exactly = 1) { featureGateService.observePlanAllowances() }
    }
}
