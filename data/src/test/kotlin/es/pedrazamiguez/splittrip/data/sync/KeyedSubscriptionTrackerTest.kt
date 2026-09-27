package es.pedrazamiguez.splittrip.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
        tracker = KeyedSubscriptionTracker()
    }

    @Nested
    @DisplayName("cancelAndRelaunch")
    inner class CancelAndRelaunch {

        @Test
        fun `launches block in the given scope`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var executed = false

            tracker.cancelAndRelaunch("key-1", scope) {
                executed = true
            }
            advanceUntilIdle()

            assertTrue(executed)
        }

        @Test
        fun `cancels previous job for the same key`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var firstJobCompleted = false
            var secondJobCompleted = false

            // Launch a long-running first job
            tracker.cancelAndRelaunch("key-1", scope) {
                try {
                    delay(10_000)
                    firstJobCompleted = true
                } catch (_: Exception) {
                    // Cancelled
                }
            }

            // Immediately relaunch with the same key
            tracker.cancelAndRelaunch("key-1", scope) {
                secondJobCompleted = true
            }
            advanceUntilIdle()

            assertFalse(firstJobCompleted, "First job should have been cancelled")
            assertTrue(secondJobCompleted, "Second job should have completed")
        }

        @Test
        fun `independent keys do not cancel each other`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var job1Completed = false
            var job2Completed = false

            tracker.cancelAndRelaunch("key-A", scope) {
                job1Completed = true
            }

            tracker.cancelAndRelaunch("key-B", scope) {
                job2Completed = true
            }
            advanceUntilIdle()

            assertTrue(job1Completed, "Key-A job should have completed")
            assertTrue(job2Completed, "Key-B job should have completed")
        }

        @Test
        fun `relaunching same key replaces job without affecting other keys`() =
            runTest(testDispatcher) {
                val scope = CoroutineScope(testDispatcher)
                var keyAFirstCompleted = false
                var keyASecondCompleted = false
                var keyBCompleted = false

                tracker.cancelAndRelaunch("key-A", scope) {
                    try {
                        delay(10_000)
                        keyAFirstCompleted = true
                    } catch (_: Exception) {
                        // Cancelled
                    }
                }

                tracker.cancelAndRelaunch("key-B", scope) {
                    keyBCompleted = true
                }

                // Replace key-A's job
                tracker.cancelAndRelaunch("key-A", scope) {
                    keyASecondCompleted = true
                }
                advanceUntilIdle()

                assertFalse(keyAFirstCompleted, "Key-A first job should have been cancelled")
                assertTrue(keyASecondCompleted, "Key-A second job should have completed")
                assertTrue(keyBCompleted, "Key-B job should not have been affected")
            }

        @Test
        fun `can relaunch after previous job completes naturally`() =
            runTest(testDispatcher) {
                val scope = CoroutineScope(testDispatcher)
                var firstCompleted = false
                var secondCompleted = false

                tracker.cancelAndRelaunch("key-1", scope) {
                    firstCompleted = true
                }
                advanceUntilIdle()

                tracker.cancelAndRelaunch("key-1", scope) {
                    secondCompleted = true
                }
                advanceUntilIdle()

                assertTrue(firstCompleted, "First job should have completed")
                assertTrue(secondCompleted, "Second job should have completed")
            }

        @Test
        fun `cancelled job does not prevent new job from running`() =
            runTest(testDispatcher) {
                val scope = CoroutineScope(testDispatcher)
                var completed = false

                // Launch and cancel via relaunch
                tracker.cancelAndRelaunch("key-1", scope) {
                    delay(Long.MAX_VALUE)
                }

                // Cancel by relaunching
                tracker.cancelAndRelaunch("key-1", scope) {
                    completed = true
                }
                advanceUntilIdle()

                assertTrue(completed, "New job should run after cancelling the previous one")
            }
    }

    @Nested
    @DisplayName("cancelAll")
    inner class CancelAll {

        @Test
        fun `cancels all active jobs`() = runTest(testDispatcher) {
            val scope = CoroutineScope(testDispatcher)
            var job1Completed = false
            var job2Completed = false

            tracker.cancelAndRelaunch("key-1", scope) {
                try {
                    delay(10_000)
                    job1Completed = true
                } catch (_: Exception) {}
            }

            tracker.cancelAndRelaunch("key-2", scope) {
                try {
                    delay(10_000)
                    job2Completed = true
                } catch (_: Exception) {}
            }

            tracker.cancelAll()
            advanceUntilIdle()

            assertFalse(job1Completed, "Job 1 should be cancelled")
            assertFalse(job2Completed, "Job 2 should be cancelled")
        }
    }
}
