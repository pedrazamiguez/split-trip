package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseSplitDocument
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit

fun ExpenseSplitDocument.toDomain(): ExpenseSplit = ExpenseSplit(
    userId = userId,
    amountCents = amountCents ?: 0L,
    percentage = percentage?.toBigDecimalOrNull(),
    isExcluded = isExcluded,
    isCoveredById = isCoveredById,
    subunitId = subunitId,
    splitType = splitType?.let { runCatching { SplitType.valueOf(it) }.getOrNull() }
)

fun ExpenseSplit.toDocument(): ExpenseSplitDocument = ExpenseSplitDocument(
    userId = userId,
    amountCents = amountCents,
    percentage = percentage?.toPlainString(),
    isExcluded = isExcluded,
    isCoveredById = isCoveredById,
    subunitId = subunitId,
    splitType = splitType?.name
)

fun List<ExpenseSplitDocument>.toDomainSplits(): List<ExpenseSplit> = map { it.toDomain() }

fun List<ExpenseSplit>.toSplitDocuments(): List<ExpenseSplitDocument> = map { it.toDocument() }
