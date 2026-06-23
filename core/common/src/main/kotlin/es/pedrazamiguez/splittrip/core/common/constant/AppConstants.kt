package es.pedrazamiguez.splittrip.core.common.constant

object AppConstants {
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
}
