package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.SubunitOptionUiModel
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.contribution.R
import es.pedrazamiguez.splittrip.features.contribution.presentation.mapper.AddContributionUiMapper
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.action.AddContributionUiAction
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Handles group configuration loading and member selection events.
 *
 * Owns [loadGroupConfig], [handleMemberSelected], and the cached
 * [groupCurrency] / [allSubunits] fields that other handlers may need.
 */
class ContributionConfigHandler(
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    private val getGroupSubunitsUseCase: GetGroupSubunitsUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val authenticationService: AuthenticationService,
    private val addContributionUiMapper: AddContributionUiMapper,
    private val appConfigService: AppConfigService
) : AddContributionEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddContributionUiState>
    private lateinit var _actions: MutableSharedFlow<AddContributionUiAction>
    private lateinit var scope: CoroutineScope

    /** Cached subunits for re-filtering on member change without re-fetching. */
    private var allSubunits: List<Subunit> = emptyList()

    /** Current group currency code — set synchronously from SharedViewModel. */
    var groupCurrency: String = appConfigService.defaultCurrencyCode.value
        private set

    /** The currently loaded group ID — used to avoid redundant reloads. */
    private var loadedGroupId: String? = null

    /** Tracks the in-flight config load job so it can be cancelled on group change. */
    private var loadConfigJob: Job? = null

    override fun bind(
        stateFlow: MutableStateFlow<AddContributionUiState>,
        actionsFlow: MutableSharedFlow<AddContributionUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    /**
     * Sets the group currency **synchronously** from the SharedViewModel-provided value.
     *
     * This is called from the Feature's `LaunchedEffect` immediately, before
     * [loadGroupConfig] completes, ensuring the currency symbol is visible on frame 1.
     */
    fun setGroupCurrency(currency: String?) {
        val resolvedCurrency = currency ?: appConfigService.defaultCurrencyCode.value
        groupCurrency = resolvedCurrency
        val symbol = addContributionUiMapper.resolveCurrencySymbol(resolvedCurrency)
        _uiState.update {
            it.copy(
                groupCurrencyCode = resolvedCurrency,
                groupCurrencySymbol = symbol
            )
        }
    }

    /**
     * Loads group configuration (members, subunits) asynchronously.
     *
     * The currency symbol is already set via [setGroupCurrency] before this is called,
     * so only member/subunit data is loaded here.
     */
    fun loadGroupConfig(groupId: String?) {
        if (groupId == null || groupId == loadedGroupId) return

        loadConfigJob?.cancel()
        loadConfigJob = scope.launch {
            try {
                val group = getGroupByIdUseCase(groupId)
                val currency = group?.currency ?: appConfigService.defaultCurrencyCode.value
                groupCurrency = currency

                val currentUserId = authenticationService.currentUserId()
                allSubunits = getGroupSubunitsUseCase(groupId)

                val memberProfiles = getMemberProfilesUseCase(group?.members ?: emptyList())
                val memberOptions = addContributionUiMapper.toMemberOptions(
                    memberIds = group?.members ?: emptyList(),
                    memberProfiles = memberProfiles,
                    currentUserId = currentUserId
                )

                val selectedMemberId = currentUserId
                val subunitOptions = filterSubunitsForMember(selectedMemberId)

                loadedGroupId = groupId

                _uiState.update {
                    it.copy(
                        groupMembers = memberOptions,
                        selectedMemberId = selectedMemberId,
                        selectedMemberDisplayName = addContributionUiMapper.resolveDisplayName(
                            selectedMemberId,
                            memberOptions
                        ),
                        subunitOptions = subunitOptions,
                        contributionScope = PayerType.USER,
                        selectedSubunitId = null,
                        amountInput = "",
                        amountError = false,
                        groupCurrencyCode = currency,
                        groupCurrencySymbol = addContributionUiMapper.resolveCurrencySymbol(
                            currency
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load group config for group %s", groupId)
                _uiState.update {
                    it.copy(
                        contributionScope = PayerType.USER,
                        selectedSubunitId = null
                    )
                }
                _actions.emit(
                    AddContributionUiAction.ShowError(
                        UiText.StringResource(R.string.contribution_add_money_error)
                    )
                )
            }
        }
    }

    fun handleMemberSelected(userId: String) {
        val subunitOptions = filterSubunitsForMember(userId)

        _uiState.update {
            it.copy(
                selectedMemberId = userId,
                selectedMemberDisplayName = addContributionUiMapper.resolveDisplayName(
                    userId,
                    it.groupMembers
                ),
                subunitOptions = subunitOptions,
                contributionScope = PayerType.USER,
                selectedSubunitId = null
            )
        }
    }

    /**
     * Filters cached subunits for the given member, returning only subunits
     * the member belongs to.
     *
     * Public so the ViewModel can call this when a different member is selected
     * (re-filter without re-fetching from the use case).
     */
    fun filterSubunitsForMember(
        memberId: String?
    ): ImmutableList<SubunitOptionUiModel> =
        allSubunits
            .filter { memberId != null && memberId in it.memberIds }
            .map { SubunitOptionUiModel(id = it.id, name = it.name) }
            .toImmutableList()
}
