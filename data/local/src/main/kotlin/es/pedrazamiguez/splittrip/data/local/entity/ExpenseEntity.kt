package es.pedrazamiguez.splittrip.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["groupId", "syncStatus"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val title: String,
    val sourceAmount: Long,
    val sourceCurrency: String,
    val groupAmount: Long,
    val groupCurrency: String,
    val exchangeRate: String,
    val category: String? = null,
    val vendor: String? = null,
    val notes: String? = null,
    val paymentMethod: String,
    val paymentStatus: String? = null,
    val dueDateMillis: Long? = null,
    val receiptLocalUri: String? = null,
    val receiptMimeType: String? = null,
    val receiptCapturedAtMillis: Long? = null,
    val receiptRemoteUrl: String? = null,
    val createdBy: String,
    val payerType: String,
    val payerId: String? = null,
    val splitType: String = "EQUAL",
    val createdAtMillis: Long?,
    val lastUpdatedAtMillis: Long?,
    val cashTranchesJson: String? = null,
    val addOnsJson: String? = null,
    val syncStatus: String = "SYNCED"
)
