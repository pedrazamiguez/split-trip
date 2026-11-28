package es.pedrazamiguez.splittrip.data.local.datasource.impl

import es.pedrazamiguez.splittrip.data.local.dao.SettlementRecordDao
import es.pedrazamiguez.splittrip.data.local.mapper.toDomain
import es.pedrazamiguez.splittrip.data.local.mapper.toEntity
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSettlementDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalSettlementDataSourceImpl(
    private val settlementRecordDao: SettlementRecordDao
) : LocalSettlementDataSource {

    override fun getSettlementsByGroupIdFlow(groupId: String): Flow<List<SettlementRecord>> =
        settlementRecordDao.getByGroupIdFlow(groupId).map { entities -> entities.toDomain() }

    override suspend fun getSettlementsByGroupId(groupId: String): List<SettlementRecord> =
        settlementRecordDao.getByGroupId(groupId).toDomain()

    override suspend fun getSettlementsByMember(
        groupId: String,
        userId: String
    ): List<SettlementRecord> =
        settlementRecordDao.getByMember(groupId, userId).toDomain()

    override suspend fun getSettlementById(id: String): SettlementRecord? =
        settlementRecordDao.getById(id)?.toDomain()

    override suspend fun saveSettlement(record: SettlementRecord, syncStatus: SyncStatus) {
        settlementRecordDao.upsert(record.toEntity(syncStatus))
    }

    override suspend fun deleteSettlement(id: String) {
        settlementRecordDao.deleteById(id)
    }

    override suspend fun getSyncStatus(id: String): SyncStatus? =
        settlementRecordDao.getSyncStatus(id)?.let { SyncStatus.fromStringOrDefault(it) }

    override suspend fun updateSyncStatus(id: String, syncStatus: SyncStatus) {
        settlementRecordDao.updateSyncStatus(id, syncStatus.name)
    }

    override suspend fun getPendingSyncSettlementIds(groupId: String): List<String> =
        settlementRecordDao.getPendingSyncIds(groupId)

    override suspend fun replaceSettlementsForGroup(
        groupId: String,
        records: List<SettlementRecord>
    ) {
        settlementRecordDao.replaceForGroup(groupId, records.map { it.toEntity() })
    }

    override suspend fun applySettlementsDeltaForGroup(
        groupId: String,
        upserts: List<SettlementRecord>,
        deletedIds: List<String>
    ) {
        settlementRecordDao.applyDeltaForGroup(
            groupId = groupId,
            upserts = upserts.map { it.toEntity() },
            deletedIds = deletedIds
        )
    }
}
