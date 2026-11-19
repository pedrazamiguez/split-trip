package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.delegate

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.usecase.balance.ConfirmSettlementUseCase
import es.pedrazamiguez.splittrip.domain.usecase.settlement.NudgeDebtorUseCase
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.action.YourBalanceUiAction
import kotlinx.coroutines.channels.Channel
import timber.log.Timber

class YourBalanceActionDelegate(
    private val confirmSettlementUseCase: ConfirmSettlementUseCase,
    private val nudgeDebtorUseCase: NudgeDebtorUseCase,
    private val telemetryTracker: TelemetryTracker
) {

    suspend fun handleConfirm(
        settlementIdString: String,
        groupId: String?,
        isOffline: Boolean,
        actions: Channel<YourBalanceUiAction>
    ) {
        if (checkOfflineAndEmitError(isOffline, actions)) return
        val validGroupId = groupId ?: return
        val settlementIds = settlementIdString.split(",")

        var hasError = false
        for (id in settlementIds) {
            confirmSettlementUseCase(validGroupId, id).fold(
                onSuccess = {
                    telemetryTracker.trackEvent(
                        "settlement_confirmed",
                        mapOf("group_id" to validGroupId, "settlement_id" to id)
                    )
                },
                onFailure = { e ->
                    Timber.w(e, "Failed to confirm settlement $id")
                    hasError = true
                }
            )
        }

        if (hasError) {
            actions.send(
                YourBalanceUiAction.ShowError(
                    UiText.StringResource(R.string.your_balance_confirm_error)
                )
            )
        } else {
            actions.send(
                YourBalanceUiAction.ShowSuccess(
                    UiText.StringResource(R.string.your_balance_confirm_success)
                )
            )
        }
    }

    suspend fun handleNudgeDebtor(
        settlementId: String,
        groupId: String?,
        isOffline: Boolean,
        actions: Channel<YourBalanceUiAction>
    ) {
        if (checkOfflineAndEmitError(isOffline, actions)) return
        val validGroupId = groupId ?: return

        nudgeDebtorUseCase(validGroupId, settlementId).fold(
            onSuccess = {
                actions.send(
                    YourBalanceUiAction.ShowSuccess(
                        UiText.StringResource(R.string.your_balance_nudge_success)
                    )
                )
            },
            onFailure = { e ->
                Timber.w(e, "Failed to send nudge for settlement $settlementId")
                actions.send(
                    YourBalanceUiAction.ShowError(
                        UiText.StringResource(R.string.your_balance_nudge_error)
                    )
                )
            }
        )
    }

    suspend fun handleOpenDispute(
        settlementId: String,
        groupId: String?,
        isOffline: Boolean,
        actions: Channel<YourBalanceUiAction>
    ) {
        if (checkOfflineAndEmitError(isOffline, actions)) return
        val validGroupId = groupId ?: return
        actions.send(
            YourBalanceUiAction.NavigateToDisputeSettlement(
                groupId = validGroupId,
                settlementId = settlementId
            )
        )
    }

    private suspend fun checkOfflineAndEmitError(isOffline: Boolean, actions: Channel<YourBalanceUiAction>): Boolean {
        if (isOffline) {
            actions.send(
                YourBalanceUiAction.ShowError(
                    UiText.StringResource(R.string.your_balance_offline_warning)
                )
            )
            return true
        }
        return false
    }
}
