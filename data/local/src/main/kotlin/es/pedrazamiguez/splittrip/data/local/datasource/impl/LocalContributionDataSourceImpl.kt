package es.pedrazamiguez.splittrip.data.local.datasource.impl

import es.pedrazamiguez.splittrip.data.local.dao.ContributionDao
import es.pedrazamiguez.splittrip.data.local.mapper.toDomain
import es.pedrazamiguez.splittrip.data.local.mapper.toEntity
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalContributionDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Contribution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalContributionDataSourceImpl(private val contributionDao: ContributionDao) : LocalContributionDataSource {

    override fun getContributionsByGroupIdFlow(groupId: String): Flow<List<Contribution>> =
        contributionDao.getContributionsByGroupIdFlow(groupId).map { entities ->
            entities.toDomain()
        }

    override suspend fun saveContribution(contribution: Contribution) {
        contributionDao.insertContribution(contribution.toEntity())
    }

    override suspend fun findContributionById(contributionId: String): Contribution? =
        contributionDao.getContributionById(contributionId)?.toDomain()

    override suspend fun deleteContribution(contributionId: String) {
        contributionDao.deleteContribution(contributionId)
    }

    override suspend fun deleteContributionsByGroupId(groupId: String) {
        contributionDao.deleteContributionsByGroupId(groupId)
    }

    override suspend fun replaceContributionsForGroup(groupId: String, contributions: List<Contribution>) {
        contributionDao.replaceContributionsForGroup(
            groupId,
            contributions.map { it.toEntity() }
        )
    }

    override suspend fun applyContributionsDeltaForGroup(
        groupId: String,
        upserts: List<Contribution>,
        deletedIds: List<String>
    ) {
        contributionDao.applyContributionsDeltaForGroup(
            groupId = groupId,
            upserts = upserts.map { it.toEntity() },
            deletedIds = deletedIds
        )
    }

    override suspend fun getContributionIdsByGroup(groupId: String): List<String> =
        contributionDao.getContributionIdsByGroupId(groupId)

    override suspend fun clearAllContributions() {
        contributionDao.clearAllContributions()
    }

    override suspend fun updateSyncStatus(contributionId: String, syncStatus: SyncStatus) {
        contributionDao.updateSyncStatus(contributionId, syncStatus.name)
    }

    override suspend fun getPendingSyncContributionIds(groupId: String): List<String> =
        contributionDao.getPendingSyncContributionIds(groupId)

    override suspend fun deleteByLinkedExpenseId(groupId: String, linkedExpenseId: String) {
        contributionDao.deleteByLinkedExpenseId(groupId, linkedExpenseId)
    }

    override suspend fun findByLinkedExpenseId(groupId: String, linkedExpenseId: String): Contribution? =
        contributionDao.findByLinkedExpenseId(groupId, linkedExpenseId)?.toDomain()
}
