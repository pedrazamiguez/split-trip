package es.pedrazamiguez.splittrip.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("KeyedSubscriptionTracker")
class KeyedSubscriptionTrackerTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var tracker: KeyedSubscriptionTracker

    @BeforeEach
    fun setUp() {
        tracker = KeyedSubscriptionTracker(defaultGracePeriodMs = 5_000L)
    }

    @Nested
    @DisplayName("Reference counting & acquisition")
    inner class AcquireAndRelease {

        @Test
        fun `single collector launches job and sets refCount to 1`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var launchCount = 0

            tracker.acquire("key-1", scope) {
                launchCount++
                awaitCancellation()
            }
            advanceTimeBy(100)

            assertTrue(tracker.isSubscribed("key-1"))
            assertEquals(1, tracker.getSubscriberCount("key-1"))
            assertEquals(1, launchCount)
            tracker.cancelAll()
        }

        @Test
        fun `multiple concurrent collectors launch exactly one cloud job`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var launchCount = 0

            tracker.acquire("key-1", scope) {
                launchCount++
                awaitCancellation()
            }
            tracker.acquire("key-1", scope) {
                launchCount++
                awaitCancellation()
            }
            tracker.acquire("key-1", scope) {
                launchCount++
                awaitCancellation()
            }
            advanceTimeBy(100)

            assertEquals(1, launchCount, "Should launch the job exactly once")
            assertEquals(3, tracker.getSubscriberCount("key-1"))
            assertTrue(tracker.isSubscribed("key-1"))
            tracker.cancelAll()
        }

        @Test
        fun `collector completion retains subscription during grace period`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var jobCancelled = false

            tracker.acquire("key-1", scope) {
                try {
                    awaitCancellation()
                } finally {
                    jobCancelled = true
                }
            }
            advanceTimeBy(100)

            // Release the only subscriber
            tracker.release("key-1", scope, gracePeriodMs = 5_000L)

            // Refcount is 0, but during grace period (before 5,000ms), job is still active
            assertEquals(0, tracker.getSubscriberCount("key-1"))
            assertTrue(tracker.isSubscribed("key-1"))
            assertFalse(jobCancelled)

            advanceTimeBy(3_000L)
            assertTrue(tracker.isSubscribed("key-1"))
            assertFalse(jobCancelled)
            tracker.cancelAll()
        }

        @Test
        fun `re-subscribing within grace period cancels pending termination`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var launchCount = 0

            tracker.acquire("key-1", scope) {
                launchCount++
                awaitCancellation()
            }
            advanceTimeBy(100)

            // Release subscriber
            tracker.release("key-1", scope, gracePeriodMs = 5_000L)
            advanceTimeBy(3_000L)

            // Re-acquire before grace period ends
            tracker.acquire("key-1", scope) {
                launchCount++
            }
            advanceTimeBy(3_000L)

            // Job should still be the original one without re-launching
            assertEquals(1, launchCount)
            assertEquals(1, tracker.getSubscriberCount("key-1"))
            assertTrue(tracker.isSubscribed("key-1"))
            tracker.cancelAll()
        }

        @Test
        fun `expiry of grace period terminates subscription and evicts key`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var jobCancelled = false

            tracker.acquire("key-1", scope) {
                try {
                    awaitCancellation()
                } finally {
                    jobCancelled = true
                }
            }
            advanceTimeBy(100)

            tracker.release("key-1", scope, gracePeriodMs = 5_000L)
            advanceTimeBy(5_001L)

            assertFalse(tracker.isSubscribed("key-1"))
            assertEquals(0, tracker.getSubscriberCount("key-1"))
            assertTrue(jobCancelled)
        }

        @Test
        fun `release with 0 grace period terminates immediately`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var jobCancelled = false

            tracker.acquire("key-1", scope) {
                try {
                    awaitCancellation()
                } finally {
                    jobCancelled = true
                }
            }
            advanceTimeBy(100)

            tracker.release("key-1", scope, gracePeriodMs = 0L)
            advanceUntilIdle()

            assertFalse(tracker.isSubscribed("key-1"))
            assertTrue(jobCancelled)
        }

        @Test
        fun `cancelAll cancels all active jobs and grace period timers`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var job1Cancelled = false
            var job2Cancelled = false

            tracker.acquire("key-1", scope) {
                try {
                    awaitCancellation()
                } finally {
                    job1Cancelled = true
                }
            }
            tracker.acquire("key-2", scope) {
                try {
                    awaitCancellation()
                } finally {
                    job2Cancelled = true
                }
            }
            advanceTimeBy(100)

            tracker.cancelAll()
            advanceUntilIdle()

            assertFalse(tracker.isSubscribed("key-1"))
            assertFalse(tracker.isSubscribed("key-2"))
            assertTrue(job1Cancelled)
            assertTrue(job2Cancelled)
        }
    }

    @Nested
    @DisplayName("trackSubscription extension")
    inner class TrackSubscriptionExtension {

        @Test
        fun `trackSubscription acquires on collection and releases on completion`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var blockExecutionCount = 0

            val flow = flowOf("item1", "item2")
                .trackSubscription(tracker, "group-1", scope, gracePeriodMs = 1_000L) {
                    blockExecutionCount++
                    awaitCancellation()
                }

            val job1 = launch { flow.collect {} }
            val job2 = launch { flow.collect {} }
            advanceTimeBy(100)

            assertEquals(1, blockExecutionCount)
            // Both collectors finished flowOf immediately, so refCount is now 0 and grace period is running
            assertTrue(tracker.isSubscribed("group-1"))

            advanceTimeBy(1_001L)
            assertFalse(tracker.isSubscribed("group-1"))
            job1.cancel()
            job2.cancel()
        }
    }
}
