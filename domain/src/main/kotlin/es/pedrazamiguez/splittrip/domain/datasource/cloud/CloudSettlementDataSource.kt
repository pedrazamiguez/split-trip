package es.pedrazamiguez.splittrip.domain.datasource.cloud

import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import kotlinx.coroutines.flow.Flow

interface CloudSettlementDataSource {
    suspend fun fetchSettlementsFullSnapshot(groupId: String): DeltaSyncBatch<SettlementRecord>
    suspend fun fetchSettlementsDelta(
        groupId: String,
        sinceWatermark: SyncWatermark
    ): DeltaSyncBatch<SettlementRecord>
    fun observeSettlementsDeltaFlow(
        groupId: String,
        sinceWatermark: SyncWatermark?
    ): Flow<DeltaSyncBatch<SettlementRecord>>
    suspend fun upsertSettlement(groupId: String, record: SettlementRecord)
    suspend fun deleteSettlement(groupId: String, id: String)
    suspend fun verifySettlementOnServer(groupId: String, id: String): Boolean
    suspend fun sendDebtorNudge(
        groupId: String,
        settlementId: String,
        fromUserId: String,
        toUserId: String,
        amountCents: Long? = null,
        currency: String? = null
    )
}
