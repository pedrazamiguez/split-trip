package es.pedrazamiguez.splittrip.domain.datasource.cloud

import es.pedrazamiguez.splittrip.domain.model.CashTransfer
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import kotlinx.coroutines.flow.Flow

interface CloudCashTransferDataSource {
    suspend fun upsertCashTransfer(transfer: CashTransfer): Result<Unit>
    suspend fun deleteCashTransfer(groupId: String, transferId: String): Result<Unit>
    suspend fun fetchGroupCashTransfersFullSnapshot(groupId: String): DeltaSyncBatch<CashTransfer>
    suspend fun fetchGroupCashTransfersDelta(
        groupId: String,
        sinceWatermark: SyncWatermark
    ): DeltaSyncBatch<CashTransfer>
    fun observeGroupCashTransfersDeltaFlow(
        groupId: String,
        sinceWatermark: SyncWatermark?
    ): Flow<DeltaSyncBatch<CashTransfer>>
    suspend fun verifyCashTransferOnServer(groupId: String, transferId: String): Boolean
}
