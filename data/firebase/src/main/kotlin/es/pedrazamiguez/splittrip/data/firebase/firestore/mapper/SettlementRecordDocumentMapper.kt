package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.FirestoreSyncFields
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus

fun SettlementRecord.toDocument(): Map<String, Any?> = mapOf(
    "id" to id,
    "groupId" to groupId,
    "fromUserId" to settlement.fromUserId,
    "toUserId" to settlement.toUserId,
    "amountCents" to settlement.amount.toString(),
    "currency" to settlement.currency,
    "sourcePocket" to settlement.sourcePocket.name,
    "status" to status.name,
    "createdAt" to createdAt.toTimestampUtc(),
    "confirmedByPayerAt" to confirmedByPayerAt?.toTimestampUtc(),
    "confirmedByPayeeAt" to confirmedByPayeeAt?.toTimestampUtc(),
    "resolvedAt" to resolvedAt?.toTimestampUtc(),
    "disputedBy" to disputedBy,
    "disputeReason" to disputeReason,
    FirestoreSyncFields.SERVER_UPDATED_AT to FieldValue.serverTimestamp(),
    FirestoreSyncFields.IS_DELETED to false,
    FirestoreSyncFields.DELETED_AT to null,
    FirestoreSyncFields.EXPIRE_AT to null
)

fun DocumentSnapshot.toSettlementRecord(): SettlementRecord? {
    if (getBoolean(FirestoreSyncFields.IS_DELETED) == true) return null
    val id = getString("id") ?: return null
    val groupId = getString("groupId") ?: return null
    val fromUserId = getString("fromUserId") ?: return null
    val toUserId = getString("toUserId") ?: return null
    val amountCents = getString("amountCents")?.toLongOrNull() ?: return null
    val currency = getString("currency") ?: ""
    val sourcePocketStr = getString("sourcePocket") ?: SettlementPocketType.NET.name
    val statusStr = getString("status") ?: SettlementStatus.SUGGESTED.name
    val createdAtTimestamp = getTimestamp("createdAt")

    return SettlementRecord(
        id = id,
        groupId = groupId,
        settlement = Settlement(
            fromUserId = fromUserId,
            toUserId = toUserId,
            amount = amountCents,
            currency = currency,
            sourcePocket = SettlementPocketType.valueOf(sourcePocketStr)
        ),
        status = SettlementStatus.fromString(statusStr),
        createdAt = createdAtTimestamp?.toLocalDateTimeUtc()
            ?: return null,
        confirmedByPayerAt = getTimestamp("confirmedByPayerAt")?.toLocalDateTimeUtc(),
        confirmedByPayeeAt = getTimestamp("confirmedByPayeeAt")?.toLocalDateTimeUtc(),
        resolvedAt = getTimestamp("resolvedAt")?.toLocalDateTimeUtc(),
        disputedBy = getString("disputedBy"),
        disputeReason = getString("disputeReason")
    )
}
