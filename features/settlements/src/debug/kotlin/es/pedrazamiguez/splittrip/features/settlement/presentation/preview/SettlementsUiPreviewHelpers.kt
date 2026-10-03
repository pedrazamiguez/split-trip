package es.pedrazamiguez.splittrip.features.settlement.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.MappedPreview
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.impl.PocketDebtDistributionServiceImpl
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.MemberSpendingChartUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.MemberSpendingChartUiModel

internal val PREVIEW_SETTLEMENT_USER_YOU = User(userId = "user-1", displayName = "You", email = "")
internal val PREVIEW_SETTLEMENT_USER_ANDRES = User(userId = "user-2", displayName = "Andrés", email = "")
internal val PREVIEW_SETTLEMENT_USER_PEPE = User(userId = "user-3", displayName = "Pepe", email = "")

internal val PREVIEW_SETTLEMENT_MEMBER_PROFILES = mapOf(
    PREVIEW_SETTLEMENT_USER_YOU.userId to PREVIEW_SETTLEMENT_USER_YOU,
    PREVIEW_SETTLEMENT_USER_ANDRES.userId to PREVIEW_SETTLEMENT_USER_ANDRES,
    PREVIEW_SETTLEMENT_USER_PEPE.userId to PREVIEW_SETTLEMENT_USER_PEPE
)

internal val PREVIEW_MEMBER_BALANCES_SCENARIO_A = listOf(
    MemberBalance(userId = "user-1", withdrawn = 166666L, cashSpent = 300000L, totalSpent = 300000L),
    MemberBalance(userId = "user-2", withdrawn = 166666L, cashSpent = 20000L, totalSpent = 20000L),
    MemberBalance(userId = "user-3", withdrawn = 166666L, cashSpent = 0L, totalSpent = 0L)
)

internal val PREVIEW_MEMBER_BALANCES_SCENARIO_B = listOf(
    MemberBalance(userId = "user-1", withdrawn = 166666L, cashSpent = 300000L, totalSpent = 300000L),
    MemberBalance(userId = "user-2", withdrawn = 166666L, cashSpent = 170000L, totalSpent = 170000L),
    MemberBalance(userId = "user-3", withdrawn = 166666L, cashSpent = 0L, totalSpent = 0L)
)

@Composable
internal fun MemberSpendingBarChartPreviewHelper(
    domainBalances: List<MemberBalance> = PREVIEW_MEMBER_BALANCES_SCENARIO_A,
    cashOnly: Boolean = true,
    currentUserId: String = "user-1",
    memberProfiles: Map<String, User> = PREVIEW_SETTLEMENT_MEMBER_PROFILES,
    groupCurrencyCode: String = "EUR",
    content: @Composable (MemberSpendingChartUiModel) -> Unit
) {
    MappedPreview(
        domain = domainBalances,
        mapper = { localeProvider, resourceProvider ->
            MemberSpendingChartUiMapper(
                localeProvider = localeProvider,
                userUiMapper = UserUiMapper(resourceProvider),
                pocketDebtDistributionService = PocketDebtDistributionServiceImpl()
            )
        },
        transform = { mapper, domain ->
            mapper.toChartUiModel(
                memberBalances = domain,
                cashOnly = cashOnly,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles,
                groupCurrencyCode = groupCurrencyCode
            )
        },
        content = content
    )
}
