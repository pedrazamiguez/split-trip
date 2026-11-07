package es.pedrazamiguez.splittrip.data.sync

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Manages reference-counted keyed cloud subscription Jobs, ensuring only one active
 * subscription exists per key at any time across multiple collectors.
 *
 * Prevents duplicate Firestore snapshot listeners from accumulating when
 * `onStart` fires multiple times (e.g., config changes, tab switches,
 * multiple active screens collecting group data).
 *
 * A key whose listener is already running is not restarted when another collector
 * subscribes. The listener stops only after the last collector leaves, after a grace period.
 */
internal class KeyedSubscriptionTracker(
    private val defaultGracePeriodMs: Long = 5_000L
) {
    private data class SubscriptionEntry(
        val job: Job,
        var refCount: Int,
        var pendingCancelJob: Job? = null
    )

    private val subscriptions = ConcurrentHashMap<String, SubscriptionEntry>()
    private val lock = Any()

    fun acquire(key: String, scope: CoroutineScope, block: suspend () -> Unit) {
        synchronized(lock) {
            val entry = subscriptions[key]
            if (entry != null && entry.job.isActive) {
                entry.pendingCancelJob?.cancel()
                entry.pendingCancelJob = null
                entry.refCount++
            } else {
                entry?.job?.cancel()
                entry?.pendingCancelJob?.cancel()
                val job = scope.launch { block() }
                subscriptions[key] = SubscriptionEntry(job = job, refCount = 1)
            }
        }
    }

    fun release(key: String, scope: CoroutineScope, gracePeriodMs: Long = defaultGracePeriodMs) {
        synchronized(lock) {
            val entry = subscriptions[key] ?: return
            entry.refCount--
            if (entry.refCount <= 0) {
                if (gracePeriodMs <= 0L) {
                    entry.job.cancel()
                    subscriptions.remove(key)
                } else {
                    entry.pendingCancelJob?.cancel()
                    entry.pendingCancelJob = scope.launch {
                        delay(gracePeriodMs)
                        synchronized(lock) {
                            if (entry.refCount <= 0) {
                                entry.job.cancel()
                                subscriptions.remove(key)
                            }
                        }
                    }
                }
            }
        }
    }

    fun cancelAll() {
        synchronized(lock) {
            subscriptions.values.forEach {
                it.pendingCancelJob?.cancel()
                it.job.cancel()
            }
            subscriptions.clear()
        }
    }

    fun isSubscribed(key: String): Boolean = synchronized(lock) {
        subscriptions[key]?.job?.isActive == true
    }

    fun getSubscriberCount(key: String): Int = synchronized(lock) {
        subscriptions[key]?.refCount ?: 0
    }
}

internal fun <T> Flow<T>.trackSubscription(
    tracker: KeyedSubscriptionTracker,
    key: String,
    scope: CoroutineScope,
    gracePeriodMs: Long = 5_000L,
    block: suspend () -> Unit
): Flow<T> = onStart {
    tracker.acquire(key, scope, block)
}.onCompletion {
    tracker.release(key, scope, gracePeriodMs)
}
