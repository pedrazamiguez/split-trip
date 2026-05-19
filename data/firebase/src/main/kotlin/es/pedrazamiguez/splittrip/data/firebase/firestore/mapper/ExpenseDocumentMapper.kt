package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import com.google.firebase.firestore.DocumentReference
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.AddOnDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.AttachmentDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseDocument
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import java.math.BigDecimal
import java.time.LocalDateTime

fun Expense.toDocument(expenseId: String, groupId: String, groupDocRef: DocumentReference, userId: String) =
    ExpenseDocument(
        expenseId = expenseId,
        groupId = groupId,
        groupRef = groupDocRef,
        title = title,
        expenseCategory = category.name,
        vendor = vendor,
        amountCents = sourceAmount,
        currency = sourceCurrency,
        groupCurrency = groupCurrency,
        groupAmountCents = groupAmount,
        exchangeRate = exchangeRate.toPlainString(),
        operationDate = LocalDateTime
            .now()
            .toTimestampUtc(),
        paymentMethod = paymentMethod.name,
        paymentStatus = paymentStatus.name,
        dueDate = dueDate.toTimestampUtc(),
        cashTranches = cashTranches.map { tranche ->
            mapOf(
                "withdrawalId" to tranche.withdrawalId,
                "amountConsumed" to tranche.amountConsumed
            )
        },
        addOns = addOns.map { it.toAddOnDocument() },
        splits = splits.toSplitDocuments(),
        splitType = splitType.name,
        notes = notes,
        payerType = payerType.name,
        payerId = payerId,
        createdBy = userId,
        lastUpdatedBy = userId,
        createdAt = createdAt?.toTimestampUtc(),
        lastUpdatedAt = lastUpdatedAt?.toTimestampUtc(),
        // Only include the attachment in the Firestore document once it has a remote URL.
        // The local URI is an on-device path with no meaning on other devices.
        attachments = buildReceiptAttachmentDocuments(receiptAttachment)
    )

fun ExpenseDocument.toDomain(): Expense {
    val resolvedPayerType = runCatching { PayerType.fromString(payerType) }.getOrDefault(PayerType.GROUP)

    return Expense(
        id = expenseId,
        groupId = groupId,
        title = title,
        category = runCatching { ExpenseCategory.fromString(expenseCategory) }.getOrDefault(
            ExpenseCategory.OTHER
        ),
        vendor = vendor,
        notes = notes,
        sourceAmount = amountCents,
        sourceCurrency = currency,
        groupAmount = groupAmountCents ?: amountCents,
        groupCurrency = groupCurrency,
        exchangeRate = exchangeRate?.toBigDecimalOrNull() ?: BigDecimal.ONE,
        addOns = addOns.map { it.toDomainAddOn() },
        paymentMethod = runCatching { PaymentMethod.fromString(paymentMethod) }.getOrDefault(
            PaymentMethod.OTHER
        ),
        paymentStatus = runCatching { PaymentStatus.fromString(paymentStatus) }.getOrDefault(
            PaymentStatus.FINISHED
        ),
        dueDate = dueDate.toLocalDateTimeUtc(),
        cashTranches = cashTranches.mapNotNull { map ->
            val withdrawalId = map["withdrawalId"] as? String ?: return@mapNotNull null
            val amountConsumed = (map["amountConsumed"] as? Number)?.toLong() ?: return@mapNotNull null
            CashTranche(withdrawalId = withdrawalId, amountConsumed = amountConsumed)
        },
        splitType = runCatching { SplitType.fromString(splitType) }.getOrDefault(SplitType.EQUAL),
        splits = splits.toDomainSplits(),
        createdBy = createdBy,
        payerType = resolvedPayerType,
        payerId = payerId.takeUnless { resolvedPayerType == PayerType.GROUP },
        createdAt = createdAt.toLocalDateTimeUtc(),
        lastUpdatedAt = lastUpdatedAt.toLocalDateTimeUtc(),
        // Restore the first attachment as a ReceiptAttachment if it has a remote URL.
        // localUri is intentionally left blank — the file does not exist on this device yet.
        receiptAttachment = attachments.firstOrNull()?.let { doc ->
            val remoteUrl = doc.path.ifBlank { null } ?: return@let null
            ReceiptAttachment(
                localUri = "",
                mimeType = doc.mime ?: "application/octet-stream",
                capturedAtMillis = doc.uploadedAt?.toDate()?.time ?: 0L,
                remoteUrl = remoteUrl
            )
        }
    )
}

// ── AddOn ↔ AddOnDocument mappers ────────────────────────────────────

private fun buildReceiptAttachmentDocuments(
    attachment: es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment?
): List<AttachmentDocument> {
    val remoteUrl = attachment?.remoteUrl ?: return emptyList()
    return listOf(AttachmentDocument(path = remoteUrl, mime = attachment.mimeType))
}

private fun AddOn.toAddOnDocument() = AddOnDocument(
    id = id,
    type = type.name,
    mode = mode.name,
    valueType = valueType.name,
    amountCents = amountCents,
    currency = currency,
    exchangeRate = exchangeRate.toPlainString(),
    groupAmountCents = groupAmountCents,
    paymentMethod = paymentMethod.name,
    description = description
)

private fun AddOnDocument.toDomainAddOn() = AddOn(
    id = id,
    type = runCatching { AddOnType.fromString(type) }.getOrDefault(AddOnType.FEE),
    mode = runCatching { AddOnMode.fromString(mode) }.getOrDefault(AddOnMode.ON_TOP),
    valueType = runCatching { AddOnValueType.fromString(valueType) }.getOrDefault(AddOnValueType.EXACT),
    amountCents = amountCents,
    currency = currency,
    exchangeRate = exchangeRate?.toBigDecimalOrNull() ?: BigDecimal.ONE,
    groupAmountCents = groupAmountCents,
    paymentMethod = runCatching { PaymentMethod.fromString(paymentMethod) }.getOrDefault(
        PaymentMethod.OTHER
    ),
    description = description
)
