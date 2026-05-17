package es.pedrazamiguez.splittrip.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_splits",
    indices = [Index(value = ["expenseId"])],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: String,
    val userId: String,
    val amountCents: Long,
    val percentage: String?,
    val isExcluded: Boolean,
    val isCoveredById: String?,
    val subunitId: String? = null,
    val splitType: String? = null
)
