package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.extensions.localeAwareComparator
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
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
    private val entitySplitFlattenDelegate: EntitySplitFlattenDelegate,
    private val userUiMapper: UserUiMapper
) {

    /**
     * Builds initial split UI models for all group members.
     */
    fun buildInitialSplits(
        memberIds: List<String>,
        shares: List<ExpenseSplit>,
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = null
    ): ImmutableList<SplitUiModel> {
        val localeComparator =
            localeAwareComparator<SplitUiModel>(localeProvider.getCurrentLocale()) { it.displayName }
        return memberIds.map { userId ->
            val share = shares.find { it.userId == userId }
            val amountCents = share?.amountCents ?: 0L
            SplitUiModel(
                userId = userId,
                displayName = resolveDisplayName(userId, memberProfiles, currentUserId),
                amountCents = amountCents,
                formattedAmount = formattingHelper.formatCentsValue(amountCents),
                amountInput = formattingHelper.formatCentsValue(amountCents),
                percentageInput = share?.percentage?.toPlainString() ?: ""
            )
        }.sortedWith(
            compareByDescending<SplitUiModel> { it.userId == currentUserId }
                .thenComparing(localeComparator)
        ).toImmutableList()
    }

    /**
     * Resolves a userId to a human-readable display name using the
     * fallback hierarchy: displayName → email → raw userId.
     *
     * When [currentUserId] matches, returns the NOMINATIVE self-identification pronoun ("You" / "Tú").
     */
    fun resolveDisplayName(userId: String, memberProfiles: Map<String, User>, currentUserId: String? = null): String {
        return userUiMapper.mapToDisplayName(
            user = memberProfiles[userId],
            fallbackUserId = userId,
            currentUserId = currentUserId,
            selfIdentificationContext = if (currentUserId != null) SelfIdentificationContextEnum.NOMINATIVE else null
        )
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
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = null
    ): ImmutableList<SplitUiModel> {
        val localeComparator =
            localeAwareComparator<SplitUiModel>(localeProvider.getCurrentLocale()) { it.displayName }
        return memberIds.map { userId ->
            val share = shares.find { it.userId == userId }
            val isExcluded = share == null
            val amountCents = share?.amountCents ?: 0L
            SplitUiModel(
                userId = userId,
                displayName = resolveDisplayName(userId, memberProfiles, currentUserId),
                amountCents = amountCents,
                formattedAmount = formattingHelper.formatCentsValue(amountCents),
                amountInput = formattingHelper.formatCentsValue(amountCents),
                percentageInput = share?.percentage?.toPlainString() ?: "",
                isExcluded = isExcluded
            )
        }.sortedWith(
            compareByDescending<SplitUiModel> { it.userId == currentUserId }
                .thenComparing(localeComparator)
        ).toImmutableList()
    }

    /**
     * Maps domain splits back into entity-level SplitUiModels for edit mode support.
     */
    fun buildEntitySplitsFromDomain(
        memberIds: List<String>,
        subunits: List<Subunit>,
        shares: List<ExpenseSplit>,
        availableSplitTypes: List<SplitTypeUiModel>,
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = null
    ): ImmutableList<SplitUiModel> {
        val subunitMemberIds = subunits.flatMap { it.memberIds }.toSet()
        val soloMemberIds = memberIds.filter { it !in subunitMemberIds }
        val defaultSplitType = availableSplitTypes.find { it.id == SplitType.EQUAL.name }

        val entityRows = mutableListOf<SplitUiModel>()

        entityRows.addAll(buildSoloMemberRows(soloMemberIds, shares, memberProfiles, currentUserId))
        entityRows.addAll(
            buildSubunitRows(subunits, shares, availableSplitTypes, defaultSplitType, memberProfiles, currentUserId)
        )

        return sortEntityRows(entityRows, currentUserId)
    }

    /**
     * Sorts entity rows for subunit split mode:
     * - If [currentUserId] belongs to a subunit, that subunit is placed first.
     * - If [currentUserId] is a solo member, their solo row is placed first.
     * - Other subunits / solo members follow with consistent grouping and alphabetical order.
     */
    fun sortEntityRows(
        entityRows: List<SplitUiModel>,
        currentUserId: String?
    ): ImmutableList<SplitUiModel> {
        val isUserInSubunit = currentUserId != null &&
            entityRows.any { entity ->
                entity.entityMembers.any { it.userId == currentUserId }
            }
        val localeComparator =
            localeAwareComparator<SplitUiModel>(localeProvider.getCurrentLocale()) { it.displayName }
        return entityRows.sortedWith(
            compareBy<SplitUiModel> { entity ->
                getEntityPriority(entity, currentUserId, isUserInSubunit)
            }.thenComparing(localeComparator)
        ).toImmutableList()
    }

    /**
     * Sorts subunit member rows, pinning [currentUserId] at index 0 followed by
     * remaining members sorted alphabetically by display name.
     */
    fun sortSubunitMembers(
        members: List<SplitUiModel>,
        currentUserId: String?
    ): ImmutableList<SplitUiModel> {
        val localeComparator =
            localeAwareComparator<SplitUiModel>(localeProvider.getCurrentLocale()) { it.displayName }
        return members.sortedWith(
            compareByDescending<SplitUiModel> { it.userId == currentUserId }
                .thenComparing(localeComparator)
        ).toImmutableList()
    }

    internal fun getEntityPriority(
        entity: SplitUiModel,
        currentUserId: String?,
        isUserInSubunit: Boolean
    ): Int {
        val isUserEntity = currentUserId != null &&
            (entity.userId == currentUserId || entity.entityMembers.any { it.userId == currentUserId })
        if (isUserEntity) return 0

        return if (isUserInSubunit) {
            if (entity.entityMembers.isNotEmpty()) 1 else 2
        } else {
            if (entity.entityMembers.isEmpty()) 1 else 2
        }
    }

    private fun buildSoloMemberRows(
        soloMemberIds: List<String>,
        shares: List<ExpenseSplit>,
        memberProfiles: Map<String, User>,
        currentUserId: String? = null
    ): List<SplitUiModel> {
        return soloMemberIds.map { userId ->
            val share = shares.find { it.userId == userId && it.subunitId == null }
            val isExcluded = share == null
            val amountCents = share?.amountCents ?: 0L
            SplitUiModel(
                userId = userId,
                displayName = resolveDisplayName(userId, memberProfiles, currentUserId),
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
        memberProfiles: Map<String, User>,
        currentUserId: String? = null
    ): List<SplitUiModel> {
        return subunits.map { subunit ->
            val subunitShares = shares.filter { it.subunitId == subunit.id }
            val isSubunitExcluded = subunitShares.isEmpty()
            val subunitTotalCents = subunitShares.sumOf { it.amountCents }

            val subunitSplitTypeDomain = subunitShares.firstOrNull()?.splitType ?: SplitType.EQUAL
            val subunitSplitType = availableSplitTypes.find { it.id == subunitSplitTypeDomain.name } ?: defaultSplitType

            val rawMemberRows = subunit.memberIds.map { memberId ->
                val share = subunitShares.find { it.userId == memberId }
                val isMemberExcluded = share == null
                val amountCents = share?.amountCents ?: 0L
                SplitUiModel(
                    userId = memberId,
                    displayName = resolveDisplayName(memberId, memberProfiles, currentUserId),
                    subunitId = subunit.id,
                    amountCents = amountCents,
                    formattedAmount = formattingHelper.formatCentsValue(amountCents),
                    amountInput = formattingHelper.formatCentsValue(amountCents),
                    percentageInput = share?.percentage?.toPlainString() ?: "",
                    isExcluded = isMemberExcluded
                )
            }
            val memberRows = sortSubunitMembers(rawMemberRows, currentUserId)

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
