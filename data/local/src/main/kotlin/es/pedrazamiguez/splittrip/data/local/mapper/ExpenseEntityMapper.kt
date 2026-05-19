package es.pedrazamiguez.splittrip.data.local.mapper

import es.pedrazamiguez.splittrip.core.common.extensions.toEpochMillisUtc
import es.pedrazamiguez.splittrip.core.common.extensions.toLocalDateTimeUtc
import es.pedrazamiguez.splittrip.data.local.converter.AddOnListConverter
import es.pedrazamiguez.splittrip.data.local.converter.CashTrancheListConverter
import es.pedrazamiguez.splittrip.data.local.entity.ExpenseEntity
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import java.math.BigDecimal

private val cashTrancheConverter = CashTrancheListConverter()
private val addOnConverter = AddOnListConverter()

fun ExpenseEntity.toDomain(): Expense {
    val resolvedPayerType = runCatching { PayerType.fromString(payerType) }.getOrDefault(PayerType.GROUP)

    return Expense(
        id = id,
        groupId = groupId,
        title = title,
        sourceAmount = sourceAmount,
        sourceCurrency = sourceCurrency,
        groupAmount = groupAmount,
        groupCurrency = groupCurrency,
        exchangeRate = exchangeRate.toBigDecimalOrNull() ?: BigDecimal.ONE,
        category = category?.let {
            runCatching { ExpenseCategory.fromString(it) }.getOrDefault(ExpenseCategory.OTHER)
        } ?: ExpenseCategory.OTHER,
        vendor = vendor,
        notes = notes,
        paymentMethod = PaymentMethod.entries.find { it.name == paymentMethod } ?: PaymentMethod.OTHER,
        paymentStatus = paymentStatus?.let {
            runCatching { PaymentStatus.fromString(it) }.getOrDefault(PaymentStatus.FINISHED)
        } ?: PaymentStatus.FINISHED,
        dueDate = dueDateMillis?.toLocalDateTimeUtc(),
        receiptAttachment = buildReceiptAttachment(
            receiptLocalUri,
            receiptMimeType,
            receiptCapturedAtMillis,
            receiptRemoteUrl
        ),
        cashTranches = cashTrancheConverter.toCashTrancheList(cashTranchesJson) ?: emptyList(),
        addOns = addOnConverter.toAddOnList(addOnsJson) ?: emptyList(),
        splitType = runCatching { SplitType.fromString(splitType) }.getOrDefault(SplitType.EQUAL),
        createdBy = createdBy,
        payerType = resolvedPayerType,
        payerId = payerId.takeUnless { resolvedPayerType == PayerType.GROUP },
        createdAt = createdAtMillis?.toLocalDateTimeUtc(),
        lastUpdatedAt = lastUpdatedAtMillis?.toLocalDateTimeUtc(),
        syncStatus = SyncStatus.fromStringOrDefault(syncStatus)
    )
}

fun Expense.toEntity(): ExpenseEntity {
    val effectiveCreatedAtMillis = createdAt?.toEpochMillisUtc() ?: System.currentTimeMillis()
    val effectiveLastUpdatedAtMillis = lastUpdatedAt?.toEpochMillisUtc() ?: effectiveCreatedAtMillis

    return ExpenseEntity(
        id = id,
        groupId = groupId,
        title = title,
        sourceAmount = sourceAmount,
        sourceCurrency = sourceCurrency,
        groupAmount = groupAmount,
        groupCurrency = groupCurrency,
        exchangeRate = exchangeRate.toPlainString(),
        category = category.name,
        vendor = vendor,
        notes = notes,
        paymentMethod = paymentMethod.name,
        paymentStatus = paymentStatus.name,
        dueDateMillis = dueDate?.toEpochMillisUtc(),
        receiptLocalUri = receiptAttachment?.localUri,
        receiptMimeType = receiptAttachment?.mimeType,
        receiptCapturedAtMillis = receiptAttachment?.capturedAtMillis,
        receiptRemoteUrl = receiptAttachment?.remoteUrl,
        createdBy = createdBy,
        payerType = payerType.name,
        payerId = payerId,
        splitType = splitType.name,
        createdAtMillis = effectiveCreatedAtMillis,
        lastUpdatedAtMillis = effectiveLastUpdatedAtMillis,
        cashTranchesJson = cashTrancheConverter.fromCashTrancheList(cashTranches.ifEmpty { null }),
        addOnsJson = addOnConverter.fromAddOnList(addOns.ifEmpty { null }),
        syncStatus = syncStatus.name
    )
}

fun List<ExpenseEntity>.toDomain(): List<Expense> = map { it.toDomain() }

fun List<Expense>.toEntity(): List<ExpenseEntity> = map { it.toEntity() }

/**
 * Reconstructs a [ReceiptAttachment] from the four nullable columns stored in Room.
 * Returns null when no receipt has been attached (all columns are null), or when only
 * [localUri] is present (legacy row from before v28 migration — treated as no attachment
 * because [mimeType] and [capturedAtMillis] are mandatory for the attachment model).
 */
private fun buildReceiptAttachment(
    localUri: String?,
    mimeType: String?,
    capturedAtMillis: Long?,
    remoteUrl: String?
): ReceiptAttachment? {
    if (localUri.isNullOrBlank() || mimeType.isNullOrBlank() || capturedAtMillis == null) return null
    return ReceiptAttachment(
        localUri = localUri,
        mimeType = mimeType,
        capturedAtMillis = capturedAtMillis,
        remoteUrl = remoteUrl
    )
}
