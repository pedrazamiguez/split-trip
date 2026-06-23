package es.pedrazamiguez.splittrip.core.common.constant

import es.pedrazamiguez.splittrip.core.common.provider.RemoteConfigProvider

object AppConstants {
    private const val FALLBACK_DEFAULT_CURRENCY_CODE = "EUR"
    private const val FALLBACK_BALANCE_COMPUTATION_DEBOUNCE_MS = 300L

    @Volatile
    var remoteConfigProvider: RemoteConfigProvider? = null

    val DEFAULT_CURRENCY_CODE: String
        get() = remoteConfigProvider?.getString("default_currency_code") ?: FALLBACK_DEFAULT_CURRENCY_CODE

    const val FLOW_RETENTION_TIME = 5_000L

    /**
     * Replay-cache expiration for `stateIn(WhileSubscribed(...))`.
     *
     * After the upstream stops (i.e., [FLOW_RETENTION_TIME] after the last
     * subscriber leaves), the replay cache is reset to `initialValue`
     * immediately. This prevents stale data (e.g., an old empty-state) from
     * being shown for a single frame when a new subscriber attaches, which
     * would otherwise cause an "Empty View → Shimmer → Content" flash on
     * tab re-entry.
     *
     * Use together with [FLOW_RETENTION_TIME]:
     * ```
     * .stateIn(
     *     scope = viewModelScope,
     *     started = SharingStarted.WhileSubscribed(
     *         stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
     *         replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
     *     ),
     *     initialValue = MyUiState(isLoading = true)
     * )
     * ```
     */
    const val FLOW_REPLAY_EXPIRATION = 0L

    /**
     * Debounce window (in ms) applied to the balance computation combine.
     *
     * When several Room tables change in quick succession (e.g. during a Firestore
     * reconciliation burst that upserts expenses + splits + withdrawals), each table
     * change triggers a new emission from its upstream Flow. Without debouncing,
     * `computeMemberBalances()` (an O(E × S) Kotlin computation) runs once per
     * individual table write rather than once per logical "batch".
     *
     * 300 ms is long enough to swallow a typical multi-table reconciliation write
     * (which completes in < 100 ms on Room's IO dispatcher) while keeping the UI
     * responsive to genuine user-initiated changes.
     */
    val BALANCE_COMPUTATION_DEBOUNCE_MS: Long
        get() = remoteConfigProvider?.getLong("balance_computation_debounce_ms")
            ?: FALLBACK_BALANCE_COMPUTATION_DEBOUNCE_MS
}
