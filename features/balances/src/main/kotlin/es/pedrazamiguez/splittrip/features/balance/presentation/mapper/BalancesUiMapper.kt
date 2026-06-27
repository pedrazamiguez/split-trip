package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.extensions.localeAwareComparator
import es.pedrazamiguez.splittrip.core.common.extensions.toEpochMillisUtc
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatForDisplay
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.CurrencyAmount
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ActivityItemUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CurrencyBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.GroupPocketBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceCashContext
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceUiModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class BalancesUiMapper(
    private val localeProvider: LocaleProvider,
    private val resourceProvider: ResourceProvider,
    private val userUiMapper: UserUiMapper
) {

    companion object {
        /** Displayed instead of a formatted amount when the value is not applicable. */
        internal const val EM_DASH = "\u2014"
    }

    fun mapBalance(balance: GroupPocketBalance, groupName: String): GroupPocketBalanceUiModel {
        val locale = localeProvider.getCurrentLocale()
        val cashBalanceUiModels = balance.cashBalances.entries
            .sortedBy { (currency, _) -> currency }
            .map { (currency, amountCents) ->
                val equivalent = balance.cashEquivalents[currency]
                CashBalanceUiModel(
                    currency = currency,
                    formattedAmount = formatCurrencyAmount(amountCents, currency, locale),
                    formattedEquivalent = if (currency != balance.currency && equivalent != null && equivalent > 0) {
                        formatCurrencyAmount(equivalent, balance.currency, locale)
                    } else {
                        ""
                    }
                )
            }.toImmutableList()

        return GroupPocketBalanceUiModel(
            groupName = groupName,
            formattedBalance = formatCurrencyAmount(balance.virtualBalance, balance.currency, locale),
            formattedTotalContributed = formatCurrencyAmount(
                balance.totalContributions,
                balance.currency,
                locale
            ),
            formattedTotalSpent = formatCurrencyAmount(
                balance.totalExpenses,
                balance.currency,
                locale
            ),
            currency = balance.currency,
            cashBalances = cashBalanceUiModels,
            formattedTotalCashEquivalent = if (balance.totalCashEquivalent > 0) {
                formatCurrencyAmount(balance.totalCashEquivalent, balance.currency, locale)
            } else {
                ""
            },
            formattedAvailableBalance = if (balance.scheduledHoldAmount > 0) {
                val available = balance.virtualBalance - balance.scheduledHoldAmount
                formatCurrencyAmount(available, balance.currency, locale)
            } else {
                null
            },
            formattedTotalExtras = if (balance.totalExtras > 0) {
                formatCurrencyAmount(balance.totalExtras, balance.currency, locale)
            } else {
                null
            }
        )
    }

    fun mapContributions(
        contributions: List<Contribution>,
        currentUserId: String?,
        memberProfiles: Map<String, User> = emptyMap(),
        subunits: Map<String, Subunit> = emptyMap()
    ): ImmutableList<ContributionUiModel> {
        val locale = localeProvider.getCurrentLocale()
        return contributions.map { contribution ->
            val isSubunit = contribution.contributionScope == PayerType.SUBUNIT
            val isPersonal = contribution.contributionScope == PayerType.USER
            val isGroup = contribution.contributionScope == PayerType.GROUP
            val scopeLabel = when {
                isSubunit -> contribution.subunitId?.let { subunits[it]?.name }
                isPersonal -> resourceProvider.getString(R.string.balances_contribution_scope_personal)
                isGroup -> resourceProvider.getString(R.string.balances_contribution_scope_group)
                else -> null
            }
            val createdByDisplayName = resolveCreatedByDisplayName(
                createdBy = contribution.createdBy,
                targetUserId = contribution.userId,
                memberProfiles = memberProfiles
            )
            ContributionUiModel(
                id = contribution.id,
                displayName = resolveDisplayName(contribution.userId, memberProfiles),
                isCurrentUser = contribution.userId == currentUserId,
                formattedAmount = formatCurrencyAmount(
                    contribution.amount,
                    contribution.currency,
                    locale
                ),
                dateText = contribution.createdAt?.formatShortDate(locale) ?: "",
                scopeLabel = scopeLabel,
                isSubunitContribution = isSubunit,
                isPersonalContribution = isPersonal,
                isGroupContribution = isGroup,
                createdByDisplayName = createdByDisplayName,
                isLinkedContribution = contribution.linkedExpenseId != null,
                syncStatus = contribution.syncStatus
            )
        }.toImmutableList()
    }

    fun mapCashWithdrawals(
        withdrawals: List<CashWithdrawal>,
        groupCurrency: String,
        currentUserId: String?,
        memberProfiles: Map<String, User> = emptyMap(),
        subunits: Map<String, Subunit> = emptyMap()
    ): ImmutableList<CashWithdrawalUiModel> {
        val locale = localeProvider.getCurrentLocale()
        return withdrawals.map { withdrawal ->
            val isForeign = withdrawal.currency != groupCurrency
            val isSubunit = withdrawal.withdrawalScope == PayerType.SUBUNIT
            val isPersonal = withdrawal.withdrawalScope == PayerType.USER
            val isGroup = withdrawal.withdrawalScope == PayerType.GROUP
            val scopeLabel = when {
                isSubunit -> withdrawal.subunitId?.let { subunits[it]?.name }
                isPersonal -> resourceProvider.getString(R.string.balances_withdraw_cash_scope_personal)
                isGroup -> resourceProvider.getString(R.string.balances_withdraw_cash_scope_group)
                else -> null
            }
            val createdByDisplayName = resolveCreatedByDisplayName(
                createdBy = withdrawal.createdBy,
                targetUserId = withdrawal.withdrawnBy,
                memberProfiles = memberProfiles
            )
            CashWithdrawalUiModel(
                id = withdrawal.id,
                displayName = resolveDisplayName(withdrawal.withdrawnBy, memberProfiles),
                isCurrentUser = withdrawal.withdrawnBy == currentUserId,
                formattedAmount = formatCurrencyAmount(
                    withdrawal.amountWithdrawn,
                    withdrawal.currency,
                    locale
                ),
                formattedDeducted = if (isForeign) {
                    formatCurrencyAmount(
                        withdrawal.deductedBaseAmount,
                        groupCurrency,
                        locale
                    )
                } else {
                    ""
                },
                currency = withdrawal.currency,
                isForeignCurrency = isForeign,
                dateText = withdrawal.createdAt?.formatShortDate(locale) ?: "",
                scopeLabel = scopeLabel,
                isSubunitWithdrawal = isSubunit,
                isPersonalWithdrawal = isPersonal,
                isGroupWithdrawal = isGroup,
                title = withdrawal.title,
                notes = withdrawal.notes,
                createdByDisplayName = createdByDisplayName,
                syncStatus = withdrawal.syncStatus
            )
        }.toImmutableList()
    }

    /**
     * Merges contributions and cash withdrawals into a single activity list,
     * sorted by date descending (newest first).
     *
     * Reuses [mapContributions] and [mapCashWithdrawals] for UiModel construction
     * to avoid duplicating formatting/mapping logic.
     */
    fun mapActivity(
        contributions: List<Contribution>,
        withdrawals: List<CashWithdrawal>,
        groupCurrency: String,
        currentUserId: String?,
        memberProfiles: Map<String, User> = emptyMap(),
        subunits: Map<String, Subunit> = emptyMap()
    ): ImmutableList<ActivityItemUiModel> {
        // Precompute sort timestamps from domain models
        val contributionTimestampsById = contributions.associate { contribution ->
            val timestamp = contribution.createdAt?.toEpochMillisUtc() ?: 0L
            contribution.id to timestamp
        }

        val withdrawalTimestampsById = withdrawals.associate { withdrawal ->
            val timestamp = withdrawal.createdAt?.toEpochMillisUtc() ?: 0L
            withdrawal.id to timestamp
        }

        // Reuse existing mappers for UiModel construction
        val contributionUiModels = mapContributions(
            contributions = contributions,
            currentUserId = currentUserId,
            memberProfiles = memberProfiles,
            subunits = subunits
        )

        val withdrawalUiModels = mapCashWithdrawals(
            withdrawals = withdrawals,
            groupCurrency = groupCurrency,
            currentUserId = currentUserId,
            memberProfiles = memberProfiles,
            subunits = subunits
        )

        val contributionItems = contributionUiModels.map { uiModel ->
            ActivityItemUiModel.ContributionItem(
                contribution = uiModel,
                sortTimestamp = contributionTimestampsById[uiModel.id] ?: 0L
            )
        }

        val withdrawalItems = withdrawalUiModels.map { uiModel ->
            ActivityItemUiModel.CashWithdrawalItem(
                withdrawal = uiModel,
                sortTimestamp = withdrawalTimestampsById[uiModel.id] ?: 0L
            )
        }

        return (contributionItems + withdrawalItems)
            .sortedByDescending { it.sortTimestamp }
            .toImmutableList()
    }

    /**
     * Maps per-member domain balances to UI models with formatted amounts.
     * Sort order: current user first, then alphabetically by display name (resolved by mapper).
     *
     * @param groupCurrency The group's base currency code, used to determine whether
     *                      to show equivalents for per-currency breakdowns.
     * @param cashContext   Supplementary cash context (withdrawals, subunit map, member IDs)
     *                      needed to build per-member cash breakdowns. Defaults to an empty
     *                      context for screens that do not display the cash breakdown section.
     */
    fun mapMemberBalances(
        balances: List<MemberBalance>,
        currency: String,
        currentUserId: String?,
        memberProfiles: Map<String, User> = emptyMap(),
        groupCurrency: String = currency,
        cashContext: MemberBalanceCashContext = MemberBalanceCashContext()
    ): ImmutableList<MemberBalanceUiModel> {
        val locale = localeProvider.getCurrentLocale()
        val mappedBalances = balances.map { balance ->
            mapSingleMemberBalance(
                balance = balance,
                currentUserId = currentUserId,
                currency = currency,
                groupCurrency = groupCurrency,
                memberProfiles = memberProfiles,
                cashContext = cashContext,
                locale = locale
            )
        }

        val displayNameComparator = localeAwareComparator<MemberBalanceUiModel>(locale) { it.displayName }

        return mappedBalances
            .sortedWith(
                compareByDescending<MemberBalanceUiModel> { it.isCurrentUser }
                    .thenComparing(displayNameComparator)
            )
            .toImmutableList()
    }

    private fun mapSingleMemberBalance(
        balance: MemberBalance,
        currentUserId: String?,
        currency: String,
        groupCurrency: String,
        memberProfiles: Map<String, User>,
        cashContext: MemberBalanceCashContext,
        locale: Locale
    ): MemberBalanceUiModel {
        val isNegativeCash = balance.cashInHand < 0
        return MemberBalanceUiModel(
            userId = balance.userId,
            displayName = resolveDisplayName(balance.userId, memberProfiles),
            isCurrentUser = balance.userId == currentUserId,
            formattedContributed = formatCurrencyAmount(balance.contributed, currency, locale),
            formattedCashInHand = if (isNegativeCash) {
                EM_DASH
            } else {
                formatCurrencyAmount(balance.cashInHand, currency, locale)
            },
            formattedTotalSpent = formatCurrencyAmount(balance.totalSpent, currency, locale),
            formattedPocketBalance = formatCurrencyAmount(balance.pocketBalance, currency, locale),
            formattedTotalBalance = formatCurrencyAmount(balance.totalBalance, currency, locale),
            formattedCashSpent = formatCurrencyAmount(balance.cashSpent, currency, locale),
            formattedNonCashSpent = formatCurrencyAmount(balance.nonCashSpent, currency, locale),
            isPositiveBalance = balance.totalBalance >= 0,
            hasNegativeCashInHand = isNegativeCash,
            cashInHandByCurrency = if (isNegativeCash) {
                persistentListOf()
            } else {
                mapCurrencyBreakdowns(balance.cashInHandByCurrency, groupCurrency, locale)
            },
            cashSpentByCurrency = mapCurrencyBreakdowns(balance.cashSpentByCurrency, groupCurrency, locale),
            nonCashSpentByCurrency = mapCurrencyBreakdowns(balance.nonCashSpentByCurrency, groupCurrency, locale),
            cashBreakdown = if (isNegativeCash) {
                persistentListOf()
            } else {
                mapCashBreakdown(
                    userId = balance.userId,
                    withdrawals = cashContext.withdrawals,
                    subunitsMap = cashContext.subunitsMap,
                    groupMemberIds = cashContext.groupMemberIds,
                    groupCurrency = groupCurrency,
                    locale = locale
                )
            }
        )
    }

    /**
     * Builds the per-withdrawal cash breakdown for a single member.
     *
     * Computes each withdrawal's attributed share using the same scope rules as
     * [es.pedrazamiguez.splittrip.domain.usecase.balance.support.attributeRemainingByScope]:
     * - GROUP → equal share among all group members (display approximation).
     * - USER → full remaining if this member made the withdrawal, else excluded.
     * - SUBUNIT → proportional share by [Subunit.memberShares] (BigDecimal, HALF_UP).
     *
     * Items are ordered: GROUP scope → SUBUNIT scope → USER scope, then by date descending
     * within each scope group so the breakdown mirrors the pool priority used by FIFO.
     *
     * Exchange rate is omitted when the withdrawal currency equals the group currency
     * (no conversion needed, no meaningful rate to display).
     */
    private fun mapCashBreakdown(
        userId: String,
        withdrawals: List<CashWithdrawal>,
        subunitsMap: Map<String, Subunit>,
        groupMemberIds: List<String>,
        groupCurrency: String,
        locale: Locale
    ): ImmutableList<CashBreakdownUiModel> {
        val scopeOrder = mapOf(PayerType.GROUP to 0, PayerType.SUBUNIT to 1, PayerType.USER to 2)
        return withdrawals
            .sortedWith(
                compareBy<CashWithdrawal> { scopeOrder[it.withdrawalScope] ?: 3 }
                    .thenByDescending { it.createdAt }
            )
            .mapNotNull { withdrawal ->
                if (withdrawal.amountWithdrawn == 0L || withdrawal.remainingAmount <= 0L) return@mapNotNull null
                val nativeShare = computeUserNativeShare(withdrawal, userId, groupMemberIds, subunitsMap)
                if (nativeShare <= 0L) return@mapNotNull null
                buildCashBreakdownEntry(withdrawal, nativeShare, groupCurrency, locale, subunitsMap)
            }
            .toImmutableList()
    }

    /**
     * Builds a single [CashBreakdownUiModel] from an attributed native share.
     * Extracted from [mapCashBreakdown] to keep cognitive complexity within detekt limits.
     */
    private fun buildCashBreakdownEntry(
        withdrawal: CashWithdrawal,
        nativeShare: Long,
        groupCurrency: String,
        locale: Locale,
        subunitsMap: Map<String, Subunit>
    ): CashBreakdownUiModel {
        val groupEquivalent = BigDecimal(nativeShare)
            .multiply(BigDecimal(withdrawal.deductedBaseAmount))
            .divide(BigDecimal(withdrawal.amountWithdrawn), 0, RoundingMode.HALF_UP)
            .toLong()
        val isForeign = withdrawal.currency != groupCurrency
        val dateText = withdrawal.createdAt?.formatShortDate(locale) ?: ""
        val label = if (withdrawal.title.isNullOrBlank()) {
            resourceProvider.getString(R.string.balances_cash_breakdown_atm_fallback, dateText)
        } else {
            withdrawal.title ?: ""
        }
        return CashBreakdownUiModel(
            withdrawalLabel = label,
            dateText = dateText,
            formattedRate = if (isForeign) {
                // Swap order: native/group → reads as "X native-units per 1 group-unit"
                // e.g. "@ 1.1813 USD/EUR" = "1.1813 USD per EUR" (everyday math, not Forex notation)
                resourceProvider.getString(
                    R.string.balances_cash_breakdown_rate,
                    withdrawal.exchangeRate.formatForDisplay(locale, maxDecimalPlaces = 6),
                    withdrawal.currency,
                    groupCurrency
                )
            } else {
                ""
            },
            formattedNativeRemaining = formatCurrencyAmount(nativeShare, withdrawal.currency, locale),
            formattedEquivalent = if (isForeign) {
                formatCurrencyAmount(groupEquivalent, groupCurrency, locale)
            } else {
                ""
            },
            scopeLabel = resolveCashBreakdownScopeLabel(withdrawal, subunitsMap),
            isEstimatedShare = withdrawal.withdrawalScope == PayerType.GROUP,
            formattedAddOns = formatWithdrawalAddOns(withdrawal.addOns, groupCurrency, locale)
        )
    }

    /** Formats a list of withdrawal add-ons (excluding discount types) into a localized string. */
    private fun formatWithdrawalAddOns(
        addOns: List<AddOn>,
        groupCurrency: String,
        locale: Locale
    ): String {
        val nonDiscountAddOns = addOns.filter { it.type != AddOnType.DISCOUNT }
        if (nonDiscountAddOns.isEmpty()) return ""
        return nonDiscountAddOns.joinToString(separator = ", ") { addOn ->
            val description = addOn.description
            val labelText = if (!description.isNullOrBlank()) {
                description
            } else {
                when (addOn.type) {
                    AddOnType.TIP -> resourceProvider.getString(R.string.balances_cash_breakdown_add_on_tip)
                    AddOnType.FEE -> resourceProvider.getString(R.string.balances_cash_breakdown_add_on_fee)
                    AddOnType.SURCHARGE -> resourceProvider.getString(
                        R.string.balances_cash_breakdown_add_on_surcharge
                    )
                    AddOnType.DISCOUNT -> "" // Filtered out
                }
            }
            val formattedAmount = formatCurrencyAmount(addOn.groupAmountCents, groupCurrency, locale)
            resourceProvider.getString(R.string.balances_cash_breakdown_fee_tip, labelText, formattedAmount)
        }
    }

    /** Resolves the user's attributed native share (in withdrawal currency cents) for display. */
    private fun computeUserNativeShare(
        withdrawal: CashWithdrawal,
        userId: String,
        groupMemberIds: List<String>,
        subunitsMap: Map<String, Subunit>
    ): Long {
        val remaining = withdrawal.remainingAmount
        return when (withdrawal.withdrawalScope) {
            PayerType.USER -> if (withdrawal.withdrawnBy == userId) remaining else 0L
            PayerType.GROUP -> {
                if (groupMemberIds.isEmpty()) 0L else remaining / groupMemberIds.size
            }
            PayerType.SUBUNIT -> {
                val subunit = withdrawal.subunitId?.let { subunitsMap[it] }
                val share = subunit?.memberShares?.get(userId) ?: return 0L
                BigDecimal(remaining)
                    .multiply(share)
                    .setScale(0, RoundingMode.HALF_UP)
                    .toLong()
            }
        }
    }

    private fun resolveCashBreakdownScopeLabel(
        withdrawal: CashWithdrawal,
        subunitsMap: Map<String, Subunit>
    ): String = when (withdrawal.withdrawalScope) {
        PayerType.GROUP -> resourceProvider.getString(R.string.balances_cash_breakdown_group_scope)
        PayerType.USER -> resourceProvider.getString(R.string.balances_cash_breakdown_personal_scope)
        PayerType.SUBUNIT -> withdrawal.subunitId?.let { subunitsMap[it]?.name }
            ?: resourceProvider.getString(R.string.balances_cash_breakdown_unknown_subunit)
    }

    /**
     * Maps a list of [CurrencyAmount] domain models to formatted [CurrencyBreakdownUiModel]s.
     * Equivalents are only shown when the currency differs from the group currency
     * **and** [CurrencyAmount.equivalentCents] is positive; zero or negative equivalents
     * are suppressed (empty string) to avoid displaying meaningless "0.00" values.
     */
    private fun mapCurrencyBreakdowns(
        amounts: List<CurrencyAmount>,
        groupCurrency: String,
        locale: Locale
    ): ImmutableList<CurrencyBreakdownUiModel> {
        return amounts.map { ca ->
            CurrencyBreakdownUiModel(
                currency = ca.currency,
                formattedAmount = formatCurrencyAmount(ca.amountCents, ca.currency, locale),
                formattedEquivalent = if (ca.currency != groupCurrency && ca.equivalentCents > 0) {
                    formatCurrencyAmount(ca.equivalentCents, groupCurrency, locale)
                } else {
                    ""
                }
            )
        }.toImmutableList()
    }

    /**
     * Resolves a userId to a human-readable display name using the
     * fallback hierarchy: displayName → email → raw userId.
     */
    private fun resolveDisplayName(userId: String, memberProfiles: Map<String, User>): String {
        return userUiMapper.mapToDisplayName(memberProfiles[userId], userId)
    }

    /**
     * Returns the actor's display name when a record was created on behalf of another member.
     * Returns `null` when:
     * - the actor is the same as the target (no impersonation),
     * - [createdBy] is blank (legacy/migrated data),
     * - the actor's profile is missing from [memberProfiles] (avoids leaking internal IDs).
     */
    private fun resolveCreatedByDisplayName(
        createdBy: String,
        targetUserId: String,
        memberProfiles: Map<String, User>
    ): String? {
        if (createdBy.isBlank() || createdBy == targetUserId) return null
        val user = memberProfiles[createdBy] ?: return null
        return userUiMapper.mapToDisplayName(user)
    }
}
