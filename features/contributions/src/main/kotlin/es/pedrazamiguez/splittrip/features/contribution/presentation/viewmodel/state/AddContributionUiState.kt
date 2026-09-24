package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.SubunitOptionUiModel
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Contribution
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AddContributionUiState(
    val isLoading: Boolean = false,
    val contributionId: String? = null,
    val originalContribution: Contribution? = null,
    val amountInput: String = "",
    val amountError: Boolean = false,
    val groupCurrencyCode: String = "",
    val groupCurrencySymbol: String = "",
    val groupCurrencyDecimalPlaces: Int = UiConstants.DEFAULT_MAX_DECIMAL_PLACES,
    val formattedAmountWithCurrency: String = "",
    val subunitOptions: ImmutableList<SubunitOptionUiModel> = persistentListOf(),
    val contributionScope: PayerType = PayerType.USER,
    val selectedSubunitId: String? = null,

    // ── Member picker (impersonation) ─────────────────────────────────
    val groupMembers: ImmutableList<MemberOptionUiModel> = persistentListOf(),
    val selectedMemberId: String? = null,
    val selectedMemberDisplayName: String = "",

    // ── Wizard ──────────────────────────────────────────────────────────
    val contributionDateMillis: Long = System.currentTimeMillis(),
    val formattedContributionDate: String = "",
    val currentStep: AddContributionStep = AddContributionStep.AMOUNT,
    val initialFormSnapshot: AddContributionFormSnapshot? = null
) {
    val isEditMode: Boolean get() = contributionId != null

    val isDirty: Boolean
        get() = initialFormSnapshot != null && toFormSnapshot() != initialFormSnapshot

    fun toFormSnapshot() = AddContributionFormSnapshot(
        amountInput = amountInput,
        contributionScope = contributionScope,
        selectedSubunitId = selectedSubunitId,
        selectedMemberId = selectedMemberId,
        contributionDateMillis = contributionDateMillis
    )

    val steps: List<AddContributionStep>
        get() = AddContributionStep.entries

    val currentStepIndex: Int
        get() = steps.indexOf(currentStep).coerceAtLeast(0)

    val canGoNext: Boolean
        get() = currentStepIndex < steps.lastIndex

    val isOnReviewStep: Boolean
        get() = currentStep == AddContributionStep.REVIEW

    val isCurrentStepValid: Boolean
        get() = when (currentStep) {
            AddContributionStep.AMOUNT -> amountInput.isNotBlank() && !amountError
            AddContributionStep.SCOPE -> true // always has a default selection
            AddContributionStep.DATE -> true
            AddContributionStep.REVIEW ->
                amountInput.isNotBlank() && !amountError
        }
}

data class AddContributionFormSnapshot(
    val amountInput: String,
    val contributionScope: PayerType,
    val selectedSubunitId: String?,
    val selectedMemberId: String?,
    val contributionDateMillis: Long
)
