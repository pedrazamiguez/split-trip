package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.usecase.expense.AttachReceiptUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Handles simple form field events that contain inline branching logic:
 * [SourceAmountChanged], [PaymentMethodSelected], [FundingSourceSelected],
 * [PaymentStatusSelected], [DueDateSelected], and other direct state updates.
 *
 * Cross-handler coordination (e.g., recalculating splits after amount changes)
 * is routed via [formPostCallback], following the same pattern as
 * [ConfigEventHandler]'s [PostConfigAction] callback.
 */
@Suppress("TooManyFunctions")
class FormEventHandler(
    private val addExpenseUiMapper: AddExpenseUiMapper,
    private val attachReceiptUseCase: AttachReceiptUseCase
) : AddExpenseEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var _actionsFlow: MutableSharedFlow<AddExpenseUiAction>
    private lateinit var _scope: CoroutineScope

    /**
     * Callback for post-form-update actions that require cross-handler communication.
     * Set by the ViewModel during initialization via [setFormPostCallback].
     */
    private var formPostCallback: ((FormPostAction) -> Unit)? = null
    private var onReceiptAttached: ((ReceiptAttachment) -> Unit)? = null

    private var attachReceiptJob: Job? = null

    override fun bind(
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        actionsFlow: MutableSharedFlow<AddExpenseUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actionsFlow = actionsFlow
        _scope = scope
    }

    /**
     * Registers the callback the ViewModel uses to route post-form-update actions
     * to the appropriate sibling handlers.
     */
    fun setFormPostCallback(callback: (FormPostAction) -> Unit) {
        formPostCallback = callback
    }

    /**
     * Registers the callback for receipt attachment success.
     */
    fun setOnReceiptAttached(callback: (ReceiptAttachment) -> Unit) {
        onReceiptAttached = callback
    }

    fun handleTitleChanged(title: String) {
        _uiState.update {
            it.copy(
                expenseTitle = title,
                isTitleValid = true,
                error = null
            )
        }
    }

    fun handleSourceAmountChanged(amount: String) {
        val isValid = validateAmountInput(amount)
        _uiState.update {
            it.copy(
                sourceAmount = amount,
                isAmountValid = isValid,
                error = null
            )
        }
        val state = _uiState.value
        val isCash = state.selectedPaymentMethod?.id?.let {
            try {
                PaymentMethod.fromString(it) == PaymentMethod.CASH
            } catch (_: IllegalArgumentException) {
                false
            }
        } ?: false
        formPostCallback?.invoke(
            FormPostAction.RecalculateAfterAmount(
                isExchangeRateLocked = state.isExchangeRateLocked,
                isCash = isCash
            )
        )
    }

    fun handlePaymentMethodSelected(methodId: String) {
        val selectedMethod = _uiState.value.paymentMethods
            .find { it.id == methodId } ?: return
        _uiState.update { it.copy(selectedPaymentMethod = selectedMethod) }

        val isCash = try {
            PaymentMethod.fromString(selectedMethod.id) == PaymentMethod.CASH
        } catch (_: IllegalArgumentException) {
            false
        }
        val isGroupPocket = _uiState.value.selectedFundingSource?.id
            ?.let {
                runCatching { PayerType.fromString(it) }
                    .getOrDefault(PayerType.GROUP) == PayerType.GROUP
            }
            ?: true
        formPostCallback?.invoke(FormPostAction.PaymentMethodChanged(isCash, isGroupPocket))
    }

    fun handleFundingSourceSelected(fundingSourceId: String) {
        val selectedSource = _uiState.value.fundingSources
            .find { it.id == fundingSourceId } ?: return
        val isUserMoney = fundingSourceId == PayerType.USER.name
        _uiState.update {
            it.copy(
                selectedFundingSource = selectedSource,
                fundingSourceHint = if (isUserMoney) {
                    UiText.StringResource(R.string.funding_source_my_money_hint)
                } else {
                    null
                },
                contributionScope = if (isUserMoney) it.contributionScope else PayerType.USER,
                selectedContributionSubunitId = if (isUserMoney) {
                    it.selectedContributionSubunitId
                } else {
                    null
                }
            ).withStepClamped()
        }
        formPostCallback?.invoke(FormPostAction.FundingSourceChanged(isGroupPocket = !isUserMoney))
    }

    fun handleContributionScopeSelected(scope: PayerType, subunitId: String?) {
        _uiState.update {
            it.copy(
                contributionScope = scope,
                selectedContributionSubunitId = subunitId
            )
        }
    }

    fun handleCategorySelected(categoryId: String) {
        val selectedCategory = _uiState.value.availableCategories
            .find { it.id == categoryId } ?: return
        _uiState.update { it.copy(selectedCategory = selectedCategory) }
    }

    fun handleVendorChanged(vendor: String) {
        _uiState.update { it.copy(vendor = vendor) }
    }

    fun handleNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun handlePaymentStatusSelected(statusId: String) {
        val selectedStatus = _uiState.value.availablePaymentStatuses
            .find { it.id == statusId } ?: return
        val isScheduled = statusId == PaymentStatus.SCHEDULED.name
        val isRefundable = statusId == PaymentStatus.REFUNDABLE.name
        val showDueDate = isScheduled || isRefundable
        _uiState.update {
            it.copy(
                selectedPaymentStatus = selectedStatus,
                showDueDateSection = showDueDate,
                dueDateMillis = if (showDueDate) it.dueDateMillis else null,
                formattedDueDate = if (showDueDate) it.formattedDueDate else "",
                isDueDateValid = true
            )
        }
    }

    fun handleDueDateSelected(dateMillis: Long) {
        val formattedDate = addExpenseUiMapper.formatDueDateForDisplay(dateMillis)
        _uiState.update {
            it.copy(
                dueDateMillis = dateMillis,
                formattedDueDate = formattedDate,
                isDueDateValid = true
            )
        }
    }

    fun handleExpenseDateSelected(dateMillis: Long) {
        val isValid = dateMillis <= System.currentTimeMillis()
        val formattedDate = addExpenseUiMapper.formatExpenseDateForDisplay(dateMillis)
        _uiState.update {
            it.copy(
                expenseDateMillis = dateMillis,
                formattedExpenseDate = formattedDate,
                isExpenseDateValid = isValid,
                isExpenseDateModifiedByUser = true,
                error = if (!isValid) UiText.StringResource(R.string.expense_error_date_future) else null
            )
        }
    }

    fun handleReceiptImageChanged(uri: String?) {
        attachReceiptJob?.cancel()
        if (uri == null) {
            _uiState.update { it.copy(receiptUri = null, receiptAttachment = null) }
            return
        }
        // Copy + compress the file asynchronously so the UI thread is not blocked.
        // The state is updated when the use case resolves; if it fails a pill error is shown.
        attachReceiptJob = _scope.launch {
            attachReceiptUseCase(uri)
                .onSuccess { attachment ->
                    _uiState.update {
                        it.copy(
                            receiptUri = attachment.localUri,
                            receiptAttachment = attachment
                        )
                    }
                    onReceiptAttached?.invoke(attachment)
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    Timber.e(e, "Failed to attach receipt from URI: $uri")
                    _actionsFlow.emit(
                        AddExpenseUiAction.ShowError(UiText.StringResource(R.string.add_expense_receipt_attach_error))
                    )
                }
        }
    }
}
