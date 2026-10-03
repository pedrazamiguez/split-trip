package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetSettlementSuggestionsUseCase : UseCase {
    /** Produces NET settlements — for the Balances dashboard. */
    operator fun invoke(memberBalances: List<MemberBalance>): List<Settlement>

    /** Produces per-pocket-type settlements — for leave-group / trip-close flows. */
    fun invokeByPocket(memberBalances: List<MemberBalance>, groupCurrency: String): List<Settlement>

    /**
     * Ephemerally computes suggested settlements in-memory for the given member balances
     * and merges them with active (non-RESOLVED) persisted records.
     */
    fun getEphemeralSettlements(
        groupId: String,
        memberBalances: List<MemberBalance>,
        groupCurrency: String,
        persistedRecords: List<SettlementRecord>
    ): List<SettlementRecord>

    /**
     * Computes per-pocket settlement suggestions for [groupId], persists them as
     * [SettlementRecord]s (idempotent — skips pairs that already have a non-RESOLVED record),
     * and returns the full current list.
     *
     * Called by [LeaveGroupUseCaseImpl] and [ArchiveGroupUseCaseImpl] before checking resolution.
     */
    suspend fun persistForGroup(groupId: String, leavingUserId: String? = null): List<SettlementRecord>
}
