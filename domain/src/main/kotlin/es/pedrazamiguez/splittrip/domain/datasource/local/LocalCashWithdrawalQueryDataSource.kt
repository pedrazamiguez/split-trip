package es.pedrazamiguez.splittrip.domain.datasource.local

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import kotlinx.coroutines.flow.Flow

/**
 * Read-only interface for local cash withdrawal data access.
 *
 * Segregated from [LocalCashWithdrawalWriteDataSource] following the Interface Segregation
 * Principle to keep each interface under the `TooManyFunctions` Detekt threshold.
 */
interface LocalCashWithdrawalQueryDataSource {

    fun getWithdrawalsByGroupIdFlow(groupId: String): Flow<List<CashWithdrawal>>

    /**
     * Fetches available (non-exhausted) withdrawals for a specific currency,
     * ordered by createdAt ascending (oldest first) for FIFO consumption.
     *
     * **Scope-blind:** returns all withdrawals regardless of scope.
     * Prefer the scoped variants below for FIFO pool selection in expense processing.
     */
    suspend fun getAvailableWithdrawals(groupId: String, currency: String): List<CashWithdrawal>

    /**
     * Fetches available GROUP-scoped withdrawals for FIFO consumption.
     * Ordered by createdAt ascending (oldest first).
     */
    suspend fun getAvailableWithdrawalsGroupScoped(
        groupId: String,
        currency: String
    ): List<CashWithdrawal>

    /**
     * Fetches available USER-scoped withdrawals for a specific user, for FIFO consumption.
     * Ordered by createdAt ascending (oldest first).
     */
    suspend fun getAvailableWithdrawalsUserScoped(
        groupId: String,
        currency: String,
        withdrawnBy: String
    ): List<CashWithdrawal>

    /**
     * Fetches available SUBUNIT-scoped withdrawals for a specific subunit, for FIFO consumption.
     * Ordered by createdAt ascending (oldest first).
     */
    suspend fun getAvailableWithdrawalsSubunitScoped(
        groupId: String,
        currency: String,
        subunitId: String
    ): List<CashWithdrawal>

    suspend fun getWithdrawalById(withdrawalId: String): CashWithdrawal?

    suspend fun getWithdrawalIdsByGroup(groupId: String): List<String>

    /**
     * Returns IDs of withdrawals in a group that are waiting for server confirmation.
     * Used by the repository after reconciliation to attempt server verification
     * and transition PENDING_SYNC items to SYNCED.
     */
    suspend fun getPendingSyncWithdrawalIds(groupId: String): List<String>
}
