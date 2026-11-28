package es.pedrazamiguez.splittrip.domain.datasource.local

import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawalRemainingAmountUpdate

/**
 * Write-only interface for local cash withdrawal data mutations.
 *
 * Segregated from [LocalCashWithdrawalQueryDataSource] following the Interface Segregation
 * Principle to keep each interface under the `TooManyFunctions` Detekt threshold.
 */
interface LocalCashWithdrawalWriteDataSource {

    suspend fun saveWithdrawal(withdrawal: CashWithdrawal)

    suspend fun updateRemainingAmount(withdrawalId: String, newRemaining: Long)

    /**
     * Atomically updates the remaining amount on multiple withdrawals in a single transaction.
     * Used during FIFO cash expense processing to batch all tranche deductions together.
     */
    suspend fun updateRemainingAmounts(updates: List<CashWithdrawalRemainingAmountUpdate>)

    suspend fun deleteWithdrawal(withdrawalId: String)

    suspend fun deleteWithdrawalsByGroupId(groupId: String)

    /**
     * Atomically replaces all withdrawals for a group with the provided list.
     * Used during real-time sync to reconcile local state with the cloud snapshot.
     */
    suspend fun replaceWithdrawalsForGroup(groupId: String, withdrawals: List<CashWithdrawal>)

    /**
     * Atomically applies an incremental delta (upserts + tombstone deletions) for a group.
     */
    suspend fun applyWithdrawalsDeltaForGroup(
        groupId: String,
        upserts: List<CashWithdrawal>,
        deletedIds: List<String>
    )

    /**
     * Updates the sync status of a single cash withdrawal.
     * Used by repositories to track cloud sync progress (PENDING_SYNC → SYNCED / SYNC_FAILED).
     */
    suspend fun updateSyncStatus(withdrawalId: String, syncStatus: SyncStatus)

    suspend fun clearAllWithdrawals()
}
