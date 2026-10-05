package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.domain.service.split.SubunitAwareSplitService
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import java.math.BigDecimal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

/**
 * Encapsulates intra-subunit (Level 2) split recalculation logic.
 *
 * Given an entity-level [SplitUiModel] with nested member rows, this delegate
 * recalculates each member's share based on the entity's split type (EQUAL,
 * EXACT, PERCENT) and the subunit's configured `memberShares` weights.
 *
 * This is a **stateless** delegate — it receives all required context as
 * parameters and returns a modified [SplitUiModel] copy.
 */
class IntraSubunitSplitDelegate(
    private val splitCalculatorFactory: ExpenseSplitCalculatorFactory,
    private val splitPreviewService: SplitPreviewService,
    private val subunitAwareSplitService: SubunitAwareSplitService,
    private val remainderDistributionService: RemainderDistributionService,
    private val formattingHelper: FormattingHelper
) {

    /**
     * Recalculates the member splits within a subunit entity row based on
     * the entity's current [SplitUiModel.amountCents] and [SplitUiModel.entitySplitType].
     *
     * @param entity         The entity-level row containing nested member rows.
     * @param currencyCode   The currency code for formatting.
     * @param groupSubunits  All subunits in the group (used to look up memberShares).
     * @param decimalDigits  Decimal digits for the selected currency.
     * @return The entity with updated [SplitUiModel.entityMembers].
     */
    fun recalculate(
        entity: SplitUiModel,
        currencyCode: String,
        groupSubunits: List<Subunit>,
        decimalDigits: Int
    ): SplitUiModel {
        if (entity.entityMembers.isEmpty()) return entity

        val subunitTotalCents = entity.amountCents
        val intraType = entity.entitySplitType?.let { SplitType.fromString(it.id) } ?: SplitType.EQUAL
        val memberIds = entity.entityMembers.map { it.userId }

        if (subunitTotalCents <= 0 || memberIds.isEmpty()) return entity

        val updatedMembers = when (intraType) {
            SplitType.EQUAL -> recalculateEqual(entity, subunitTotalCents, memberIds, currencyCode)
            SplitType.EXACT -> recalculateExact(
                entity,
                subunitTotalCents,
                memberIds,
                currencyCode,
                decimalDigits,
                groupSubunits
            )
            SplitType.PERCENT -> recalculatePercent(
                entity,
                subunitTotalCents,
                memberIds,
                currencyCode,
                groupSubunits
            )
        }

        return entity.copy(entityMembers = updatedMembers)
    }

    /**
     * Parses the source amount string to cents using the currency's decimal places.
     */
    fun parseSourceAmountToCents(sourceAmount: String, decimalDigits: Int): Long {
        return splitPreviewService.parseAmountToCents(sourceAmount.trim(), decimalDigits)
    }

    // ── EQUAL ───────────────────────────────────────────────────────────

    /**
     * EQUAL intra-subunit recalculation: strictly divides equally (1/N)
     * across active members via the calculator factory.
     */
    internal fun recalculateEqual(
        entity: SplitUiModel,
        subunitTotalCents: Long,
        memberIds: List<String>,
        currencyCode: String
    ): ImmutableList<SplitUiModel> {
        return try {
            val calculator = splitCalculatorFactory.create(SplitType.EQUAL)
            val shares = calculator.calculateShares(subunitTotalCents, memberIds)
                .associateBy { it.userId }
            entity.entityMembers.map { member ->
                val share = shares[member.userId]
                if (share != null) {
                    member.copy(
                        amountCents = share.amountCents,
                        formattedAmount = formattingHelper.formatCentsWithCurrency(
                            share.amountCents,
                            currencyCode
                        )
                    )
                } else {
                    member
                }
            }.toImmutableList()
        } catch (e: Exception) {
            Timber.w(e, "Failed to recalculate equal intra-subunit shares for entity: %s", entity.userId)
            entity.entityMembers
        }
    }

    // ── EXACT ───────────────────────────────────────────────────────────

    /**
     * EXACT intra-subunit recalculation: pre-fills amounts based on configured
     * memberShares when available, falling back to even distribution.
     */
    internal fun recalculateExact(
        entity: SplitUiModel,
        subunitTotalCents: Long,
        memberIds: List<String>,
        currencyCode: String,
        decimalDigits: Int,
        groupSubunits: List<Subunit>
    ): ImmutableList<SplitUiModel> {
        val subunit = groupSubunits.find { it.id == entity.userId }
        val memberShares = subunit?.memberShares ?: emptyMap()

        return try {
            if (memberShares.isNotEmpty() && memberShares.values.any { it > BigDecimal.ZERO }) {
                val distributed = subunitAwareSplitService.distributeByMemberShares(
                    memberIds = memberIds,
                    totalCents = subunitTotalCents,
                    memberShares = memberShares
                )
                entity.entityMembers.map { member ->
                    val share = memberShares[member.userId]
                    val cents = distributed[member.userId]
                    if (share != null && cents != null) {
                        member.copy(
                            amountCents = cents,
                            amountInput = formattingHelper.formatCentsValue(cents, decimalDigits),
                            formattedAmount = formattingHelper.formatCentsWithCurrency(cents, currencyCode)
                        )
                    } else {
                        member
                    }
                }.toImmutableList()
            } else {
                val calculator = splitCalculatorFactory.create(SplitType.EQUAL)
                val shares = calculator.calculateShares(subunitTotalCents, memberIds)
                    .associateBy { it.userId }
                entity.entityMembers.map { member ->
                    val share = shares[member.userId]
                    if (share != null) {
                        member.copy(
                            amountCents = share.amountCents,
                            amountInput = formattingHelper.formatCentsValue(
                                share.amountCents,
                                decimalDigits
                            ),
                            formattedAmount = formattingHelper.formatCentsWithCurrency(
                                share.amountCents,
                                currencyCode
                            )
                        )
                    } else {
                        member
                    }
                }.toImmutableList()
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to recalculate exact intra-subunit shares for entity: %s", entity.userId)
            entity.entityMembers
        }
    }

    // ── PERCENT ─────────────────────────────────────────────────────────

    /**
     * PERCENT intra-subunit recalculation: pre-fills percentages based on configured
     * memberShares when available, falling back to even percentage distribution.
     */
    internal fun recalculatePercent(
        entity: SplitUiModel,
        subunitTotalCents: Long,
        memberIds: List<String>,
        currencyCode: String,
        groupSubunits: List<Subunit>
    ): ImmutableList<SplitUiModel> {
        val subunit = groupSubunits.find { it.id == entity.userId }
        val memberShares = subunit?.memberShares ?: emptyMap()

        return try {
            if (memberShares.isNotEmpty() && memberShares.values.any { it > BigDecimal.ZERO }) {
                recalculatePercentWithMemberShares(
                    entity = entity,
                    subunitTotalCents = subunitTotalCents,
                    memberIds = memberIds,
                    currencyCode = currencyCode,
                    memberShares = memberShares
                )
            } else {
                recalculatePercentEvenly(
                    entity = entity,
                    subunitTotalCents = subunitTotalCents,
                    memberIds = memberIds,
                    currencyCode = currencyCode
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to recalculate percent intra-subunit shares for entity: %s", entity.userId)
            entity.entityMembers
        }
    }

    private fun recalculatePercentWithMemberShares(
        entity: SplitUiModel,
        subunitTotalCents: Long,
        memberIds: List<String>,
        currencyCode: String,
        memberShares: Map<String, BigDecimal>
    ): ImmutableList<SplitUiModel> {
        val distributed = subunitAwareSplitService.distributeByMemberShares(
            memberIds = memberIds,
            totalCents = subunitTotalCents,
            memberShares = memberShares
        )
        return entity.entityMembers.map { member ->
            val share = memberShares[member.userId]
            if (share != null) {
                val pct = share.multiply(HUNDRED)
                val amountCents = distributed[member.userId] ?: 0L
                member.copy(
                    percentageInput = formattingHelper.formatPercentageForDisplay(pct),
                    amountCents = amountCents,
                    formattedAmount = if (subunitTotalCents > 0) {
                        formattingHelper.formatCentsWithCurrency(amountCents, currencyCode)
                    } else {
                        ""
                    }
                )
            } else {
                member
            }
        }.toImmutableList()
    }

    private fun recalculatePercentEvenly(
        entity: SplitUiModel,
        subunitTotalCents: Long,
        memberIds: List<String>,
        currencyCode: String
    ): ImmutableList<SplitUiModel> {
        val shares = splitPreviewService.distributePercentagesEvenly(
            subunitTotalCents,
            memberIds
        ).associateBy { it.userId }
        return entity.entityMembers.map { member ->
            val share = shares[member.userId]
            if (share != null) {
                val pct = share.percentage ?: BigDecimal.ZERO
                member.copy(
                    percentageInput = formattingHelper.formatPercentageForDisplay(pct),
                    amountCents = share.amountCents,
                    formattedAmount = if (subunitTotalCents > 0) {
                        formattingHelper.formatCentsWithCurrency(
                            share.amountCents,
                            currencyCode
                        )
                    } else {
                        ""
                    }
                )
            } else {
                member
            }
        }.toImmutableList()
    }

    // ── Entity-Level Distribution ──────────────────────────────────────

    /**
     * Distributes [sourceAmountCents] across entity rows using the specified [splitType].
     * Each entity row is then passed through [recalculate] for intra-subunit member splits.
     *
     * @return The updated entity splits list, or `null` if the operation is a no-op or fails.
     */
    fun distributeEntitySplits(
        entitySplits: ImmutableList<SplitUiModel>,
        splitType: SplitType,
        sourceAmountCents: Long,
        activeEntityIds: List<String>,
        currencyCode: String,
        groupSubunits: List<Subunit>,
        decimalDigits: Int
    ): ImmutableList<SplitUiModel>? {
        if (activeEntityIds.isEmpty()) return null
        return when (splitType) {
            SplitType.EQUAL -> distributeEqualEntities(
                entitySplits,
                sourceAmountCents,
                activeEntityIds,
                currencyCode,
                groupSubunits,
                decimalDigits
            )
            SplitType.EXACT -> distributeExactEntities(
                entitySplits,
                sourceAmountCents,
                activeEntityIds,
                currencyCode,
                groupSubunits,
                decimalDigits
            )
            SplitType.PERCENT -> distributePercentEntities(
                entitySplits,
                sourceAmountCents,
                activeEntityIds,
                currencyCode,
                groupSubunits,
                decimalDigits
            )
        }
    }

    private fun calculateHeadcountWeightedAmounts(
        activeEntities: List<SplitUiModel>,
        sourceAmountCents: Long
    ): Map<String, Long> {
        if (activeEntities.isEmpty() || sourceAmountCents <= 0) return emptyMap()
        val weights = activeEntities.map { BigDecimal(getEntityHeadcount(it)) }
        val allocatedAmounts = remainderDistributionService.distributeByWeights(sourceAmountCents, weights)
        return activeEntities.mapIndexed { index, entity ->
            entity.userId to allocatedAmounts[index]
        }.toMap()
    }

    private fun distributeEqualEntities(
        entitySplits: ImmutableList<SplitUiModel>,
        sourceAmountCents: Long,
        activeEntityIds: List<String>,
        currencyCode: String,
        groupSubunits: List<Subunit>,
        decimalDigits: Int
    ): ImmutableList<SplitUiModel>? {
        if (sourceAmountCents <= 0 || activeEntityIds.isEmpty()) return null
        return try {
            val activeEntities = entitySplits.filter { !it.isExcluded && it.userId in activeEntityIds }
            if (activeEntities.isEmpty()) return null

            val sharesByEntityId = calculateHeadcountWeightedAmounts(activeEntities, sourceAmountCents)

            entitySplits.map { entity ->
                val shareAmount = sharesByEntityId[entity.userId]
                if (shareAmount != null && !entity.isExcluded) {
                    val updated = entity.copy(
                        amountCents = shareAmount,
                        formattedAmount = formattingHelper.formatCentsWithCurrency(shareAmount, currencyCode)
                    )
                    recalculate(updated, currencyCode, groupSubunits, decimalDigits)
                } else if (entity.isExcluded) {
                    zeroOutEntity(entity)
                } else {
                    entity
                }
            }.toImmutableList()
        } catch (e: Exception) {
            Timber.w(e, "Failed to distribute equal entity splits for amountCents: %d", sourceAmountCents)
            null
        }
    }

    private fun distributeExactEntities(
        entitySplits: ImmutableList<SplitUiModel>,
        sourceAmountCents: Long,
        activeEntityIds: List<String>,
        currencyCode: String,
        groupSubunits: List<Subunit>,
        decimalDigits: Int
    ): ImmutableList<SplitUiModel>? {
        if (sourceAmountCents <= 0 || activeEntityIds.isEmpty()) return null
        return try {
            val activeEntities = entitySplits.filter { !it.isExcluded && it.userId in activeEntityIds }
            if (activeEntities.isEmpty()) return null

            val sharesByEntityId = calculateHeadcountWeightedAmounts(activeEntities, sourceAmountCents)

            entitySplits.map { entity ->
                val shareAmount = sharesByEntityId[entity.userId]
                if (shareAmount != null && !entity.isExcluded) {
                    val updated = entity.copy(
                        amountCents = shareAmount,
                        amountInput = formattingHelper.formatCentsValue(shareAmount, decimalDigits),
                        formattedAmount = formattingHelper.formatCentsWithCurrency(shareAmount, currencyCode)
                    )
                    recalculate(updated, currencyCode, groupSubunits, decimalDigits)
                } else if (entity.isExcluded) {
                    zeroOutEntity(entity)
                } else {
                    entity
                }
            }.toImmutableList()
        } catch (e: Exception) {
            Timber.w(e, "Failed to distribute exact entity splits for amountCents: %d", sourceAmountCents)
            null
        }
    }

    private fun distributePercentEntities(
        entitySplits: ImmutableList<SplitUiModel>,
        sourceAmountCents: Long,
        activeEntityIds: List<String>,
        currencyCode: String,
        groupSubunits: List<Subunit>,
        decimalDigits: Int
    ): ImmutableList<SplitUiModel> {
        val activeEntities = entitySplits.filter { !it.isExcluded && it.userId in activeEntityIds }
        if (activeEntities.isEmpty()) {
            return entitySplits.map { if (it.isExcluded) zeroOutEntity(it) else it }.toImmutableList()
        }

        val headcounts = activeEntities.map { getEntityHeadcount(it).toLong() }
        val percentages = remainderDistributionService.distributePercentages(
            remainingPercentage = BigDecimal("100"),
            amounts = headcounts,
            totalCents = headcounts.sum()
        )
        val amounts = computePercentAmounts(percentages, sourceAmountCents)

        val sharesByEntityId = activeEntities.mapIndexed { index, entity ->
            entity.userId to Pair(percentages[index], amounts[index])
        }.toMap()

        return entitySplits.map { entity ->
            val share = sharesByEntityId[entity.userId]
            when {
                !entity.isExcluded && share != null -> applyPercentShareToEntity(
                    entity = entity,
                    pct = share.first,
                    amountCents = share.second,
                    sourceAmountCents = sourceAmountCents,
                    currencyCode = currencyCode,
                    groupSubunits = groupSubunits,
                    decimalDigits = decimalDigits
                )
                entity.isExcluded -> zeroOutEntity(entity)
                else -> entity
            }
        }.toImmutableList()
    }

    private fun computePercentAmounts(
        percentages: List<BigDecimal>,
        sourceAmountCents: Long
    ): List<Long> {
        val rawAmounts = percentages.map { pct ->
            splitPreviewService.calculateAmountFromPercentage(pct, sourceAmountCents)
        }
        var remainder = if (sourceAmountCents > 0) sourceAmountCents - rawAmounts.sum() else 0L
        return rawAmounts.map { amount ->
            if (remainder > 0) {
                remainder--
                amount + 1L
            } else {
                amount
            }
        }
    }

    private fun applyPercentShareToEntity(
        entity: SplitUiModel,
        pct: BigDecimal,
        amountCents: Long,
        sourceAmountCents: Long,
        currencyCode: String,
        groupSubunits: List<Subunit>,
        decimalDigits: Int
    ): SplitUiModel {
        val updated = entity.copy(
            percentageInput = formattingHelper.formatPercentageForDisplay(pct),
            amountCents = amountCents,
            formattedAmount = if (sourceAmountCents > 0) {
                formattingHelper.formatCentsWithCurrency(amountCents, currencyCode)
            } else {
                ""
            }
        )
        return recalculate(updated, currencyCode, groupSubunits, decimalDigits)
    }

    companion object {
        private val HUNDRED = BigDecimal("100")
    }
}

private fun getEntityHeadcount(entity: SplitUiModel): Int {
    return if (entity.entityMembers.isEmpty()) {
        1
    } else {
        entity.entityMembers.count { !it.isExcluded }.coerceAtLeast(1)
    }
}

private fun zeroOutEntity(entity: SplitUiModel): SplitUiModel {
    val zeroedMembers = entity.entityMembers.map { member ->
        member.copy(
            amountCents = 0L,
            amountInput = "",
            percentageInput = "",
            formattedAmount = ""
        )
    }.toImmutableList()
    return entity.copy(
        amountCents = 0L,
        amountInput = "",
        percentageInput = "",
        formattedAmount = "",
        entityMembers = zeroedMembers
    )
}
