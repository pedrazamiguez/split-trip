package es.pedrazamiguez.splittrip.features.settlement.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.MappedPreview
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.impl.PocketDebtDistributionServiceImpl
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.MemberSpendingChartUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.SettlementConsensusUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.MemberSpendingChartUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementConsensusItemUiModel
import java.time.LocalDateTime

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

internal val PREVIEW_SETTLEMENT_RECORD_PENDING = SettlementRecord(
    id = "c-1",
    groupId = "group-1",
    settlement = Settlement(
        fromUserId = PREVIEW_SETTLEMENT_USER_YOU.userId,
        toUserId = PREVIEW_SETTLEMENT_USER_ANDRES.userId,
        amount = 2500L,
        currency = "EUR",
        sourcePocket = SettlementPocketType.NET
    ),
    status = SettlementStatus.SUGGESTED,
    createdAt = LocalDateTime.now()
)

internal val PREVIEW_SETTLEMENT_RECORD_CONFIRMED = PREVIEW_SETTLEMENT_RECORD_PENDING.copy(
    id = "c-2",
    status = SettlementStatus.CONFIRMED_BY_PAYER
)

internal val PREVIEW_SETTLEMENT_RECORD_DISPUTED = PREVIEW_SETTLEMENT_RECORD_PENDING.copy(
    id = "c-3",
    status = SettlementStatus.DISPUTED,
    disputeReason = "Wrong amount entered"
)

internal val PREVIEW_SETTLEMENT_RECORD_RECEIVER = SettlementRecord(
    id = "c-4",
    groupId = "group-1",
    settlement = Settlement(
        fromUserId = PREVIEW_SETTLEMENT_USER_ANDRES.userId,
        toUserId = PREVIEW_SETTLEMENT_USER_YOU.userId,
        amount = 5000L,
        currency = "EUR",
        sourcePocket = SettlementPocketType.NET
    ),
    status = SettlementStatus.SUGGESTED,
    createdAt = LocalDateTime.now()
)

@Composable
internal fun SettlementConsensusCardPreviewHelper(
    record: SettlementRecord = PREVIEW_SETTLEMENT_RECORD_PENDING,
    currentUserId: String = PREVIEW_SETTLEMENT_USER_YOU.userId,
    groupCreatorId: String = PREVIEW_SETTLEMENT_USER_YOU.userId,
    memberProfiles: Map<String, User> = PREVIEW_SETTLEMENT_MEMBER_PROFILES,
    content: @Composable (SettlementConsensusItemUiModel) -> Unit
) {
    MappedPreview(
        domain = record,
        mapper = { localeProvider, resourceProvider ->
            SettlementConsensusUiMapper(localeProvider, resourceProvider)
        },
        transform = { mapper, domain ->
            mapper.toConsensusItems(
                settlements = listOf(domain),
                currentUserId = currentUserId,
                groupCreatorId = groupCreatorId,
                memberProfiles = memberProfiles
            ).first()
        },
        content = content
    )
}
