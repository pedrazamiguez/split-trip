package es.pedrazamiguez.splittrip.data.sync

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Manages a set of keyed cloud subscription Jobs, ensuring only one active
 * subscription exists per key at any time.
 *
 * Prevents duplicate Firestore snapshot listeners from accumulating when
 * `onStart` fires multiple times (e.g., config changes, tab switches,
 * `WhileSubscribed` resubscriptions, `flatMapLatest` restarts).
 *
 * Used by group-keyed repositories (Expense, Subunit, Contribution, CashWithdrawal)
 * where subscriptions are scoped per `groupId`.
 */
internal class KeyedSubscriptionTracker {

    private val jobs = ConcurrentHashMap<String, Job>()

    /**
     * Cancels any existing subscription for [key] and launches a new one
     * in the given [scope].
     *
     * @param key Subscription key (typically a groupId).
     * @param scope CoroutineScope to launch the subscription in.
     * @param block Suspend function to run as the subscription body.
     */
    fun cancelAndRelaunch(
        key: String,
        scope: CoroutineScope,
        block: suspend () -> Unit
    ) {
        jobs.compute(key) { _, existing ->
            existing?.cancel()
            scope.launch { block() }
        }
    }

    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }
}
