package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SettlementUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class SettlementsUiMapper(
    private val localeProvider: LocaleProvider,
    private val resourceProvider: ResourceProvider,
    private val userUiMapper: UserUiMapper
) {
    companion object {
        private val POCKET_RES_MAP = mapOf(
            SettlementPocketType.POCKET to DesignSystemR.string.settlement_pocket_type_virtual,
            SettlementPocketType.CASH to DesignSystemR.string.settlement_pocket_type_cash,
            SettlementPocketType.NET to DesignSystemR.string.settlement_pocket_type_net
        )

        private val STATUS_RES_MAP = mapOf(
            SettlementStatus.SUGGESTED to DesignSystemR.string.settlement_status_pending,
            SettlementStatus.CONFIRMED_BY_PAYER to DesignSystemR.string.settlement_status_awaiting_confirmation,
            SettlementStatus.DISPUTED to DesignSystemR.string.settlement_status_disputed,
            SettlementStatus.RESOLVED to DesignSystemR.string.settlement_status_confirmed
        )

        private val STATUS_STYLE_MAP = mapOf(
            SettlementStatus.SUGGESTED to PassportTagStyle.NEUTRAL,
            SettlementStatus.CONFIRMED_BY_PAYER to PassportTagStyle.WARNING,
            SettlementStatus.DISPUTED to PassportTagStyle.ERROR,
            SettlementStatus.RESOLVED to PassportTagStyle.SUCCESS
        )
    }

    fun mapSettlements(
        settlements: List<Settlement>,
        currency: String,
        currentUserId: String,
        memberProfiles: Map<String, User>
    ): ImmutableList<SettlementUiModel> {
        val locale = localeProvider.getCurrentLocale()

        val grouped = settlements.groupBy { s ->
            // In SettlementsUiMapper, we group to avoid "Debes a Pepe twice"
            val isCurrentUserPayer = s.fromUserId == currentUserId
            val counterpartyId = if (isCurrentUserPayer) s.toUserId else s.fromUserId
            Triple(counterpartyId, s.currency, isCurrentUserPayer)
        }

        return grouped.values.map { groupRecords ->
            val s = groupRecords.first()
            val isDebtor = s.fromUserId == currentUserId
            val isCreditor = s.toUserId == currentUserId
            val status = SettlementStatus.SUGGESTED

            val totalAmount = groupRecords.sumOf { it.amount }

            val pocketTypeLabel = resolvePocketTypeLabel(groupRecords, s)

            SettlementUiModel(
                debtorId = s.fromUserId,
                creditorId = s.toUserId,
                debtorName = userUiMapper.mapToDisplayName(
                    user = memberProfiles[s.fromUserId],
                    fallbackUserId = s.fromUserId,
                    currentUserId = currentUserId,
                    selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
                ),
                creditorName = userUiMapper.mapToDisplayName(
                    user = memberProfiles[s.toUserId],
                    fallbackUserId = s.toUserId,
                    currentUserId = currentUserId,
                    selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
                ),
                formattedAmount = formatCurrencyAmount(totalAmount, s.currency.ifEmpty { currency }, locale),
                isCurrentUserDebtor = isDebtor,
                isCurrentUserCreditor = isCreditor,
                pocketType = s.sourcePocket,
                currencyCode = s.currency.ifEmpty { currency },
                pocketTypeLabel = pocketTypeLabel,
                statusLabel = resourceProvider.getString(STATUS_RES_MAP[status]!!),
                statusChipStyle = STATUS_STYLE_MAP[status]!!,
                status = status
            )
        }.sortedWith { a, b ->
            val aInvolved = a.isCurrentUserDebtor || a.isCurrentUserCreditor
            val bInvolved = b.isCurrentUserDebtor || b.isCurrentUserCreditor
            when {
                aInvolved && !bInvolved -> -1
                !aInvolved && bInvolved -> 1
                else -> a.debtorName.compareTo(b.debtorName, ignoreCase = true)
            }
        }.toImmutableList()
    }

    private fun resolvePocketTypeLabel(
        groupRecords: List<Settlement>,
        s: Settlement
    ): String {
        return if (groupRecords.size > 1) {
            val hasPocket = groupRecords.any { it.sourcePocket == SettlementPocketType.POCKET }
            val hasCash = groupRecords.any { it.sourcePocket == SettlementPocketType.CASH }
            if (hasPocket && hasCash) {
                val pocketLabel = resourceProvider.getString(POCKET_RES_MAP[SettlementPocketType.POCKET]!!)
                val cashLabel = resourceProvider.getString(POCKET_RES_MAP[SettlementPocketType.CASH]!!)
                "$pocketLabel + $cashLabel"
            } else {
                resourceProvider.getString(POCKET_RES_MAP[s.sourcePocket]!!)
            }
        } else {
            resourceProvider.getString(POCKET_RES_MAP[s.sourcePocket]!!)
        }
    }
}
