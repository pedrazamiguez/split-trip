package es.pedrazamiguez.splittrip.features.settlement.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripSpacing
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButtonDefaults
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementCardAction
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementCardActionLayoutMode
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.resolveSettlementCardActionLayoutMode

@Composable
internal fun SettlementCardActionRow(
    actions: List<SettlementCardAction>,
    modifier: Modifier = Modifier
) {
    if (actions.isEmpty()) return

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val labelStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    val textMeasurer = rememberTextMeasurer()

    val renderAction: @Composable (SettlementCardAction, PaddingValues, Modifier) -> Unit =
        { action, contentPadding, buttonModifier ->
            if (action.isPrimary) {
                GradientButton(
                    text = action.label,
                    onClick = action.onClick,
                    enabled = action.isEnabled,
                    contentPadding = contentPadding,
                    modifier = buttonModifier
                )
            } else {
                SecondaryButton(
                    text = action.label,
                    onClick = action.onClick,
                    enabled = action.isEnabled,
                    contentPadding = contentPadding,
                    modifier = buttonModifier
                )
            }
        }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layoutMode = computeSettlementCardActionLayoutMode(
            actions = actions,
            availableWidthPx = constraints.maxWidth,
            labelStyle = labelStyle,
            textMeasurer = textMeasurer,
            density = density,
            layoutDirection = layoutDirection
        )
        val contentPadding = layoutMode.resolveContentPadding()
        if (layoutMode == SettlementCardActionLayoutMode.COLUMN_STACKED) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                actions.forEach { action ->
                    renderAction(action, contentPadding, Modifier.fillMaxWidth())
                }
            }
        } else {
            val isMultiAction = actions.size > 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                val itemModifier = if (isMultiAction) Modifier.weight(1f) else Modifier.fillMaxWidth()
                actions.forEach { action ->
                    renderAction(action, contentPadding, itemModifier)
                }
            }
        }
    }
}

private fun SettlementCardActionLayoutMode.resolveContentPadding(): PaddingValues =
    if (this == SettlementCardActionLayoutMode.ROW_COMPACT) {
        GradientButtonDefaults.CompactContentPadding
    } else {
        GradientButtonDefaults.ContentPadding
    }

private fun computeSettlementCardActionLayoutMode(
    actions: List<SettlementCardAction>,
    availableWidthPx: Int,
    labelStyle: TextStyle,
    textMeasurer: TextMeasurer,
    density: Density,
    layoutDirection: LayoutDirection
): SettlementCardActionLayoutMode {
    val maxLabelWidthPx = actions.maxOf { action ->
        textMeasurer.measure(
            text = action.label,
            style = labelStyle,
            maxLines = 1,
            softWrap = false
        ).size.width
    }
    val spacingPx = with(density) { SplitTripSpacing.Small.roundToPx() }
    val standardPaddingPx = GradientButtonDefaults.ContentPadding.horizontalPx(density, layoutDirection)
    val compactPaddingPx = GradientButtonDefaults.CompactContentPadding.horizontalPx(density, layoutDirection)

    return resolveSettlementCardActionLayoutMode(
        actionCount = actions.size,
        availableWidthPx = availableWidthPx,
        spacingPx = spacingPx,
        standardRequiredButtonWidthPx = maxLabelWidthPx + standardPaddingPx,
        compactRequiredButtonWidthPx = maxLabelWidthPx + compactPaddingPx
    )
}

private fun PaddingValues.horizontalPx(density: Density, layoutDirection: LayoutDirection): Int =
    with(density) {
        (calculateLeftPadding(layoutDirection) + calculateRightPadding(layoutDirection)).roundToPx()
    }
