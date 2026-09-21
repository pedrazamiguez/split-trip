package es.pedrazamiguez.splittrip.core.designsystem.ad

import android.app.Activity
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.usecase.ad.ShouldShowAdsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InterstitialAdManagerTest {

    private lateinit var testDispatcher: TestDispatcher

    private lateinit var appConfigRepository: AppConfigRepository

    private val shouldShowAdsFlow = MutableStateFlow(true)
    private val shouldShowAdsUseCase = object : ShouldShowAdsUseCase {
        override fun invoke(): Flow<Boolean> = shouldShowAdsFlow
    }
    private val interstitialAdUnitIdFlow = MutableStateFlow("test-interstitial-id")
    private val frequencyFlow = MutableStateFlow(5)
    private val minIntervalFlow = MutableStateFlow(180L)
    private var currentTime = 1000000L

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        shouldShowAdsFlow.value = true
        appConfigRepository = mockk {
            every { admobInterstitialAdUnitId } returns interstitialAdUnitIdFlow
            every { adInterstitialActionFrequency } returns frequencyFlow
            every { adInterstitialMinIntervalSeconds } returns minIntervalFlow
        }
        currentTime = 1000000L
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createManager(scope: CoroutineScope): InterstitialAdManager {
        return InterstitialAdManager(
            appConfigRepository = appConfigRepository,
            shouldShowAdsUseCase = shouldShowAdsUseCase,
            coroutineScope = scope,
            timeProvider = { currentTime }
        )
    }

    @Test
    fun `onActionCompleted executes onComplete immediately when ads are disabled`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        manager.setAdEnabled(false)

        val mockActivity = mockk<Activity>()
        val mockAd = mockk<InterstitialAd>(relaxed = true)
        manager.setCachedAd(mockAd)
        manager.setActionCounter(4) // will increment to 5

        var completed = false
        manager.onActionCompleted(mockActivity) { completed = true }

        assertTrue(completed)
        verify(exactly = 0) { mockAd.show(any()) }
    }

    @Test
    fun `onActionCompleted executes onComplete immediately when no ad is preloaded`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        shouldShowAdsFlow.value = true
        advanceUntilIdle()

        val mockActivity = mockk<Activity>()
        manager.setCachedAd(null)
        manager.setActionCounter(4)

        var completed = false
        manager.onActionCompleted(mockActivity) { completed = true }

        assertTrue(completed)
    }

    @Test
    fun `onActionCompleted respects action frequency cap before attempting to show ad`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        shouldShowAdsFlow.value = true
        advanceUntilIdle()

        val mockActivity = mockk<Activity>()
        val mockAd = mockk<InterstitialAd>(relaxed = true)
        manager.setCachedAd(mockAd)
        manager.setActionCounter(2) // will increment to 3, threshold is 5

        var completed = false
        manager.onActionCompleted(mockActivity) { completed = true }

        assertTrue(completed)
        verify(exactly = 0) { mockAd.show(any()) }
        assertEquals(3, manager.getActionCounter())
    }

    @Test
    fun `onActionCompleted respects min interval seconds cooldown before attempting to show ad`() =
        runTest(testDispatcher) {
            val manager = createManager(backgroundScope)
            shouldShowAdsFlow.value = true
            advanceUntilIdle()

            val mockActivity = mockk<Activity>()
            val mockAd = mockk<InterstitialAd>(relaxed = true)
            manager.setCachedAd(mockAd)
            manager.setActionCounter(4) // will increment to 5
            manager.setLastShownTimestamp(currentTime - 60_000L) // Only 60s elapsed, needs 180s

            var completed = false
            manager.onActionCompleted(mockActivity) { completed = true }

            assertTrue(completed)
            verify(exactly = 0) { mockAd.show(any()) }
            assertEquals(5, manager.getActionCounter())
        }

    @Test
    fun `onActionCompleted shows ad and calls onComplete upon dismissal`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        manager.setAdEnabled(true)

        val mockActivity = mockk<Activity>()
        val mockAd = mockk<InterstitialAd>(relaxed = true)
        val callbackSlot = slot<FullScreenContentCallback>()
        every { mockAd.fullScreenContentCallback = capture(callbackSlot) } answers { }

        manager.setCachedAd(mockAd)
        manager.setActionCounter(4) // will increment to 5
        manager.setLastShownTimestamp(currentTime - 200_000L) // 200s elapsed >= 180s

        var completed = false
        manager.onActionCompleted(mockActivity) { completed = true }

        verify(exactly = 1) { mockAd.show(mockActivity) }
        assertEquals(0, manager.getActionCounter())

        // Simulate ad dismissal
        callbackSlot.captured.onAdDismissedFullScreenContent()
        assertTrue(completed)
        assertNull(manager.getCachedAd())
    }
}
