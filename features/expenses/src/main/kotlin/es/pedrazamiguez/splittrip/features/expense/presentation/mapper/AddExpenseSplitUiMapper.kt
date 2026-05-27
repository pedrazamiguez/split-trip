package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.extensions.localeAwareComparator
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.EntitySplitFlattenDelegate
import java.math.BigDecimal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Handles split-related UI mapping and locale-aware display formatting for
 * the Add Expense form.
 *
 * Responsible for:
 * - Building and sorting initial [SplitUiModel] lists
 * - Resolving member display names
 * - Mapping splits to domain [ExpenseSplit] objects (flat and entity modes)
 * - Formatting cents as plain values or currency-symbol strings for split rows
 *
 * Extracted from [AddExpenseUiMapper] to keep class function count within the
 * configured Detekt threshold.
 */
class AddExpenseSplitUiMapper(
    private val localeProvider: LocaleProvider,
    private val formattingHelper: FormattingHelper,
    private val splitPreviewService: SplitPreviewService,
    private val entitySplitFlattenDelegate: EntitySplitFlattenDelegate
) {

    /**
     * Builds initial split UI models for all group members.
     */
    fun buildInitialSplits(
        memberIds: List<String>,
        shares: List<ExpenseSplit>,
        memberProfiles: Map<String, User> = emptyMap()
    ): ImmutableList<SplitUiModel> =
        memberIds.map { userId ->
            val share = shares.find { it.userId == userId }
            val amountCents = share?.amountCents ?: 0L
            SplitUiModel(
                userId = userId,
                displayName = resolveDisplayName(userId, memberProfiles),
                amountCents = amountCents,
                formattedAmount = formattingHelper.formatCentsValue(amountCents),
                amountInput = formattingHelper.formatCentsValue(amountCents),
                percentageInput = share?.percentage?.toPlainString() ?: ""
            )
        }.sortedWith(
            localeAwareComparator(localeProvider.getCurrentLocale()) { it.displayName }
        ).toImmutableList()

    /**
     * Resolves a userId to a human-readable display name using the
     * fallback hierarchy: displayName → email → raw userId.
     */
    fun resolveDisplayName(userId: String, memberProfiles: Map<String, User>): String {
        val user = memberProfiles[userId] ?: return userId
        return user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email.takeIf { it.isNotBlank() }
            ?: userId
    }

    /**
     * Maps flat split UI models to domain [ExpenseSplit] list.
     */
    fun mapSplitsToDomain(splits: List<SplitUiModel>, splitType: SplitType): List<ExpenseSplit> =
        splits.filter { !it.isExcluded }.map { uiModel ->
            ExpenseSplit(
                userId = uiModel.userId,
                amountCents = uiModel.amountCents,
                percentage = if (splitType == SplitType.PERCENT) {
                    parseLocaleAwareDecimal(uiModel.percentageInput)
                } else {
                    null
                },
                subunitId = uiModel.subunitId
            )
        }

    /**
     * Flattens entity-level splits into per-user [ExpenseSplit] entries for domain mapping.
     *
     * In subunit mode, entity rows contain nested member rows. This method extracts
     * all member rows from subunit entities and includes solo entity rows directly,
     * producing the flat list needed for storage.
     *
     * When [splitType] is PERCENT, effective per-user percentages are computed using
     * DOWN rounding + remainder distribution so the total sums to exactly 100.00.
     */
    fun mapEntitySplitsToDomain(
        entitySplits: List<SplitUiModel>,
        splitType: SplitType
    ): List<ExpenseSplit> {
        val result = entitySplitFlattenDelegate.flattenEntities(entitySplits, splitType)
        return entitySplitFlattenDelegate.redistributePercentagesIfNeeded(result, splitType)
    }

    /**
     * Maps domain splits back into SplitUiModels for edit mode support.
     */
    fun mapDomainToSplits(
        memberIds: List<String>,
        shares: List<ExpenseSplit>,
        memberProfiles: Map<String, User> = emptyMap()
    ): ImmutableList<SplitUiModel> =
        memberIds.map { userId ->
            val share = shares.find { it.userId == userId }
            val isExcluded = share == null
            val amountCents = share?.amountCents ?: 0L
            SplitUiModel(
                userId = userId,
                displayName = resolveDisplayName(userId, memberProfiles),
                amountCents = amountCents,
                formattedAmount = formattingHelper.formatCentsValue(amountCents),
                amountInput = formattingHelper.formatCentsValue(amountCents),
                percentageInput = share?.percentage?.toPlainString() ?: "",
                isExcluded = isExcluded
            )
        }.sortedWith(
            localeAwareComparator(localeProvider.getCurrentLocale()) { it.displayName }
        ).toImmutableList()

    /**
     * Maps domain splits back into entity-level SplitUiModels for edit mode support.
     */
    fun buildEntitySplitsFromDomain(
        memberIds: List<String>,
        subunits: List<Subunit>,
        shares: List<ExpenseSplit>,
        availableSplitTypes: List<SplitTypeUiModel>,
        memberProfiles: Map<String, User> = emptyMap()
    ): ImmutableList<SplitUiModel> {
        val subunitMemberIds = subunits.flatMap { it.memberIds }.toSet()
        val soloMemberIds = memberIds.filter { it !in subunitMemberIds }
        val defaultSplitType = availableSplitTypes.find { it.id == SplitType.EQUAL.name }

        val entityRows = mutableListOf<SplitUiModel>()

        entityRows.addAll(buildSoloMemberRows(soloMemberIds, shares, memberProfiles))
        entityRows.addAll(buildSubunitRows(subunits, shares, availableSplitTypes, defaultSplitType, memberProfiles))

        val localeComparator =
            localeAwareComparator(localeProvider.getCurrentLocale()) { model: SplitUiModel -> model.displayName }
        return entityRows.sortedWith { a, b ->
            val firstCompare = a.entityMembers.isNotEmpty().compareTo(b.entityMembers.isNotEmpty())
            if (firstCompare != 0) firstCompare else localeComparator.compare(a, b)
        }.toImmutableList()
    }

    private fun buildSoloMemberRows(
        soloMemberIds: List<String>,
        shares: List<ExpenseSplit>,
        memberProfiles: Map<String, User>
    ): List<SplitUiModel> {
        return soloMemberIds.map { userId ->
            val share = shares.find { it.userId == userId && it.subunitId == null }
            val isExcluded = share == null
            val amountCents = share?.amountCents ?: 0L
            SplitUiModel(
                userId = userId,
                displayName = resolveDisplayName(userId, memberProfiles),
                isEntityRow = true,
                amountCents = amountCents,
                formattedAmount = formattingHelper.formatCentsValue(amountCents),
                amountInput = formattingHelper.formatCentsValue(amountCents),
                percentageInput = share?.percentage?.toPlainString() ?: "",
                isExcluded = isExcluded
            )
        }
    }

    private fun buildSubunitRows(
        subunits: List<Subunit>,
        shares: List<ExpenseSplit>,
        availableSplitTypes: List<SplitTypeUiModel>,
        defaultSplitType: SplitTypeUiModel?,
        memberProfiles: Map<String, User>
    ): List<SplitUiModel> {
        return subunits.map { subunit ->
            val subunitShares = shares.filter { it.subunitId == subunit.id }
            val isSubunitExcluded = subunitShares.isEmpty()
            val subunitTotalCents = subunitShares.sumOf { it.amountCents }

            val subunitSplitTypeDomain = subunitShares.firstOrNull()?.splitType ?: SplitType.EQUAL
            val subunitSplitType = availableSplitTypes.find { it.id == subunitSplitTypeDomain.name } ?: defaultSplitType

            val memberRows = subunit.memberIds.map { memberId ->
                val share = subunitShares.find { it.userId == memberId }
                val isMemberExcluded = share == null
                val amountCents = share?.amountCents ?: 0L
                SplitUiModel(
                    userId = memberId,
                    displayName = resolveDisplayName(memberId, memberProfiles),
                    subunitId = subunit.id,
                    amountCents = amountCents,
                    formattedAmount = formattingHelper.formatCentsValue(amountCents),
                    amountInput = formattingHelper.formatCentsValue(amountCents),
                    percentageInput = share?.percentage?.toPlainString() ?: "",
                    isExcluded = isMemberExcluded
                )
            }.sortedWith(
                localeAwareComparator(localeProvider.getCurrentLocale()) { model -> model.displayName }
            ).toImmutableList()

            SplitUiModel(
                userId = subunit.id,
                displayName = subunit.name,
                isEntityRow = true,
                amountCents = subunitTotalCents,
                formattedAmount = formattingHelper.formatCentsValue(subunitTotalCents),
                amountInput = formattingHelper.formatCentsValue(subunitTotalCents),
                percentageInput = "",
                isExcluded = isSubunitExcluded,
                entityMembers = memberRows,
                entitySplitType = subunitSplitType
            )
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private fun parseLocaleAwareDecimal(input: String): BigDecimal? =
        splitPreviewService.parseToDecimalOrNull(input)
}
