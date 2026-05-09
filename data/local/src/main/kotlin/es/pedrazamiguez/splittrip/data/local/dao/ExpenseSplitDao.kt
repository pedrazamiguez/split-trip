package es.pedrazamiguez.splittrip.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import es.pedrazamiguez.splittrip.data.local.entity.ExpenseSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseSplitDao {

    @Upsert
    suspend fun upsertSplits(splits: List<ExpenseSplitEntity>)

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId ORDER BY id ASC")
    suspend fun getSplitsByExpenseId(expenseId: String): List<ExpenseSplitEntity>

    @Query("SELECT * FROM expense_splits WHERE expenseId IN (:expenseIds) ORDER BY expenseId ASC, id ASC")
    suspend fun getSplitsByExpenseIds(expenseIds: List<String>): List<ExpenseSplitEntity>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId ORDER BY id ASC")
    fun getSplitsByExpenseIdFlow(expenseId: String): Flow<List<ExpenseSplitEntity>>

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsByExpenseId(expenseId: String)

    /**
     * Replaces all splits for a given expense atomically.
     * Deletes existing splits first, then inserts the new ones.
     */
    @Transaction
    suspend fun replaceSplitsForExpense(expenseId: String, splits: List<ExpenseSplitEntity>) {
        deleteSplitsByExpenseId(expenseId)
        if (splits.isNotEmpty()) {
            upsertSplits(splits)
        }
    }
}
