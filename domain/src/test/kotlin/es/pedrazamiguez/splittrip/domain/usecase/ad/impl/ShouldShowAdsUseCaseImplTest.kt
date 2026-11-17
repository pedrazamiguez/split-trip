package es.pedrazamiguez.splittrip.domain.usecase.ad.impl

import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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

class ShouldShowAdsUseCaseImplTest {

    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var featureGateService: FeatureGateService
    private lateinit var useCase: ShouldShowAdsUseCaseImpl

    private val adsEnabledFlow = MutableStateFlow(true)
    private val isInitialFetchSettledFlow = MutableStateFlow(true)

    @BeforeEach
    fun setUp() {
        adsEnabledFlow.value = true
        isInitialFetchSettledFlow.value = true
        appConfigRepository = mockk {
            every { adsEnabled } returns adsEnabledFlow
            every { isInitialFetchSettled } returns isInitialFetchSettledFlow
        }
        featureGateService = mockk()
        useCase = ShouldShowAdsUseCaseImpl(appConfigRepository, featureGateService)
    }

    @Test
    fun `invoke returns true when adsEnabled is true and user is FREE`() = runTest {
        adsEnabledFlow.value = true
        every { featureGateService.isFeatureEnabled(GatedFeature.AD_FREE) } returns flowOf(false)

        val result = useCase().first()

        assertTrue(result)
    }

    @Test
    fun `invoke returns false when adsEnabled is false even if user is FREE`() = runTest {
        adsEnabledFlow.value = false
        every { featureGateService.isFeatureEnabled(GatedFeature.AD_FREE) } returns flowOf(false)

        val result = useCase().first()

        assertFalse(result)
    }

    @Test
    fun `invoke returns false when user is PRO even if adsEnabled is true`() = runTest {
        adsEnabledFlow.value = true
        every { featureGateService.isFeatureEnabled(GatedFeature.AD_FREE) } returns flowOf(true)

        val result = useCase().first()

        assertFalse(result)
    }

    @Test
    fun `invoke returns false when adsEnabled is false and user is PRO`() = runTest {
        adsEnabledFlow.value = false
        every { featureGateService.isFeatureEnabled(GatedFeature.AD_FREE) } returns flowOf(true)

        val result = useCase().first()

        assertFalse(result)
    }

    @Test
    fun `invoke transitions from false to true when AD_FREE flips to false on PRO to FREE downgrade`() =
        runTest {
            val isAdFreeFlow = MutableStateFlow(true)
            adsEnabledFlow.value = true
            every { featureGateService.isFeatureEnabled(GatedFeature.AD_FREE) } returns isAdFreeFlow

            val emissions = mutableListOf<Boolean>()
            val collectJob = launch {
                useCase().take(2).toList(emissions)
            }
            testScheduler.runCurrent()

            isAdFreeFlow.value = false
            collectJob.join()

            assertEquals(listOf(false, true), emissions)
        }

    @Test
    fun `invoke returns false before initial fetch settles even when adsEnabled is true for FREE user`() =
        runTest {
            isInitialFetchSettledFlow.value = false
            adsEnabledFlow.value = true
            every { featureGateService.isFeatureEnabled(GatedFeature.AD_FREE) } returns flowOf(false)

            val result = useCase().first()

            assertFalse(result)
        }

    @Test
    fun `invoke stays false when initial fetch settles with adsEnabled false for FREE user`() = runTest {
        isInitialFetchSettledFlow.value = false
        adsEnabledFlow.value = true
        every { featureGateService.isFeatureEnabled(GatedFeature.AD_FREE) } returns flowOf(false)

        val emissions = mutableListOf<Boolean>()
        val collectJob = launch {
            useCase().distinctUntilChanged().take(1).toList(emissions)
        }
        testScheduler.runCurrent()

        adsEnabledFlow.value = false
        isInitialFetchSettledFlow.value = true
        testScheduler.runCurrent()
        collectJob.join()

        assertEquals(listOf(false), emissions)
        assertFalse(useCase().first())
    }

    @Test
    fun `invoke transitions from false to true when initial fetch settles with adsEnabled true for FREE user`() =
        runTest {
            isInitialFetchSettledFlow.value = false
            adsEnabledFlow.value = true
            every { featureGateService.isFeatureEnabled(GatedFeature.AD_FREE) } returns flowOf(false)

            val emissions = mutableListOf<Boolean>()
            val collectJob = launch {
                useCase().take(2).toList(emissions)
            }
            testScheduler.runCurrent()

            isInitialFetchSettledFlow.value = true
            collectJob.join()

            assertEquals(listOf(false, true), emissions)
        }
}
