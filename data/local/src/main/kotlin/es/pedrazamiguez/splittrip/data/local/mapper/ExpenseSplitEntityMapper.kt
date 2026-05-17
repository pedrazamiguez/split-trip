package es.pedrazamiguez.splittrip.data.local.mapper

import es.pedrazamiguez.splittrip.data.local.entity.ExpenseSplitEntity
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import java.math.BigDecimal

fun ExpenseSplitEntity.toDomain(): ExpenseSplit = ExpenseSplit(
    userId = userId,
    amountCents = amountCents,
    percentage = percentage?.let { BigDecimal(it) },
    isExcluded = isExcluded,
    isCoveredById = isCoveredById,
    subunitId = subunitId,
    splitType = splitType?.let { runCatching { SplitType.valueOf(it) }.getOrNull() }
)

fun ExpenseSplit.toEntity(expenseId: String): ExpenseSplitEntity = ExpenseSplitEntity(
    expenseId = expenseId,
    userId = userId,
    amountCents = amountCents,
    percentage = percentage?.toPlainString(),
    isExcluded = isExcluded,
    isCoveredById = isCoveredById,
    subunitId = subunitId,
    splitType = splitType?.name
)

fun List<ExpenseSplitEntity>.toDomainSplits(): List<ExpenseSplit> = map { it.toDomain() }

fun List<ExpenseSplit>.toSplitEntities(expenseId: String): List<ExpenseSplitEntity> = map { it.toEntity(expenseId) }
