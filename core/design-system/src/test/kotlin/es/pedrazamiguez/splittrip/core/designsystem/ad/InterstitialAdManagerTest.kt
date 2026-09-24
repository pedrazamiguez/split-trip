package es.pedrazamiguez.splittrip.core.designsystem.ad

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.usecase.ad.ShouldShowAdsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
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
import org.junit.jupiter.api.Assertions.assertFalse
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
    private val frequencyFlow = MutableStateFlow(3)
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
        manager.setActionCounter(2) // will increment to 3

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
        manager.setActionCounter(2)

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
        manager.setActionCounter(1) // will increment to 2, threshold is 3

        var completed = false
        manager.onActionCompleted(mockActivity) { completed = true }

        assertTrue(completed)
        verify(exactly = 0) { mockAd.show(any()) }
        assertEquals(2, manager.getActionCounter())
    }

    @Test
    fun `onActionCompleted respects min interval seconds cooldown before showing ad`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        shouldShowAdsFlow.value = true
        advanceUntilIdle()

        val mockActivity = mockk<Activity>()
        val mockAd = mockk<InterstitialAd>(relaxed = true)
        manager.setCachedAd(mockAd)
        manager.setActionCounter(2) // will increment to 3
        manager.setLastShownTimestamp(currentTime - 60_000L) // Only 60s elapsed, needs 180s

        var completed = false
        manager.onActionCompleted(mockActivity) { completed = true }

        assertTrue(completed)
        verify(exactly = 0) { mockAd.show(any()) }
        assertEquals(3, manager.getActionCounter())
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
        manager.setActionCounter(2) // will increment to 3
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

    @Test
    fun `onActionCompleted calls onComplete and clears ad when ad fails to show`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        manager.setAdEnabled(true)

        val mockActivity = mockk<Activity>()
        val mockAd = mockk<InterstitialAd>(relaxed = true)
        val callbackSlot = slot<FullScreenContentCallback>()
        every { mockAd.fullScreenContentCallback = capture(callbackSlot) } answers { }

        manager.setCachedAd(mockAd)
        manager.setActionCounter(2) // will increment to 3
        manager.setLastShownTimestamp(currentTime - 200_000L) // 200s elapsed >= 180s

        var completed = false
        manager.onActionCompleted(mockActivity) { completed = true }

        verify(exactly = 1) { mockAd.show(mockActivity) }
        assertEquals(0, manager.getActionCounter())

        // Simulate show failure
        val mockError = mockk<AdError>(relaxed = true)
        callbackSlot.captured.onAdFailedToShowFullScreenContent(mockError)
        assertTrue(completed)
        assertNull(manager.getCachedAd())
    }

    @Test
    fun `shouldShowAdsUseCase emitting false clears cached ad and sets isAdEnabled to false`() =
        runTest(testDispatcher) {
            val manager = createManager(CoroutineScope(testDispatcher))
            advanceUntilIdle()

            val mockAd = mockk<InterstitialAd>(relaxed = true)
            manager.setCachedAd(mockAd)
            assertTrue(manager.isAdEnabled())

            shouldShowAdsFlow.value = false
            advanceUntilIdle()

            assertFalse(manager.isAdEnabled())
            assertNull(manager.getCachedAd())
        }

    @Test
    fun `preloadAd does nothing when ads are disabled`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        manager.setAdEnabled(false)

        val mockContext = mockk<Context>()
        manager.preloadAd(mockContext)

        assertNull(manager.getCachedAd())
    }

    @Test
    fun `preloadAd does nothing when ad is already cached`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        manager.setAdEnabled(true)

        val mockAd = mockk<InterstitialAd>(relaxed = true)
        manager.setCachedAd(mockAd)

        val mockContext = mockk<Context>()
        manager.preloadAd(mockContext)

        assertEquals(mockAd, manager.getCachedAd())
    }

    @Test
    fun `preloadAd does nothing when adUnitId is blank`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        manager.setAdEnabled(true)
        interstitialAdUnitIdFlow.value = ""

        val mockContext = mockk<Context>()
        manager.preloadAd(mockContext)

        assertNull(manager.getCachedAd())
    }

    @Test
    fun `preloadAd loads ad successfully and caches it`() = runTest(testDispatcher) {
        mockkStatic(InterstitialAd::class)
        try {
            val manager = createManager(backgroundScope)
            manager.setAdEnabled(true)

            val mockContext = mockk<Context>()
            val mockAd = mockk<InterstitialAd>(relaxed = true)
            val callbackSlot = slot<InterstitialAdLoadCallback>()

            every {
                InterstitialAd.load(
                    mockContext,
                    "test-interstitial-id",
                    any(),
                    capture(callbackSlot)
                )
            } answers { }

            manager.preloadAd(mockContext)

            callbackSlot.captured.onAdLoaded(mockAd)
            assertEquals(mockAd, manager.getCachedAd())
        } finally {
            unmockkStatic(InterstitialAd::class)
        }
    }

    @Test
    fun `preloadAd handles onAdFailedToLoad properly`() = runTest(testDispatcher) {
        mockkStatic(InterstitialAd::class)
        try {
            val manager = createManager(backgroundScope)
            manager.setAdEnabled(true)

            val mockContext = mockk<Context>()
            val mockError = mockk<LoadAdError>(relaxed = true)
            val callbackSlot = slot<InterstitialAdLoadCallback>()

            every {
                InterstitialAd.load(
                    mockContext,
                    "test-interstitial-id",
                    any(),
                    capture(callbackSlot)
                )
            } answers { }

            manager.preloadAd(mockContext)

            callbackSlot.captured.onAdFailedToLoad(mockError)
            assertNull(manager.getCachedAd())
        } finally {
            unmockkStatic(InterstitialAd::class)
        }
    }

    @Test
    fun `preloadAd handles exception during load gracefully`() = runTest(testDispatcher) {
        mockkStatic(InterstitialAd::class)
        try {
            val manager = createManager(backgroundScope)
            manager.setAdEnabled(true)

            val mockContext = mockk<Context>()
            every {
                InterstitialAd.load(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } throws RuntimeException("Load error")

            manager.preloadAd(mockContext)
            assertNull(manager.getCachedAd())
        } finally {
            unmockkStatic(InterstitialAd::class)
        }
    }
}
