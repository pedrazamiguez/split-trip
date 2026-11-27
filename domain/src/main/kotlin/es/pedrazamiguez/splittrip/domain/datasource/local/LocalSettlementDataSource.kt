package es.pedrazamiguez.splittrip.domain.datasource.local

import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import kotlinx.coroutines.flow.Flow

interface LocalSettlementDataSource {
    fun getSettlementsByGroupIdFlow(groupId: String): Flow<List<SettlementRecord>>
    suspend fun getSettlementsByGroupId(groupId: String): List<SettlementRecord>
    suspend fun getSettlementsByMember(groupId: String, userId: String): List<SettlementRecord>
    suspend fun getSettlementById(id: String): SettlementRecord?
    suspend fun saveSettlement(record: SettlementRecord, syncStatus: SyncStatus = SyncStatus.SYNCED)
    suspend fun deleteSettlement(id: String)
    suspend fun getSyncStatus(id: String): SyncStatus?
    suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus)
    suspend fun getPendingSyncSettlementIds(groupId: String): List<String>
    suspend fun replaceSettlementsForGroup(groupId: String, records: List<SettlementRecord>)
    suspend fun applySettlementsDeltaForGroup(
        groupId: String,
        upserts: List<SettlementRecord>,
        deletedIds: List<String>
    )
}
