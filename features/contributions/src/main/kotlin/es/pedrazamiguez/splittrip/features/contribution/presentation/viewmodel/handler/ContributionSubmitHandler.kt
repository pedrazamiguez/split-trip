package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.parseAmountToSmallestUnit
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.service.ContributionValidationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddContributionUseCase
import es.pedrazamiguez.splittrip.features.contribution.R
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.action.AddContributionUiAction
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Handles contribution submission: validation and use case delegation.
 *
 * Receives the current group currency via [groupCurrencyProvider] lambda
 * to avoid a direct handler-to-handler constructor dependency.
 */
class ContributionSubmitHandler(
    private val addContributionUseCase: AddContributionUseCase,
    private val contributionValidationService: ContributionValidationService,
    private val groupCurrencyProvider: () -> String
) : AddContributionEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddContributionUiState>
    private lateinit var _actions: MutableSharedFlow<AddContributionUiAction>
    private lateinit var scope: CoroutineScope

    override fun bind(
        stateFlow: MutableStateFlow<AddContributionUiState>,
        actionsFlow: MutableSharedFlow<AddContributionUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    fun handleSubmit(groupId: String?, onSuccess: () -> Unit) {
        if (groupId == null) return

        val state = _uiState.value
        val groupCurrency = groupCurrencyProvider()
        val amountInSmallestUnit = parseAmountToSmallestUnit(state.amountInput, groupCurrency)

        // Validate amount
        val amountValidation = contributionValidationService.validateAmount(amountInSmallestUnit)
        if (amountValidation is ContributionValidationService.ValidationResult.Invalid) {
            _uiState.update { it.copy(amountError = true) }
            return
        }

        // Validate subunit selection against loaded options (only for SUBUNIT scope)
        val selectedSubunitId = state.selectedSubunitId
        if (!validateSubunit(state, selectedSubunitId)) {
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        scope.launch {
            val contribution = Contribution(
                groupId = groupId,
                userId = state.selectedMemberId ?: "",
                contributionScope = state.contributionScope,
                subunitId = selectedSubunitId,
                amount = amountInSmallestUnit,
                currency = groupCurrency
            )
            performSubmit(groupId, contribution, onSuccess)
        }
    }

    private fun validateSubunit(state: AddContributionUiState, selectedSubunitId: String?): Boolean {
        if (state.contributionScope == PayerType.SUBUNIT && selectedSubunitId != null) {
            val validSubunitIds = state.subunitOptions.map { it.id }.toSet()
            if (selectedSubunitId !in validSubunitIds) {
                scope.launch {
                    _actions.emit(
                        AddContributionUiAction.ShowError(
                            UiText.StringResource(R.string.contribution_add_money_error_subunit)
                        )
                    )
                }
                return false
            }
        }
        return true
    }

    private suspend fun performSubmit(
        groupId: String,
        contribution: Contribution,
        onSuccess: () -> Unit
    ) {
        try {
            addContributionUseCase(groupId, contribution)
            _uiState.update { it.copy(isLoading = false) }
            _actions.emit(
                AddContributionUiAction.ShowSuccess(
                    UiText.StringResource(R.string.contribution_add_money_success)
                )
            )
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException) {
            Timber.e(e, "Group is archived, cannot add contribution")
            _uiState.update { it.copy(isLoading = false) }
            _actions.emit(
                AddContributionUiAction.ShowError(
                    UiText.StringResource(
                        es.pedrazamiguez.splittrip.core.designsystem.R.string.group_error_archived
                    )
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to add contribution")
            _uiState.update { it.copy(isLoading = false) }
            _actions.emit(
                AddContributionUiAction.ShowError(
                    UiText.StringResource(R.string.contribution_add_money_error)
                )
            )
        }
    }
}
