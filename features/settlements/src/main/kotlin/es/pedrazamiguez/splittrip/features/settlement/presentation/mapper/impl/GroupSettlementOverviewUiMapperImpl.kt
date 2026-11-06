package es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.GroupSettlementOverviewUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementRowUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.archive.ArchiveWizardStep
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.GroupSettlementOverviewUiState
import kotlinx.collections.immutable.toImmutableList

class GroupSettlementOverviewUiMapperImpl(
    private val formattingHelper: FormattingHelper,
    private val resourceProvider: ResourceProvider,
    private val userUiMapper: UserUiMapper
) : GroupSettlementOverviewUiMapper {

    override fun toUiState(
        settlements: List<SettlementRecord>,
        memberProfiles: Map<String, User>,
        currentUserId: String,
        groupCreatorId: String,
        groupName: String
    ): GroupSettlementOverviewUiState {
        val rows = settlements.map { record ->
            toRowModel(record, memberProfiles, currentUserId)
        }

        val pending = rows.filter {
            it.status == SettlementStatus.SUGGESTED ||
                it.status == SettlementStatus.CONFIRMED_BY_PAYER
        }
        val disputed = rows.filter { it.status == SettlementStatus.DISPUTED }
        val resolved = rows.filter { it.status == SettlementStatus.RESOLVED }

        val areAllSettlementsResolved = rows.all { it.status == SettlementStatus.RESOLVED }

        val isUserCreator = groupCreatorId == currentUserId
        val actionRequiredCount = pending.count { it.canCurrentUserConfirm }
        val waitingOnOthersCount = pending.count { !it.canCurrentUserConfirm }
        val disputedCount = disputed.size

        val activeSteps = listOf(
            ArchiveWizardStep.SETTLEMENT_SUMMARY,
            ArchiveWizardStep.CONFIRMATION
        ).toImmutableList()

        return GroupSettlementOverviewUiState(
            pendingSettlements = pending.toImmutableList(),
            disputedSettlements = disputed.toImmutableList(),
            resolvedSettlements = resolved.toImmutableList(),
            areAllSettlementsResolved = areAllSettlementsResolved,
            isLoading = false,
            currentStep = ArchiveWizardStep.SETTLEMENT_SUMMARY,
            activeSteps = activeSteps,
            isUserCreator = isUserCreator,
            actionRequiredCount = actionRequiredCount,
            waitingOnOthersCount = waitingOnOthersCount,
            disputedCount = disputedCount,
            groupName = groupName
        )
    }

    private fun toRowModel(
        record: SettlementRecord,
        memberProfiles: Map<String, User>,
        currentUserId: String
    ): SettlementRowUiModel {
        val debtorName = resolveMemberName(record.settlement.fromUserId, memberProfiles, currentUserId)
        val creditorName = resolveMemberName(record.settlement.toUserId, memberProfiles, currentUserId)
        val isDebtor = record.settlement.fromUserId == currentUserId
        val isCreditor = record.settlement.toUserId == currentUserId
        val directionTitle = resolveDirectionTitle(isDebtor, isCreditor, debtorName, creditorName)

        return SettlementRowUiModel(
            settlementId = record.id,
            debtorId = record.settlement.fromUserId,
            creditorId = record.settlement.toUserId,
            debtorName = debtorName,
            creditorName = creditorName,
            directionTitle = directionTitle,
            formattedAmount = formatAmount(record.settlement.amount, record.settlement.currency),
            isCurrentUserDebtor = isDebtor,
            isCurrentUserCreditor = isCreditor,
            pocketTypeLabel = resolvePocketTypeLabel(record.settlement.sourcePocket),
            currencyCode = record.settlement.currency,
            statusLabel = resolveStatusLabel(record.status),
            statusChipStyle = resolveStatusStyle(record.status),
            canCurrentUserConfirm = canConfirm(record.status, isDebtor, isCreditor),
            canCurrentUserDispute = canDispute(record.status, isDebtor, isCreditor),
            disputedByCurrentUser = record.disputedBy == currentUserId,
            disputeReason = record.disputeReason,
            status = record.status
        )
    }

    private fun resolveDirectionTitle(
        isDebtor: Boolean,
        isCreditor: Boolean,
        debtorName: String,
        creditorName: String
    ): String = when {
        isCreditor -> resourceProvider.getString(R.string.settlement_overview_direction_owes_you, debtorName)
        isDebtor -> resourceProvider.getString(R.string.settlement_overview_direction_you_owe, creditorName)
        else -> resourceProvider.getString(R.string.settlement_overview_direction_other_owes, debtorName, creditorName)
    }

    private fun resolveMemberName(
        userId: String,
        profiles: Map<String, User>,
        currentUserId: String
    ): String {
        return userUiMapper.mapToDisplayName(
            user = profiles[userId],
            fallbackUserId = userId,
            currentUserId = currentUserId,
            selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
        )
    }

    private fun resolveStatusLabel(status: SettlementStatus): String {
        return when (status) {
            SettlementStatus.SUGGESTED ->
                resourceProvider.getString(R.string.settlement_overview_status_suggested)
            SettlementStatus.CONFIRMED_BY_PAYER ->
                resourceProvider.getString(R.string.settlement_overview_status_confirmed_by_payer)
            SettlementStatus.DISPUTED ->
                resourceProvider.getString(R.string.settlement_overview_status_disputed)
            SettlementStatus.RESOLVED ->
                resourceProvider.getString(R.string.settlement_overview_status_resolved)
        }
    }

    private fun resolveStatusStyle(status: SettlementStatus): PassportTagStyle {
        return when (status) {
            SettlementStatus.SUGGESTED -> PassportTagStyle.NEUTRAL
            SettlementStatus.CONFIRMED_BY_PAYER -> PassportTagStyle.WARNING
            SettlementStatus.DISPUTED -> PassportTagStyle.ERROR
            SettlementStatus.RESOLVED -> PassportTagStyle.SUCCESS
        }
    }

    private fun canConfirm(
        status: SettlementStatus,
        isDebtor: Boolean,
        isCreditor: Boolean
    ): Boolean {
        return when (status) {
            SettlementStatus.SUGGESTED -> isDebtor
            SettlementStatus.CONFIRMED_BY_PAYER -> isCreditor
            SettlementStatus.DISPUTED -> false
            SettlementStatus.RESOLVED -> false
        }
    }

    private fun canDispute(
        status: SettlementStatus,
        isDebtor: Boolean,
        isCreditor: Boolean
    ): Boolean {
        return when (status) {
            SettlementStatus.SUGGESTED, SettlementStatus.CONFIRMED_BY_PAYER ->
                isDebtor || isCreditor
            SettlementStatus.DISPUTED, SettlementStatus.RESOLVED -> false
        }
    }

    private fun resolvePocketTypeLabel(type: SettlementPocketType): String {
        return when (type) {
            SettlementPocketType.POCKET ->
                resourceProvider.getString(DesignSystemR.string.settlement_pocket_type_virtual)
            SettlementPocketType.CASH ->
                resourceProvider.getString(DesignSystemR.string.settlement_pocket_type_cash)
            SettlementPocketType.NET ->
                resourceProvider.getString(DesignSystemR.string.settlement_pocket_type_net)
        }
    }

    private fun formatAmount(cents: Long, currencyCode: String): String {
        return formattingHelper.formatCentsWithCurrency(cents, currencyCode)
    }
}
