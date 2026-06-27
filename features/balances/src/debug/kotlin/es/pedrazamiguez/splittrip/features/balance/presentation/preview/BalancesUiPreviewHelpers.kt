package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.MappedPreview
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.BalancesUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ActivityItemUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.GroupPocketBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceCashContext
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun BalanceCardPreviewHelper(
    domainBalance: GroupPocketBalance = PREVIEW_POCKET_BALANCE,
    groupName: String = PREVIEW_GROUP_NAME,
    content: @Composable (GroupPocketBalanceUiModel) -> Unit
) {
    MappedPreview(
        domain = domainBalance,
        mapper = { localeProvider, resourceProvider ->
            BalancesUiMapper(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
        },
        transform = { mapper, domain ->
            mapper.mapBalance(domain, groupName)
        },
        content = content
    )
}

@Composable
fun ContributionItemPreviewHelper(
    domainContribution: Contribution = PREVIEW_CONTRIBUTION_GROUP,
    memberProfiles: Map<String, User> = PREVIEW_MEMBER_PROFILES,
    content: @Composable (ContributionUiModel) -> Unit
) {
    MappedPreview(
        domain = domainContribution,
        mapper = { localeProvider, resourceProvider ->
            BalancesUiMapper(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
        },
        transform = { mapper, domain ->
            mapper.mapContributions(
                listOf(domain),
                currentUserId = null,
                memberProfiles = memberProfiles
            ).first()
        },
        content = content
    )
}

@Composable
fun ContributionListPreviewHelper(
    domainContributions: List<Contribution> = PREVIEW_CONTRIBUTIONS,
    memberProfiles: Map<String, User> = PREVIEW_MEMBER_PROFILES,
    content: @Composable (ImmutableList<ContributionUiModel>) -> Unit
) {
    MappedPreview(
        domain = domainContributions,
        mapper = { localeProvider, resourceProvider ->
            BalancesUiMapper(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
        },
        transform = { mapper, domain ->
            mapper.mapContributions(
                domain,
                currentUserId = null,
                memberProfiles = memberProfiles
            )
        },
        content = content
    )
}

@Composable
fun ActivityListPreviewHelper(
    domainContributions: List<Contribution> = PREVIEW_CONTRIBUTIONS,
    domainWithdrawals: List<CashWithdrawal> = listOf(
        PREVIEW_CASH_WITHDRAWAL_GROUP,
        PREVIEW_CASH_WITHDRAWAL_SUBUNIT,
        PREVIEW_CASH_WITHDRAWAL_PERSONAL,
        PREVIEW_CASH_WITHDRAWAL_IMPERSONATED
    ),
    groupCurrency: String = "EUR",
    memberProfiles: Map<String, User> = PREVIEW_MEMBER_PROFILES,
    content: @Composable (ImmutableList<ActivityItemUiModel>) -> Unit
) {
    MappedPreview(
        domain = domainContributions to domainWithdrawals,
        mapper = { localeProvider, resourceProvider ->
            BalancesUiMapper(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
        },
        transform = { mapper, domain ->
            mapper.mapActivity(
                contributions = domain.first,
                withdrawals = domain.second,
                groupCurrency = groupCurrency,
                currentUserId = null,
                memberProfiles = memberProfiles
            )
        },
        content = content
    )
}

@Composable
fun CashWithdrawalItemPreviewHelper(
    domainWithdrawal: CashWithdrawal = PREVIEW_CASH_WITHDRAWAL_GROUP,
    groupCurrency: String = "EUR",
    memberProfiles: Map<String, User> = PREVIEW_MEMBER_PROFILES,
    content: @Composable (CashWithdrawalUiModel) -> Unit
) {
    MappedPreview(
        domain = domainWithdrawal,
        mapper = { localeProvider, resourceProvider ->
            BalancesUiMapper(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
        },
        transform = { mapper, domain ->
            mapper.mapCashWithdrawals(
                withdrawals = listOf(domain),
                groupCurrency = groupCurrency,
                currentUserId = null,
                memberProfiles = memberProfiles
            ).first()
        },
        content = content
    )
}

@Composable
fun MemberBalanceItemPreviewHelper(
    domainBalance: MemberBalance = PREVIEW_MEMBER_BALANCE_POSITIVE,
    currency: String = "EUR",
    memberProfiles: Map<String, User> = PREVIEW_MEMBER_PROFILES,
    content: @Composable (MemberBalanceUiModel) -> Unit
) {
    MappedPreview(
        domain = domainBalance,
        mapper = { localeProvider, resourceProvider ->
            BalancesUiMapper(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
        },
        transform = { mapper, domain ->
            mapper.mapMemberBalances(
                balances = listOf(domain),
                currency = currency,
                currentUserId = null,
                memberProfiles = memberProfiles
            ).first()
        },
        content = content
    )
}

@Composable
fun MemberBalanceListPreviewHelper(
    domainBalances: List<MemberBalance> = listOf(
        PREVIEW_MEMBER_BALANCE_POSITIVE,
        PREVIEW_MEMBER_BALANCE_NEGATIVE,
        PREVIEW_MEMBER_BALANCE_NEGATIVE_CASH
    ),
    currency: String = "EUR",
    memberProfiles: Map<String, User> = PREVIEW_MEMBER_PROFILES,
    content: @Composable (ImmutableList<MemberBalanceUiModel>) -> Unit
) {
    MappedPreview(
        domain = domainBalances,
        mapper = { localeProvider, resourceProvider ->
            BalancesUiMapper(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
        },
        transform = { mapper, domain ->
            mapper.mapMemberBalances(
                balances = domain,
                currency = currency,
                currentUserId = null,
                memberProfiles = memberProfiles
            )
        },
        content = content
    )
}

/**
 * Preview helper for [es.pedrazamiguez.splittrip.features.balance.presentation.component.CashBreakdownBottomSheet].
 *
 * Maps [memberBalance] + [withdrawals] through [BalancesUiMapper.mapMemberBalances] with a full
 * [MemberBalanceCashContext], then passes `cashBreakdown` and `formattedCashInHand` to [content].
 * Defaults exercise all three scope types (GROUP, SUBUNIT, USER) so all sections appear in the preview.
 */
@Composable
fun CashBreakdownPreviewHelper(
    memberBalance: MemberBalance = PREVIEW_MEMBER_BALANCE_FOR_BREAKDOWN,
    withdrawals: List<CashWithdrawal> = listOf(
        PREVIEW_CASH_WITHDRAWAL_GROUP,
        PREVIEW_CASH_WITHDRAWAL_SUBUNIT,
        PREVIEW_CASH_WITHDRAWAL_PERSONAL
    ),
    groupCurrency: String = "EUR",
    content: @Composable (breakdown: ImmutableList<CashBreakdownUiModel>, formattedTotal: String) -> Unit
) {
    MappedPreview(
        domain = Triple(memberBalance, withdrawals, groupCurrency),
        mapper = { localeProvider, resourceProvider ->
            BalancesUiMapper(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
        },
        transform = { mapper, (balance, wds, currency) ->
            val uiModel = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = balance.userId,
                groupCurrency = currency,
                cashContext = MemberBalanceCashContext(
                    withdrawals = wds,
                    subunitsMap = mapOf(PREVIEW_SUBUNIT_FOR_BREAKDOWN.id to PREVIEW_SUBUNIT_FOR_BREAKDOWN),
                    groupMemberIds = listOf("user-1", "user-2")
                )
            ).first()
            uiModel.cashBreakdown to uiModel.formattedCashInHand
        },
        content = { (breakdown, total) -> content(breakdown, total) }
    )
}
