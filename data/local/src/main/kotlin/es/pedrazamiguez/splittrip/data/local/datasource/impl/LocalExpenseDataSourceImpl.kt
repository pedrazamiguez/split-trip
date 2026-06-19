package es.pedrazamiguez.splittrip.data.local.datasource.impl

import androidx.room.withTransaction
import es.pedrazamiguez.splittrip.data.local.dao.ExpenseDao
import es.pedrazamiguez.splittrip.data.local.dao.ExpenseSplitDao
import es.pedrazamiguez.splittrip.data.local.database.AppDatabase
import es.pedrazamiguez.splittrip.data.local.mapper.toDomain
import es.pedrazamiguez.splittrip.data.local.mapper.toDomainSplits
import es.pedrazamiguez.splittrip.data.local.mapper.toEntity
import es.pedrazamiguez.splittrip.data.local.mapper.toSplitEntities
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalExpenseDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class LocalExpenseDataSourceImpl(
    private val appDatabase: AppDatabase,
    private val expenseDao: ExpenseDao,
    private val expenseSplitDao: ExpenseSplitDao
) : LocalExpenseDataSource {

    override fun getExpensesByGroupIdFlow(groupId: String): Flow<List<Expense>> =
        expenseDao.getExpensesByGroupIdFlow(groupId).map { entities ->
            val domainExpenses = entities.toDomain()
            if (domainExpenses.isEmpty()) {
                emptyList()
            } else {
                val expenseIds = domainExpenses.map { it.id }
                val splitEntities = expenseSplitDao.getSplitsByExpenseIds(expenseIds)
                val splitsByExpenseId = splitEntities.groupBy { it.expenseId }

                domainExpenses.map { expense ->
                    val splitsForExpense = splitsByExpenseId[expense.id].orEmpty()
                    expense.copy(splits = splitsForExpense.toDomainSplits())
                }
            }
        }

    override suspend fun getExpenseById(expenseId: String): Expense? =
        expenseDao.getExpenseById(expenseId)?.toDomain()?.let { expense ->
            val splitEntities = expenseSplitDao.getSplitsByExpenseId(expenseId)
            expense.copy(splits = splitEntities.toDomainSplits())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getExpenseByIdFlow(expenseId: String): Flow<Expense?> =
        expenseDao.getExpenseByIdFlow(expenseId).flatMapLatest { entity ->
            if (entity == null) {
                flowOf(null)
            } else {
                expenseSplitDao.getSplitsByExpenseIdFlow(expenseId).map { splits ->
                    entity.toDomain().copy(splits = splits.toDomainSplits())
                }
            }
        }

    override suspend fun saveExpenses(expenses: List<Expense>) {
        appDatabase.withTransaction {
            expenseDao.insertExpenses(expenses.toEntity())
            expenses.forEach { expense ->
                if (expense.splits.isNotEmpty()) {
                    expenseSplitDao.replaceSplitsForExpense(
                        expense.id,
                        expense.splits.toSplitEntities(expense.id)
                    )
                }
            }
        }
    }

    override suspend fun saveExpense(expense: Expense) {
        appDatabase.withTransaction {
            expenseDao.insertExpense(expense.toEntity())
            if (expense.splits.isNotEmpty()) {
                expenseSplitDao.replaceSplitsForExpense(
                    expense.id,
                    expense.splits.toSplitEntities(expense.id)
                )
            }
        }
    }

    override suspend fun deleteExpense(expenseId: String) {
        // Splits are deleted via CASCADE
        expenseDao.deleteExpense(expenseId)
    }

    override suspend fun deleteExpensesByGroupId(groupId: String) {
        // Splits are deleted via CASCADE
        expenseDao.deleteExpensesByGroupId(groupId)
    }

    override suspend fun replaceExpensesForGroup(groupId: String, expenses: List<Expense>) {
        appDatabase.withTransaction {
            expenseDao.replaceExpensesForGroup(groupId, expenses.toEntity())
            expenses.forEach { expense ->
                expenseSplitDao.replaceSplitsForExpense(
                    expense.id,
                    expense.splits.toSplitEntities(expense.id)
                )
            }
        }
    }

    override suspend fun getExpenseIdsByGroup(groupId: String): List<String> =
        expenseDao.getExpenseIdsByGroupId(groupId)

    override suspend fun updateSyncStatus(expenseId: String, syncStatus: SyncStatus) {
        expenseDao.updateSyncStatus(expenseId, syncStatus.name)
    }

    override suspend fun getPendingSyncExpenseIds(groupId: String): List<String> =
        expenseDao.getPendingSyncExpenseIds(groupId)

    override suspend fun updateReceiptRemoteUrl(expenseId: String, remoteUrl: String) {
        expenseDao.updateReceiptRemoteUrl(expenseId, remoteUrl)
    }

    override suspend fun updateReceiptLocalUri(expenseId: String, localUri: String) {
        expenseDao.updateReceiptLocalUri(expenseId, localUri)
    }

    override suspend fun clearAllExpenses() {
        expenseDao.clearAllExpenses()
    }
}
