package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.exception.TerminalDownloadException
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.DeleteExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.DownloadReceiptUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.UpdateExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpenseDetailUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpenseDetailUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpenseDetailUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpenseDetailUiState
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the Expense Detail screen.
 *
 * Mirrors the `_groupId`-gated `flatMapLatest + stateIn` pattern used in
 * [GroupDetailViewModel]: the expense ID is pushed via [setExpenseId], which
 * gates the reactive load. Deletion is handled by [DeleteExpenseUseCase] and
 * reported as a [ExpenseDetailUiAction.DeleteSuccess] side-effect so the
 * Feature can navigate back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseDetailViewModel(
    private val getExpenseByIdFlowUseCase: GetExpenseByIdFlowUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val getCashWithdrawalsFlowUseCase: GetCashWithdrawalsFlowUseCase,
    private val getGroupSubunitsUseCase: GetGroupSubunitsUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val downloadReceiptUseCase: DownloadReceiptUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val authenticationService: AuthenticationService,
    private val expenseDetailUiMapper: ExpenseDetailUiMapper,
    private val observeGroupUseCase: ObserveGroupUseCase
) : ViewModel() {

    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val failedDownloads = ConcurrentHashMap.newKeySet<String>()

    private val _expenseId = MutableStateFlow("")

    private val _actions = Channel<ExpenseDetailUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    val uiState: StateFlow<ExpenseDetailUiState> = _expenseId
        .filter { it.isNotBlank() }
        .flatMapLatest { expenseId ->
            var cachedUserIds = emptySet<String>()
            var cachedProfiles = emptyMap<String, User>()
            var cachedWithdrawalIds = emptySet<String>()
            var cachedWithdrawals = emptyMap<String, CashWithdrawal>()
            var cachedSubunitGroupId = ""
            var cachedSubunits = emptyMap<String, String>()

            getExpenseByIdFlowUseCase(expenseId)
                .flatMapLatest { expense ->
                    if (expense == null) {
                        return@flatMapLatest flowOf(
                            ExpenseDetailUiState(isLoading = false, hasError = true)
                        )
                    }

                    val groupFlow = observeGroupUseCase(expense.groupId)

                    val attachment = expense.receiptAttachment
                    if (shouldDownloadPdf(attachment) && !failedDownloads.contains(expense.id)) {
                        triggerReceiptDownload(expense.id, attachment?.remoteUrl.orEmpty())
                    }

                    val allUserIds = buildSet {
                        add(expense.createdBy)
                        expense.payerId?.let { add(it) }
                        expense.splits.forEach { add(it.userId) }
                    }

                    if (allUserIds != cachedUserIds) {
                        cachedUserIds = allUserIds
                        cachedProfiles = try {
                            getMemberProfilesUseCase(allUserIds.toList())
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to fetch member profiles for expense $expenseId")
                            emptyMap()
                        }
                    }

                    val currentUserId = authenticationService.currentUserId()

                    val withdrawalIds = expense.cashTranches.map { it.withdrawalId }.toSet()
                    if (withdrawalIds.isNotEmpty() && withdrawalIds != cachedWithdrawalIds) {
                        cachedWithdrawalIds = withdrawalIds
                        cachedWithdrawals = try {
                            getCashWithdrawalsFlowUseCase(expense.groupId)
                                .first()
                                .filter { it.id in withdrawalIds }
                                .associateBy { it.id }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to fetch withdrawals for expense $expenseId")
                            emptyMap()
                        }
                    } else if (withdrawalIds.isEmpty()) {
                        cachedWithdrawalIds = emptySet()
                        cachedWithdrawals = emptyMap()
                    }

                    val needsSubunits = cachedWithdrawals.values.any {
                        it.withdrawalScope == PayerType.SUBUNIT && !it.subunitId.isNullOrBlank()
                    } ||
                        expense.splits.any { !it.subunitId.isNullOrBlank() }

                    if (needsSubunits && cachedSubunitGroupId != expense.groupId) {
                        cachedSubunitGroupId = expense.groupId
                        cachedSubunits = try {
                            getGroupSubunitsUseCase(expense.groupId)
                                .associate { it.id to it.name }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to fetch subunits for expense $expenseId")
                            emptyMap()
                        }
                    } else if (!needsSubunits) {
                        cachedSubunitGroupId = ""
                        cachedSubunits = emptyMap()
                    }

                    val uiModel = expenseDetailUiMapper.map(
                        expense = expense,
                        memberProfiles = cachedProfiles,
                        currentUserId = currentUserId,
                        withdrawalLookup = cachedWithdrawals,
                        subunitNameLookup = cachedSubunits
                    )

                    groupFlow.map { group ->
                        ExpenseDetailUiState(
                            expense = uiModel,
                            isLoading = false,
                            isGroupArchived = group?.status == GroupStatus.ARCHIVED
                        )
                    }
                }
        }
        .catch { e ->
            Timber.e(e, "Fatal error in ExpenseDetailViewModel flow")
            _actions.send(
                ExpenseDetailUiAction.ShowError(
                    UiText.StringResource(R.string.expense_detail_error_loading)
                )
            )
            emit(ExpenseDetailUiState(isLoading = false, hasError = true))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = ExpenseDetailUiState()
        )

    fun setExpenseId(expenseId: String) {
        if (expenseId != _expenseId.value) {
            _expenseId.value = expenseId
        }
    }

    fun onEvent(event: ExpenseDetailUiEvent) {
        when (event) {
            ExpenseDetailUiEvent.DeleteConfirmed -> handleDelete()
            ExpenseDetailUiEvent.RetryReceiptDownload -> handleRetryReceiptDownload()
            ExpenseDetailUiEvent.CancelConfirmed -> handleCancelReservation()
        }
    }

    private fun handleDelete() {
        val expense = uiState.value.expense ?: return
        viewModelScope.launch {
            try {
                deleteExpenseUseCase(expense.groupId, expense.id)
                _actions.send(
                    ExpenseDetailUiAction.DeleteSuccess(
                        UiText.StringResource(R.string.expense_deleted_successfully)
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete expense: ${expense.id}")
                _actions.send(
                    ExpenseDetailUiAction.ShowError(
                        UiText.StringResource(R.string.error_deleting_expense)
                    )
                )
            }
        }
    }

    private fun handleCancelReservation() {
        val expenseUi = uiState.value.expense ?: return
        viewModelScope.launch {
            try {
                val domainExpense = getExpenseByIdFlowUseCase(expenseUi.id).first() ?: return@launch
                val updatedExpense = domainExpense.copy(
                    paymentStatus = PaymentStatus.CANCELLED
                )
                updateExpenseUseCase(
                    groupId = expenseUi.groupId,
                    expense = updatedExpense,
                    pairedContributionScope = PayerType.USER,
                    pairedSubunitId = null
                ).onSuccess {
                    _actions.send(
                        ExpenseDetailUiAction.CancelSuccess(
                            UiText.StringResource(R.string.expense_cancelled_successfully)
                        )
                    )
                }.onFailure { e ->
                    Timber.e(e, "Failed to cancel reservation expense: ${expenseUi.id}")
                    _actions.send(
                        ExpenseDetailUiAction.ShowError(
                            UiText.StringResource(R.string.error_cancelling_expense)
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error cancelling reservation expense: ${expenseUi.id}")
                _actions.send(
                    ExpenseDetailUiAction.ShowError(
                        UiText.StringResource(R.string.error_cancelling_expense)
                    )
                )
            }
        }
    }

    private fun handleRetryReceiptDownload() {
        val expenseId = _expenseId.value
        if (expenseId.isBlank()) return
        failedDownloads.remove(expenseId)
        viewModelScope.launch {
            try {
                val expense = getExpenseByIdFlowUseCase(expenseId).first()
                if (expense != null) {
                    val attachment = expense.receiptAttachment
                    if (shouldDownloadPdf(attachment)) {
                        triggerReceiptDownload(expense.id, attachment?.remoteUrl.orEmpty())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to retry receipt download for expense $expenseId")
            }
        }
    }

    private fun triggerReceiptDownload(expenseId: String, remoteUrl: String) {
        if (downloadJobs.containsKey(expenseId)) {
            return
        }
        downloadJobs[expenseId] = viewModelScope.launch {
            try {
                val result = downloadReceiptUseCase(expenseId, remoteUrl)
                result.onFailure { e ->
                    val isTerminal = when (e) {
                        is TerminalDownloadException -> e.responseCode in 400..499
                        else -> false
                    }
                    if (isTerminal) {
                        failedDownloads.add(expenseId)
                    }
                    _actions.send(
                        ExpenseDetailUiAction.ShowError(
                            UiText.StringResource(R.string.expense_detail_receipt_download_failed)
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to download receipt")
                failedDownloads.add(expenseId)
                _actions.send(
                    ExpenseDetailUiAction.ShowError(
                        UiText.StringResource(R.string.expense_detail_receipt_download_failed)
                    )
                )
            } finally {
                downloadJobs.remove(expenseId)
            }
        }
    }

    private fun shouldDownloadPdf(attachment: ReceiptAttachment?): Boolean {
        if (attachment == null) return false
        val isPdf = attachment.mimeType == "application/pdf"
        val hasNoLocal = attachment.localUri.isBlank()
        val hasRemote = !attachment.remoteUrl.isNullOrBlank()
        return isPdf && hasNoLocal && hasRemote
    }
}
