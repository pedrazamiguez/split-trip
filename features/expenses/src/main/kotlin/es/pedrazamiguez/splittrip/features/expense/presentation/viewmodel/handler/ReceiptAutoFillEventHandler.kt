package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.usecase.expense.ExtractReceiptFieldsUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AutoFillBanner
import java.math.BigDecimal
import java.time.ZoneOffset
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class ReceiptAutoFillEventHandler(
    private val extractReceiptFieldsUseCase: ExtractReceiptFieldsUseCase,
    private val receiptExtractionService: ReceiptExtractionService,
    private val formattingHelper: FormattingHelper
) : AddExpenseEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var _actionsFlow: MutableSharedFlow<AddExpenseUiAction>
    private lateinit var scope: CoroutineScope

    private var onCurrencySelected: ((String) -> Unit)? = null
    private var onAmountChanged: ((String) -> Unit)? = null

    override fun bind(
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        actionsFlow: MutableSharedFlow<AddExpenseUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actionsFlow = actionsFlow
        this.scope = scope
    }

    fun setOnCurrencySelected(callback: (String) -> Unit) {
        onCurrencySelected = callback
    }

    fun setOnAmountChanged(callback: (String) -> Unit) {
        onAmountChanged = callback
    }

    /**
     * Triggered when a receipt is successfully attached. If AI extraction is supported on
     * the device, runs OCR and extracts structured fields, then merges them.
     */
    fun handleReceiptAttached(attachment: ReceiptAttachment) {
        val capability = receiptExtractionService.capability()
        if (capability != ExtractionCapability.ON_DEVICE_AI) {
            scope.launch {
                _actionsFlow.emit(
                    AddExpenseUiAction.ShowPill(
                        UiText.StringResource(R.string.expense_autofill_unavailable)
                    )
                )
            }
            return
        }

        scope.launch {
            _actionsFlow.emit(
                AddExpenseUiAction.ShowPill(
                    UiText.StringResource(R.string.expense_autofill_in_progress)
                )
            )

            extractReceiptFieldsUseCase(attachment)
                .onSuccess { extracted ->
                    mergeExtractedFields(extracted)
                }
                .onFailure { error ->
                    Timber.e(error, "Receipt auto-fill failed")
                    _actionsFlow.emit(
                        AddExpenseUiAction.ShowPill(
                            UiText.StringResource(R.string.expense_autofill_failed)
                        )
                    )
                }
        }
    }

    /**
     * Toggles the AI mode active state and coordinates navigation if the current step is affected.
     */
    fun handleSetAiModeActive(active: Boolean) {
        val state = _uiState.value
        if (state.isAiModeActive == active) return

        _uiState.update { it.copy(isAiModeActive = active) }

        // If the user turned off AI mode while on the RECEIPT step, transition to TITLE.
        // If they turned on AI mode while on TITLE step, transition to RECEIPT.
        val updatedState = _uiState.value
        if (!active && state.currentStep == AddExpenseStep.RECEIPT) {
            _uiState.update { updatedState.copy(currentStep = AddExpenseStep.TITLE) }
        } else if (active && state.currentStep == AddExpenseStep.TITLE) {
            _uiState.update { updatedState.copy(currentStep = AddExpenseStep.RECEIPT) }
        }
    }

    /**
     * Dismisses the auto-fill pre-fill fields banner.
     */
    fun handleDismissAutoFillBanner() {
        _uiState.update { it.copy(autoFillBanner = null) }
    }

    private suspend fun mergeExtractedFields(extracted: ExtractedReceipt) {
        val state = _uiState.value
        val bannerFields = mutableListOf<UiText>()

        tryMergeTitle(extracted, state, bannerFields)
        tryMergeDate(extracted, state, bannerFields)
        tryMergeCurrency(extracted, state, bannerFields)
        tryMergeAmount(extracted, state, bannerFields)

        // 5. Update banner state and notify success
        if (bannerFields.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    autoFillBanner = AutoFillBanner(
                        fields = bannerFields.toImmutableList(),
                        source = extracted.source
                    )
                )
            }
            _actionsFlow.emit(
                AddExpenseUiAction.ShowPill(
                    UiText.StringResource(R.string.expense_autofill_success)
                )
            )
        }
    }

    private fun tryMergeTitle(
        extracted: ExtractedReceipt,
        state: AddExpenseUiState,
        bannerFields: MutableList<UiText>
    ) {
        val extractedTitle = extracted.title
        val shouldPreFillTitle = state.expenseTitle.isBlank() &&
            !extractedTitle.isNullOrBlank() &&
            extracted.confidence == ExtractionConfidence.HIGH

        if (shouldPreFillTitle && extractedTitle != null) {
            _uiState.update { it.copy(expenseTitle = extractedTitle, isTitleValid = true) }
            bannerFields.add(UiText.StringResource(R.string.expense_field_title))
        }
    }

    private fun tryMergeDate(
        extracted: ExtractedReceipt,
        state: AddExpenseUiState,
        bannerFields: MutableList<UiText>
    ) {
        val extractedDate = extracted.date
        val shouldPreFillDate = state.expenseDateMillis == null && extractedDate != null
        if (shouldPreFillDate && extractedDate != null) {
            val dateMillis = extractedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            _uiState.update { it.copy(expenseDateMillis = dateMillis) }
            bannerFields.add(UiText.StringResource(R.string.add_expense_date))
        }
    }

    private fun tryMergeCurrency(
        extracted: ExtractedReceipt,
        state: AddExpenseUiState,
        bannerFields: MutableList<UiText>
    ) {
        val extractedCurrencyCode = extracted.currency
        val currentCurrency = state.selectedCurrency
        val shouldPreFillCurrency = !extractedCurrencyCode.isNullOrBlank() &&
            extractedCurrencyCode != currentCurrency?.code

        if (shouldPreFillCurrency) {
            val matchingCurrency = state.availableCurrencies
                .find { it.code.equals(extractedCurrencyCode, ignoreCase = true) }
            if (matchingCurrency != null) {
                onCurrencySelected?.invoke(matchingCurrency.code)
                bannerFields.add(UiText.StringResource(R.string.add_expense_currency_label))
            }
        }
    }

    private fun tryMergeAmount(
        extracted: ExtractedReceipt,
        state: AddExpenseUiState,
        bannerFields: MutableList<UiText>
    ) {
        val extractedAmount = extracted.amount
        val shouldPreFillAmount = state.sourceAmount.isBlank() &&
            extractedAmount != null &&
            extractedAmount > BigDecimal.ZERO

        if (shouldPreFillAmount && extractedAmount != null) {
            // Re-read selected currency decimal places in case currency changed
            val decimalDigits = _uiState.value.selectedCurrency?.decimalDigits ?: 2
            val cents = extractedAmount.movePointRight(decimalDigits).toLong()
            val amountString = formattingHelper.formatCentsValue(cents, decimalDigits)
            onAmountChanged?.invoke(amountString)
            bannerFields.add(UiText.StringResource(R.string.expense_field_amount))
        }
    }
}
