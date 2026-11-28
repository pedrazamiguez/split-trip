package es.pedrazamiguez.splittrip.domain.datasource.cloud

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import kotlinx.coroutines.flow.Flow

interface CloudCashWithdrawalDataSource {

    suspend fun addWithdrawal(groupId: String, withdrawal: CashWithdrawal)

    suspend fun updateWithdrawal(groupId: String, withdrawal: CashWithdrawal)

    suspend fun deleteWithdrawal(groupId: String, withdrawalId: String)

    /**
     * One-shot paginated full-collection fetch used for initial bootstrap or expired-checkpoint fallback.
     */
    suspend fun fetchWithdrawalsFullSnapshot(groupId: String): DeltaSyncBatch<CashWithdrawal>

    /**
     * One-shot incremental delta fetch for changes after [sinceWatermark].
     */
    suspend fun fetchWithdrawalsDelta(
        groupId: String,
        sinceWatermark: SyncWatermark
    ): DeltaSyncBatch<CashWithdrawal>

    /**
     * Reactive delta stream for real-time observers starting after [sinceWatermark].
     */
    fun observeWithdrawalsDeltaFlow(
        groupId: String,
        sinceWatermark: SyncWatermark?
    ): Flow<DeltaSyncBatch<CashWithdrawal>>

    /**
     * Verifies that a withdrawal document exists on the Firestore server (not just local cache).
     * Forces a server round-trip — throws if the device is offline.
     *
     * Used by repositories to confirm that a locally-created withdrawal has been
     * successfully persisted to the server, enabling the PENDING_SYNC → SYNCED transition.
     *
     * @param groupId The ID of the group containing the withdrawal.
     * @param withdrawalId The ID of the withdrawal to verify.
     * @return true if the withdrawal exists on the server.
     * @throws Exception if the server is unreachable (e.g., airplane mode).
     */
    suspend fun verifyWithdrawalOnServer(groupId: String, withdrawalId: String): Boolean
}
