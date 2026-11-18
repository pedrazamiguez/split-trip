package es.pedrazamiguez.splittrip.features.settlement.presentation.screen

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.InlineWarningBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.DisputeSettlementBottomBar
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.event.DisputeSettlementUiEvent
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.DisputeSettlementUiState

const val DISPUTE_REASON_INPUT_TEST_TAG = "dispute_reason_input"

@Composable
fun DisputeSettlementScreen(
    uiState: DisputeSettlementUiState,
    onEvent: (DisputeSettlementUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    attachmentContent: (@Composable () -> Unit)? = null
) {
    val topPadding = LocalTopPadding.current
    val bottomPadding = LocalBottomPadding.current
    val spacing = MaterialTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(
                start = spacing.ExtraLarge,
                end = spacing.ExtraLarge,
                top = spacing.ExtraLarge + topPadding,
                bottom = spacing.ExtraLarge + bottomPadding
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.Medium)
        ) {
            if (uiState.isOffline) {
                InlineWarningBanner(
                    warning = UiText.StringResource(R.string.your_balance_offline_warning)
                )
            }

            StyledOutlinedTextField(
                value = uiState.reason,
                onValueChange = { onEvent(DisputeSettlementUiEvent.ReasonChanged(it)) },
                placeholder = stringResource(R.string.your_balance_dispute_dialog_hint),
                singleLine = false,
                minLines = 5,
                maxLines = 10,
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DISPUTE_REASON_INPUT_TEST_TAG)
            )
        }

        DisputeSettlementBottomBar(
            isSubmitEnabled = uiState.isSubmitEnabled,
            isSubmitting = uiState.isSubmitting,
            onCancelClick = { onEvent(DisputeSettlementUiEvent.NavigateBackClicked) },
            onSubmitClick = { onEvent(DisputeSettlementUiEvent.SubmitClicked) },
            attachmentContent = attachmentContent
        )
    }
}
