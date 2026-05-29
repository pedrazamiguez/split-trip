package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.usecase.expense.ExtractReceiptFieldsUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
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

@Suppress("TooManyFunctions")
class ReceiptAutoFillEventHandler(
    private val extractReceiptFieldsUseCase: ExtractReceiptFieldsUseCase,
    private val receiptExtractionService: ReceiptExtractionService,
    private val formattingHelper: FormattingHelper,
    private val addExpenseUiMapper: AddExpenseUiMapper
) : AddExpenseEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var _actionsFlow: MutableSharedFlow<AddExpenseUiAction>
    private lateinit var scope: CoroutineScope

    private var onCurrencySelected: ((String) -> Unit)? = null
    private var onAmountChanged: ((String) -> Unit)? = null
    private var onCategorySelected: ((String) -> Unit)? = null
    private var onPaymentMethodSelected: ((String) -> Unit)? = null

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

    fun setOnCategorySelected(callback: (String) -> Unit) {
        onCategorySelected = callback
    }

    fun setOnPaymentMethodSelected(callback: (String) -> Unit) {
        onPaymentMethodSelected = callback
    }

    /**
     * Triggered when a receipt is successfully attached. If AI extraction is supported on
     * the device, runs OCR and extracts structured fields, then merges them.
     */
    fun handleReceiptAttached(attachment: ReceiptAttachment) {
        val state = _uiState.value
        if (!state.isAiModeActive) return

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

        val preScanCurrency = state.selectedCurrency
        val preScanPaymentMethod = state.selectedPaymentMethod

        scope.launch {
            _uiState.update { it.copy(isAnalyzingReceipt = true) }

            extractReceiptFieldsUseCase(attachment)
                .onSuccess { extracted ->
                    mergeExtractedFields(extracted, preScanCurrency, preScanPaymentMethod)
                    _uiState.update { it.copy(isAnalyzingReceipt = false) }
                }
                .onFailure { error ->
                    Timber.e(error, "Receipt auto-fill failed")
                    _uiState.update { it.copy(isAnalyzingReceipt = false) }
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

    private suspend fun mergeExtractedFields(
        extracted: ExtractedReceipt,
        preScanCurrency: CurrencyUiModel?,
        preScanPaymentMethod: PaymentMethodUiModel?
    ) {
        val bannerFields = mutableListOf<UiText>()

        tryMergeTitleAndVendor(extracted, bannerFields)
        tryMergeDate(extracted, bannerFields)
        tryMergeCurrency(extracted, preScanCurrency, bannerFields)
        tryMergeAmount(extracted, bannerFields)
        tryMergeCategory(extracted, bannerFields)
        tryMergePaymentMethod(extracted, preScanPaymentMethod, bannerFields)
        tryMergeNotes(extracted, bannerFields)

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

    private fun tryMergeTitleAndVendor(
        extracted: ExtractedReceipt,
        bannerFields: MutableList<UiText>
    ) {
        val extractedTitle = extracted.title ?: extracted.vendor
        val stateTitle = _uiState.value
        if (stateTitle.expenseTitle.isBlank() &&
            !extractedTitle.isNullOrBlank() &&
            extracted.confidence == ExtractionConfidence.HIGH
        ) {
            _uiState.update { it.copy(expenseTitle = extractedTitle, isTitleValid = true) }
            bannerFields.add(UiText.StringResource(R.string.expense_field_title))
        }

        val extractedVendor = extracted.vendor
        val stateVendor = _uiState.value
        if (stateVendor.vendor.isBlank() &&
            !extractedVendor.isNullOrBlank() &&
            extracted.confidence == ExtractionConfidence.HIGH
        ) {
            _uiState.update { it.copy(vendor = extractedVendor) }
            bannerFields.add(UiText.StringResource(R.string.expense_field_vendor))
        }
    }

    private fun tryMergeDate(
        extracted: ExtractedReceipt,
        bannerFields: MutableList<UiText>
    ) {
        val extractedDate = extracted.date
        val currentState = _uiState.value
        if (!currentState.isExpenseDateModifiedByUser && extractedDate != null) {
            val extractedTime = extracted.time
            val localDateTime = if (extractedTime != null) {
                extractedDate.atTime(extractedTime)
            } else {
                extractedDate.atStartOfDay()
            }
            val dateMillis = localDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
            val formattedDate = addExpenseUiMapper.formatExpenseDateForDisplay(dateMillis)
            _uiState.update {
                it.copy(
                    expenseDateMillis = dateMillis,
                    formattedExpenseDate = formattedDate,
                    isExpenseDateValid = true
                )
            }
            bannerFields.add(UiText.StringResource(R.string.add_expense_date))
            if (extractedTime != null) {
                bannerFields.add(UiText.StringResource(R.string.expense_field_time))
            }
        }
    }

    private fun tryMergeCurrency(
        extracted: ExtractedReceipt,
        preScanCurrency: CurrencyUiModel?,
        bannerFields: MutableList<UiText>
    ) {
        val extractedCurrencyCode = extracted.currency
        val currentState = _uiState.value
        val currentCurrency = currentState.selectedCurrency
        val shouldPreFillCurrency = !extractedCurrencyCode.isNullOrBlank() &&
            currentCurrency == preScanCurrency &&
            extractedCurrencyCode != currentCurrency?.code

        if (shouldPreFillCurrency) {
            val matchingCurrency = currentState.availableCurrencies
                .find { it.code.equals(extractedCurrencyCode, ignoreCase = true) }
            if (matchingCurrency != null) {
                onCurrencySelected?.invoke(matchingCurrency.code)
                bannerFields.add(UiText.StringResource(R.string.add_expense_currency_label))
            }
        }
    }

    private fun tryMergeAmount(
        extracted: ExtractedReceipt,
        bannerFields: MutableList<UiText>
    ) {
        val extractedAmount = extracted.amount
        val currentState = _uiState.value
        if (currentState.sourceAmount.isBlank() &&
            extractedAmount != null &&
            extractedAmount > BigDecimal.ZERO
        ) {
            // Re-read selected currency decimal places in case currency changed
            val decimalDigits = _uiState.value.selectedCurrency?.decimalDigits ?: 2
            try {
                val scaledAmount = extractedAmount.setScale(decimalDigits, java.math.RoundingMode.HALF_UP)
                val cents = scaledAmount.movePointRight(decimalDigits).longValueExact()
                val amountString = formattingHelper.formatCentsValue(cents, decimalDigits)
                onAmountChanged?.invoke(amountString)
                bannerFields.add(UiText.StringResource(R.string.expense_field_amount))
            } catch (e: Exception) {
                Timber.e(e, "Failed to convert extracted amount to cents")
            }
        }
    }

    private fun tryMergeCategory(
        extracted: ExtractedReceipt,
        bannerFields: MutableList<UiText>
    ) {
        val extractedCategory = extracted.category
        val currentState = _uiState.value
        val currentCategory = currentState.selectedCategory
        val shouldPreFillCategory = !extractedCategory.isNullOrBlank() &&
            !extractedCategory.equals(currentCategory?.id, ignoreCase = true)

        if (shouldPreFillCategory) {
            val matchingCategory = currentState.availableCategories
                .find { it.id.equals(extractedCategory, ignoreCase = true) }
            if (matchingCategory != null) {
                onCategorySelected?.invoke(matchingCategory.id)
                bannerFields.add(UiText.StringResource(R.string.add_expense_category_title))
            }
        }
    }

    private fun tryMergePaymentMethod(
        extracted: ExtractedReceipt,
        preScanPaymentMethod: PaymentMethodUiModel?,
        bannerFields: MutableList<UiText>
    ) {
        val extractedPaymentMethod = extracted.paymentMethod
        val currentState = _uiState.value
        val defaultPaymentMethod = currentState.paymentMethods.firstOrNull()
        val shouldPreFillPaymentMethod = !extractedPaymentMethod.isNullOrBlank() &&
            currentState.selectedPaymentMethod == preScanPaymentMethod &&
            (
                currentState.selectedPaymentMethod == null ||
                    currentState.selectedPaymentMethod == defaultPaymentMethod
                ) &&
            !extractedPaymentMethod.equals(currentState.selectedPaymentMethod?.id, ignoreCase = true)

        if (shouldPreFillPaymentMethod) {
            val matchingMethod = currentState.paymentMethods
                .find { it.id.equals(extractedPaymentMethod, ignoreCase = true) }
            if (matchingMethod != null) {
                onPaymentMethodSelected?.invoke(matchingMethod.id)
                bannerFields.add(UiText.StringResource(R.string.expense_field_payment_method))
            }
        }
    }

    private fun tryMergeNotes(
        extracted: ExtractedReceipt,
        bannerFields: MutableList<UiText>
    ) {
        val extractedNotes = extracted.notes
        val currentState = _uiState.value
        if (currentState.notes.isBlank() && !extractedNotes.isNullOrBlank()) {
            _uiState.update { it.copy(notes = extractedNotes) }
            bannerFields.add(UiText.StringResource(R.string.expense_field_notes))
        }
    }
}
