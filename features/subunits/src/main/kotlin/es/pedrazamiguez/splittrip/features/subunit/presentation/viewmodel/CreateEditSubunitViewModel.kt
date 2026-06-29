package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigator
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.exception.ValidationException
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.SubunitShareDistributionService
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.CreateSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.UpdateSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.mapper.SubunitUiMapper
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.MemberUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.action.CreateEditSubunitUiAction
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the Create/Edit Subunit screen.
 *
 * Loads group members and existing subunits to build the member selection list
 * with assignment awareness. Handles form state for creating or editing a subunit.
 *
 * Delegates share parsing and distribution to [SubunitShareDistributionService]
 * (pure percentage math, no amountCents coupling) and share formatting to
 * [SubunitUiMapper] (locale-aware display).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateEditSubunitViewModel(
    private val createSubunitUseCase: CreateSubunitUseCase,
    private val updateSubunitUseCase: UpdateSubunitUseCase,
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    private val getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val subunitUiMapper: SubunitUiMapper,
    private val shareDistributionService: SubunitShareDistributionService
) : ViewModel() {

    private data class InitParams(val groupId: String, val subunitId: String?)
    private val _initParams = MutableStateFlow<InitParams?>(null)

    private val _formState = MutableStateFlow(FormState())

    // Holds the computed available members from the data layer
    private val _availableMembers = MutableStateFlow<ImmutableList<MemberUiModel>>(persistentListOf())
    private val _dataLoaded = MutableStateFlow(false)

    private val _actions = MutableSharedFlow<CreateEditSubunitUiAction>()
    val actions: SharedFlow<CreateEditSubunitUiAction> = _actions.asSharedFlow()

    private val wizardNavigator = WizardNavigator()

    val uiState: StateFlow<CreateEditSubunitUiState> = combine(
        _dataLoaded,
        _formState,
        _availableMembers
    ) { dataLoaded, form, availableMembers ->
        if (!dataLoaded) {
            CreateEditSubunitUiState()
        } else {
            val params = _initParams.value
            CreateEditSubunitUiState(
                isLoading = false,
                isSaving = form.isSaving,
                isEditing = params?.subunitId != null,
                name = form.name,
                selectedMemberIds = form.selectedMemberIds.toImmutableList(),
                memberShares = form.memberShares,
                lockedMemberIds = form.lockedMemberIds.toImmutableSet(),
                availableMembers = availableMembers,
                nameError = form.nameError,
                membersError = form.membersError,
                sharesError = form.sharesError,
                currentStep = form.currentStep
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = CreateEditSubunitUiState()
    )

    init {
        // Observe init params and load data reactively
        viewModelScope.launch {
            _initParams
                .filter { it != null }
                .flatMapLatest { params ->
                    val groupId = params!!.groupId
                    val subunitId = params.subunitId

                    getGroupSubunitsFlowUseCase(groupId)
                        .map { subunits ->
                            val group = getGroupByIdUseCase(groupId)
                            val memberIds = group?.members ?: emptyList()
                            val memberProfiles = getMemberProfilesUseCase(memberIds)

                            // Pre-fill form for edit mode (only on first load)
                            val editingSubunit = subunitId?.let { id -> subunits.find { it.id == id } }
                            if (editingSubunit != null && !_formState.value.initialized) {
                                _formState.update {
                                    FormState(
                                        initialized = true,
                                        name = editingSubunit.name,
                                        selectedMemberIds = editingSubunit.memberIds,
                                        memberShares = editingSubunit.memberShares.mapValues { (_, share) ->
                                            subunitUiMapper.formatShareAsPercentage(share)
                                        }
                                    )
                                }
                            } else if (!_formState.value.initialized) {
                                _formState.update { it.copy(initialized = true) }
                            }

                            subunitUiMapper.toMemberUiModelList(
                                memberIds = memberIds,
                                memberProfiles = memberProfiles,
                                subunits = subunits,
                                excludeSubunitId = subunitId
                            )
                        }
                }
                .collect { availableMembers ->
                    _availableMembers.value = availableMembers
                    _dataLoaded.value = true
                }
        }
    }

    fun init(groupId: String, subunitId: String?) {
        val newParams = InitParams(groupId, subunitId)
        if (newParams != _initParams.value) {
            _initParams.value = newParams
        }
    }

    fun onEvent(event: CreateEditSubunitUiEvent) {
        when (event) {
            is CreateEditSubunitUiEvent.UpdateName -> updateName(event.name)
            is CreateEditSubunitUiEvent.ToggleMember -> toggleMember(event.userId)
            is CreateEditSubunitUiEvent.UpdateMemberShare -> updateMemberShare(event.userId, event.share)
            is CreateEditSubunitUiEvent.ToggleShareLock -> toggleShareLock(event.userId)
            CreateEditSubunitUiEvent.Save -> save()
            CreateEditSubunitUiEvent.NextStep -> handleNextStep()
            CreateEditSubunitUiEvent.PreviousStep -> handlePreviousStep()
            is CreateEditSubunitUiEvent.JumpToStep -> handleJumpToStep(event.stepIndex)
        }
    }

    private fun handleNextStep() {
        _formState.update { form ->
            // Validate current step before advancing
            val stepError = validateCurrentStep(form)
            if (stepError != null) return@update stepError

            val nextStep = wizardNavigator.navigateNext(form.currentStep, CreateEditSubunitStep.entries)
                ?: return@update form
            form.copy(currentStep = nextStep, nameError = null, membersError = null, sharesError = null)
        }
    }

    /**
     * Validates the current step and returns the updated [FormState] with an error
     * if validation fails, or `null` if the step is valid and can advance.
     */
    private fun validateCurrentStep(form: FormState): FormState? = when (form.currentStep) {
        CreateEditSubunitStep.SHARES -> validateSharesStep(form)
        else -> null
    }

    /**
     * Delegates share-text validation to the domain service and maps the result
     * to a presentation-layer error (or `null` when valid).
     */
    private fun validateSharesStep(form: FormState): FormState? {
        val result = shareDistributionService.validateShareTexts(
            selectedMemberIds = form.selectedMemberIds,
            memberShareTexts = form.memberShares
        )
        val error = when (result) {
            SubunitShareDistributionService.ShareTextValidation.Valid -> return null
            SubunitShareDistributionService.ShareTextValidation.Unparseable ->
                UiText.StringResource(R.string.subunit_error_validation_failed)
            SubunitShareDistributionService.ShareTextValidation.OutOfRange ->
                UiText.StringResource(R.string.subunit_error_share_out_of_range)
            SubunitShareDistributionService.ShareTextValidation.SumMismatch ->
                UiText.StringResource(R.string.subunit_error_shares_dont_sum)
        }
        return form.copy(sharesError = error)
    }

    private fun handlePreviousStep() {
        val form = _formState.value
        when (val result = wizardNavigator.navigatePrevious(form.currentStep, null, CreateEditSubunitStep.entries)) {
            is WizardNavigator.NavigationResult.WithStep ->
                _formState.update {
                    it.copy(currentStep = result.step, nameError = null, membersError = null, sharesError = null)
                }

            WizardNavigator.NavigationResult.ExitWizard ->
                viewModelScope.launch { _actions.emit(CreateEditSubunitUiAction.NavigateBack) }
        }
    }

    /**
     * Jumps directly to a previously completed step at [stepIndex].
     * Clears all step-level validation errors so the destination renders cleanly,
     * matching the behaviour of [handlePreviousStep].
     */
    private fun handleJumpToStep(stepIndex: Int) {
        val target =
            wizardNavigator.jumpToStep(_formState.value.currentStep, stepIndex, CreateEditSubunitStep.entries) ?: return
        _formState.update {
            it.copy(currentStep = target, nameError = null, membersError = null, sharesError = null)
        }
    }

    private fun updateName(name: String) {
        _formState.update { it.copy(name = name, nameError = null) }
    }

    private fun toggleMember(userId: String) {
        _formState.update { form ->
            val currentIds = form.selectedMemberIds
            val updatedIds = if (userId in currentIds) {
                currentIds - userId
            } else {
                currentIds + userId
            }

            // Distribute shares evenly among all selected members
            val evenShares = shareDistributionService.distributeEvenly(updatedIds)
            val updatedShares = evenShares.mapValues { (_, share) ->
                subunitUiMapper.formatShareAsPercentage(share)
            }

            form.copy(
                selectedMemberIds = updatedIds,
                memberShares = updatedShares,
                lockedMemberIds = emptySet(),
                membersError = null
            )
        }
    }

    private fun updateMemberShare(userId: String, share: String) {
        _formState.update { form ->
            val updatedShares = form.memberShares.toMutableMap()
            updatedShares[userId] = share

            // Auto-lock the edited member
            val updatedLocks = form.lockedMemberIds + userId

            // Parse the typed value (locale-safe) and redistribute remaining to other selected members
            val normalized = CurrencyConverter.normalizeAmountString(share)
            val parsedValue = normalized.toBigDecimalOrNull()
                ?.divide(BigDecimal("100"), 10, RoundingMode.HALF_UP)
            if (parsedValue != null) {
                // Other locked members (excluding the currently edited one)
                val otherLockedIds = (updatedLocks - userId).filter { it in form.selectedMemberIds }

                // Build locked shares map — unparseable locked shares are treated as 0 budget
                // but still excluded from redistribution (they keep their displayed text)
                val lockedSharesMap = otherLockedIds.associate { lockedId ->
                    val lockedText = updatedShares[lockedId] ?: ""
                    val lockedNorm = CurrencyConverter.normalizeAmountString(lockedText)
                    val lockedVal = lockedNorm.toBigDecimalOrNull()
                        ?.divide(BigDecimal("100"), 10, RoundingMode.HALF_UP)
                        ?: BigDecimal.ZERO
                    lockedId to lockedVal
                }

                // All other selected members — the service handles locked/unlocked
                // separation internally via filteredLockedShares + unlockedIds.
                val allOtherIds = form.selectedMemberIds.filter { it != userId }

                val redistribution = shareDistributionService.redistributeRemaining(
                    editedShare = parsedValue,
                    otherMemberIds = allOtherIds,
                    lockedShares = lockedSharesMap
                )
                redistribution.forEach { (otherId, otherShare) ->
                    updatedShares[otherId] = subunitUiMapper.formatShareAsPercentage(otherShare)
                }
            }

            form.copy(
                memberShares = updatedShares,
                lockedMemberIds = updatedLocks,
                sharesError = null
            )
        }
    }

    private fun toggleShareLock(userId: String) {
        _formState.update { form ->
            val updatedLocks = if (userId in form.lockedMemberIds) {
                form.lockedMemberIds - userId
            } else {
                form.lockedMemberIds + userId
            }
            form.copy(lockedMemberIds = updatedLocks)
        }
    }

    @Suppress("LongMethod") // Sequential save flow: validate → build domain object → persist → handle result
    private fun save() {
        val form = _formState.value
        val params = _initParams.value ?: return

        // Client-side validation for immediate feedback
        if (form.name.isBlank()) {
            _formState.update {
                it.copy(nameError = UiText.StringResource(R.string.subunit_error_name_empty))
            }
            return
        }
        if (form.selectedMemberIds.isEmpty()) {
            _formState.update {
                it.copy(membersError = UiText.StringResource(R.string.subunit_error_no_members))
            }
            return
        }

        _formState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val memberShares = shareDistributionService.parseShareTexts(
                selectedMemberIds = form.selectedMemberIds,
                memberShareTexts = form.memberShares
            )

            // If the form has non-blank share entries but parsing returned empty,
            // the input was unparseable — surface an error instead of auto-normalizing.
            val hasNonBlankShares = form.memberShares.values.any { it.isNotBlank() }
            if (memberShares.isEmpty() && hasNonBlankShares) {
                _formState.update {
                    it.copy(
                        isSaving = false,
                        sharesError = UiText.StringResource(R.string.subunit_error_validation_failed)
                    )
                }
                return@launch
            }

            val subunit = Subunit(
                id = params.subunitId ?: "",
                groupId = params.groupId,
                name = form.name.trim(),
                memberIds = form.selectedMemberIds,
                memberShares = memberShares
            )

            val result = if (params.subunitId != null) {
                updateSubunitUseCase(params.groupId, subunit)
                    .map { UiText.StringResource(R.string.subunit_updated_success) }
            } else {
                createSubunitUseCase(params.groupId, subunit)
                    .map { UiText.StringResource(R.string.subunit_created_success) }
            }

            result
                .onSuccess { message ->
                    _actions.emit(CreateEditSubunitUiAction.ShowSuccess(message))
                    _actions.emit(CreateEditSubunitUiAction.NavigateBack)
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to save subunit")
                    _formState.update { it.copy(isSaving = false) }

                    val errorMessage = when (error) {
                        is GroupArchivedException -> {
                            UiText.StringResource(
                                DesignSystemR.string.group_error_archived
                            )
                        }
                        is ValidationException -> {
                            UiText.StringResource(R.string.subunit_error_validation_failed)
                        }
                        else -> {
                            UiText.StringResource(R.string.subunit_error_save_failed)
                        }
                    }

                    _actions.emit(CreateEditSubunitUiAction.ShowError(errorMessage))
                }
        }
    }

    private data class FormState(
        val initialized: Boolean = false,
        val isSaving: Boolean = false,
        val name: String = "",
        val selectedMemberIds: List<String> = emptyList(),
        val memberShares: Map<String, String> = emptyMap(),
        val lockedMemberIds: Set<String> = emptySet(),
        val nameError: UiText? = null,
        val membersError: UiText? = null,
        val sharesError: UiText? = null,
        val currentStep: CreateEditSubunitStep = CreateEditSubunitStep.NAME
    )
}
