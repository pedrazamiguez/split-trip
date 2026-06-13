package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.features.group.presentation.component.step.GroupCurrencyStep
import es.pedrazamiguez.splittrip.features.group.presentation.component.step.GroupInfoStep
import es.pedrazamiguez.splittrip.features.group.presentation.component.step.GroupMembersStep
import es.pedrazamiguez.splittrip.features.group.presentation.component.step.GroupReviewStep
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupStep
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState

@Composable
internal fun CreateGroupWizardContent(
    uiState: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState.currentStep,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (slideInHorizontally { it * direction } + fadeIn())
                .togetherWith(slideOutHorizontally { -it * direction } + fadeOut())
                .using(SizeTransform(clip = false))
        },
        label = "createGroupWizardStep"
    ) { step ->
        val bottomPadding = LocalBottomPadding.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomPadding + UiConstants.WIZARD_NAV_BAR_HEIGHT)
        ) {
            val nextStep = { onEvent(CreateGroupUiEvent.NextStep) }
            when (step) {
                CreateGroupStep.INFO -> GroupInfoStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                CreateGroupStep.CURRENCY -> GroupCurrencyStep(uiState = uiState, onEvent = onEvent)
                CreateGroupStep.MEMBERS -> GroupMembersStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onScannerClick = onScannerClick
                )
                CreateGroupStep.REVIEW -> {
                    GroupReviewStep(uiState = uiState)
                    FormErrorBanner(
                        error = uiState.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.spacing.Large)
                            .padding(top = MaterialTheme.spacing.Small)
                    )
                }
            }
        }
    }
}
