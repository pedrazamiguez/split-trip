package es.pedrazamiguez.splittrip.domain.datasource.cloud

import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import kotlinx.coroutines.flow.Flow

interface CloudSettlementDataSource {
    suspend fun fetchSettlementsByGroupId(groupId: String): List<SettlementRecord>
    fun getSettlementsByGroupIdFlow(groupId: String): Flow<List<SettlementRecord>>
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
