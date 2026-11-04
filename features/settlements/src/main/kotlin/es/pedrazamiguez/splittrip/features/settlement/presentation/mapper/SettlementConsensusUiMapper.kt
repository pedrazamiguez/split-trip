package es.pedrazamiguez.splittrip.features.settlement.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.EphemeralSettlementIdHelper
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.ConsensusChipStyle
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementConsensusItemUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class SettlementConsensusUiMapper(
    private val localeProvider: LocaleProvider,
    private val resourceProvider: ResourceProvider
) {
    fun toConsensusItems(
        settlements: List<SettlementRecord>,
        currentUserId: String,
        groupCreatorId: String,
        memberProfiles: Map<String, User>,
        nudgeTimestamps: Map<String, Long> = emptyMap(),
        rateLimitHours: Long = 24L,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): ImmutableList<SettlementConsensusItemUiModel> {
        val activeRecords = settlements
            .filter { record ->
                record.status in ACTIVE_STATUSES &&
                    (record.settlement.fromUserId == currentUserId || record.settlement.toUserId == currentUserId)
            }

        val groupedRecords = activeRecords.groupBy { record ->
            val isCurrentUserPayer = record.settlement.fromUserId == currentUserId
            val counterpartyId = if (isCurrentUserPayer) record.settlement.toUserId else record.settlement.fromUserId
            Triple(counterpartyId, record.settlement.currency, record.status)
        }

        return groupedRecords.values
            .map { group ->
                // Sort within group to ensure consistent primary record if needed
                val sortedGroup = group.sortedBy { it.id }
                toConsensusItem(
                    records = sortedGroup,
                    currentUserId = currentUserId,
                    groupCreatorId = groupCreatorId,
                    memberProfiles = memberProfiles,
                    nudgeTimestamps = nudgeTimestamps,
                    rateLimitHours = rateLimitHours,
                    currentTimeMillis = currentTimeMillis
                )
            }
            .sortedWith(
                compareBy { item ->
                    when (item.status) {
                        SettlementStatus.DISPUTED -> 0
                        SettlementStatus.CONFIRMED_BY_PAYER -> 1
                        SettlementStatus.SUGGESTED -> 2
                        else -> 3
                    }
                }
            )
            .toImmutableList()
    }

    private fun toConsensusItem(
        records: List<SettlementRecord>,
        currentUserId: String,
        groupCreatorId: String,
        memberProfiles: Map<String, User>,
        nudgeTimestamps: Map<String, Long>,
        rateLimitHours: Long,
        currentTimeMillis: Long
    ): SettlementConsensusItemUiModel {
        val primaryRecord = records.first()
        val isCurrentUserPayer = primaryRecord.settlement.fromUserId == currentUserId
        val counterpartyName = resolveCounterpartyName(primaryRecord, isCurrentUserPayer, memberProfiles)
        val directionLabel = resolveDirectionLabel(isCurrentUserPayer, counterpartyName)

        val totalAmount = records.sumOf { it.settlement.amount }
        val formattedAmount = formatCurrencyAmount(
            amount = totalAmount,
            currencyCode = primaryRecord.settlement.currency,
            locale = localeProvider.getCurrentLocale()
        )

        val isCash = primaryRecord.settlement.sourcePocket == SettlementPocketType.CASH
        val actions = resolveActionCapabilities(
            status = primaryRecord.status,
            isCurrentUserPayer = isCurrentUserPayer,
            isPayee = primaryRecord.settlement.toUserId == currentUserId,
            isGroupCreator = currentUserId == groupCreatorId,
            isCash = isCash
        )

        val isCreditor = primaryRecord.settlement.toUserId == currentUserId
        val isSuggested = primaryRecord.status == SettlementStatus.SUGGESTED ||
            EphemeralSettlementIdHelper.isEphemeral(primaryRecord.id)
        val canNudge = isCreditor && !isCash && !isSuggested

        val nudgeInfo = resolveNudgeInfo(
            records = records,
            nudgeTimestamps = nudgeTimestamps,
            rateLimitHours = rateLimitHours,
            currentTimeMillis = currentTimeMillis,
            isCash = isCash,
            isSuggested = isSuggested
        )

        val statusDetails = resolveStatusDetails(primaryRecord.status)

        return SettlementConsensusItemUiModel(
            settlementId = records.joinToString(",") { it.id },
            counterpartyName = counterpartyName,
            formattedAmount = formattedAmount,
            currencyCode = primaryRecord.settlement.currency,
            pocketTypeLabel = resolveCombinedPocketTypeLabel(records, primaryRecord),
            directionLabel = directionLabel,
            statusLabel = statusDetails.label,
            statusChipStyle = statusDetails.style,
            isCurrentUserPayer = isCurrentUserPayer,
            canConfirm = actions.canConfirm,
            confirmLabel = actions.confirmLabel,
            canDispute = actions.canDispute,
            disputeReason = primaryRecord.disputeReason,
            status = primaryRecord.status,
            canNudge = canNudge,
            isNudgeRateLimited = nudgeInfo.first,
            nudgeButtonLabel = nudgeInfo.second
        )
    }

    private fun resolveCounterpartyName(
        primaryRecord: SettlementRecord,
        isCurrentUserPayer: Boolean,
        memberProfiles: Map<String, User>
    ): String {
        val counterpartyId = if (isCurrentUserPayer) {
            primaryRecord.settlement.toUserId
        } else {
            primaryRecord.settlement.fromUserId
        }
        return memberProfiles[counterpartyId]?.displayName ?: counterpartyId
    }

    private fun resolveDirectionLabel(isCurrentUserPayer: Boolean, counterpartyName: String): String {
        return if (isCurrentUserPayer) {
            resourceProvider.getString(R.string.your_balance_settlement_you_owe, counterpartyName)
        } else {
            resourceProvider.getString(R.string.your_balance_settlement_owes_you, counterpartyName)
        }
    }

    private fun resolveCombinedPocketTypeLabel(
        records: List<SettlementRecord>,
        primaryRecord: SettlementRecord
    ): String {
        return if (records.size > 1) {
            val hasPocket = records.any { it.settlement.sourcePocket == SettlementPocketType.POCKET }
            val hasCash = records.any { it.settlement.sourcePocket == SettlementPocketType.CASH }
            if (hasPocket && hasCash) {
                "${resolvePocketTypeLabel(SettlementPocketType.POCKET)} + " +
                    resolvePocketTypeLabel(SettlementPocketType.CASH)
            } else {
                resolvePocketTypeLabel(primaryRecord.settlement.sourcePocket)
            }
        } else {
            resolvePocketTypeLabel(primaryRecord.settlement.sourcePocket)
        }
    }

    private fun resolveNudgeInfo(
        records: List<SettlementRecord>,
        nudgeTimestamps: Map<String, Long>,
        rateLimitHours: Long,
        currentTimeMillis: Long,
        isCash: Boolean,
        isSuggested: Boolean = false
    ): Pair<Boolean, String> {
        if (isCash || isSuggested) {
            return Pair(true, "")
        }
        val lastNudgeTs = records.maxOfOrNull { nudgeTimestamps[it.id] ?: 0L } ?: 0L
        val rateLimitMillis = rateLimitHours * MILLIS_PER_HOUR
        val isNudgeRateLimited = lastNudgeTs > 0 && (currentTimeMillis - lastNudgeTs) < rateLimitMillis
        val nudgeButtonLabel = if (isNudgeRateLimited) {
            resourceProvider.getString(R.string.your_balance_settlement_reminded)
        } else {
            resourceProvider.getString(R.string.your_balance_settlement_remind)
        }
        return Pair(isNudgeRateLimited, nudgeButtonLabel)
    }

    private fun resolvePocketTypeLabel(sourcePocket: SettlementPocketType): String = when (sourcePocket) {
        SettlementPocketType.POCKET -> resourceProvider.getString(R.string.your_balance_settlement_pocket_pocket)
        SettlementPocketType.CASH -> resourceProvider.getString(R.string.your_balance_settlement_pocket_cash)
        SettlementPocketType.NET -> resourceProvider.getString(R.string.your_balance_settlement_pocket_net)
    }

    private fun resolveStatusDetails(status: SettlementStatus): StatusDetails = when (status) {
        SettlementStatus.SUGGESTED -> StatusDetails(
            label = resourceProvider.getString(R.string.your_balance_settlement_status_suggested),
            style = ConsensusChipStyle.SUGGESTED
        )
        SettlementStatus.CONFIRMED_BY_PAYER -> StatusDetails(
            label = resourceProvider.getString(R.string.your_balance_settlement_status_confirmed_payer),
            style = ConsensusChipStyle.IN_PROGRESS
        )
        SettlementStatus.DISPUTED -> StatusDetails(
            label = resourceProvider.getString(R.string.your_balance_settlement_status_disputed),
            style = ConsensusChipStyle.DISPUTED
        )
        SettlementStatus.RESOLVED -> StatusDetails(label = "", style = ConsensusChipStyle.SUGGESTED)
    }

    private fun resolveActionCapabilities(
        status: SettlementStatus,
        isCurrentUserPayer: Boolean,
        isPayee: Boolean,
        isGroupCreator: Boolean,
        isCash: Boolean
    ): ActionCapabilities {
        if (isCash || status == SettlementStatus.RESOLVED) {
            return ActionCapabilities(canConfirm = false, confirmLabel = "", canDispute = false)
        }

        return when (status) {
            SettlementStatus.SUGGESTED -> ActionCapabilities(
                canConfirm = isCurrentUserPayer,
                confirmLabel = if (isCurrentUserPayer) {
                    resourceProvider.getString(R.string.your_balance_settlement_mark_paid)
                } else {
                    ""
                },
                canDispute = true
            )
            SettlementStatus.CONFIRMED_BY_PAYER -> ActionCapabilities(
                canConfirm = isPayee,
                confirmLabel = if (isPayee) {
                    resourceProvider.getString(R.string.your_balance_settlement_confirm_receipt)
                } else {
                    ""
                },
                canDispute = isPayee
            )
            SettlementStatus.DISPUTED -> ActionCapabilities(
                canConfirm = isPayee || isGroupCreator,
                confirmLabel = if (isPayee || isGroupCreator) {
                    resourceProvider.getString(R.string.your_balance_settlement_resolve_dispute)
                } else {
                    ""
                },
                canDispute = false
            )
            else -> ActionCapabilities(canConfirm = false, confirmLabel = "", canDispute = false)
        }
    }

    private data class StatusDetails(val label: String, val style: ConsensusChipStyle)

    private data class ActionCapabilities(val canConfirm: Boolean, val confirmLabel: String, val canDispute: Boolean)

    companion object {
        private const val MILLIS_PER_HOUR = 3_600_000L
        private val ACTIVE_STATUSES = setOf(
            SettlementStatus.SUGGESTED,
            SettlementStatus.CONFIRMED_BY_PAYER,
            SettlementStatus.DISPUTED
        )
    }
}
