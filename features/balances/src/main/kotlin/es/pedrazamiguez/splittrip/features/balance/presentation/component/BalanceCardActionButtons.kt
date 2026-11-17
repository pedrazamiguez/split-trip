package es.pedrazamiguez.splittrip.features.balance.presentation.component

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripSpacing
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CashBanknote
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plus
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButtonDefaults
import es.pedrazamiguez.splittrip.core.designsystem.transition.fabSharedTransitionModifier
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ActionButtonsLayoutMode
import es.pedrazamiguez.splittrip.features.balance.presentation.model.resolveActionButtonsLayoutMode

@Composable
internal fun BalanceCardActionButtons(
    onAddMoney: () -> Unit,
    onWithdrawCash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addMoneyText = stringResource(R.string.balances_add_money)
    val withdrawText = stringResource(R.string.balances_withdraw_cash)
    val addMoneySharedModifier = fabSharedTransitionModifier(SharedElementKeys.CONTRIBUTION_WIZARD)
    val withdrawSharedModifier = fabSharedTransitionModifier(SharedElementKeys.ADD_CASH_WITHDRAWAL)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val labelStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layoutMode = computeLayoutMode(
            availableWidthPx = constraints.maxWidth,
            addMoneyText = addMoneyText,
            withdrawText = withdrawText,
            labelStyle = labelStyle,
            textMeasurer = textMeasurer,
            density = density,
            layoutDirection = layoutDirection
        )
        val contentPadding = if (layoutMode == ActionButtonsLayoutMode.ROW_COMPACT) {
            GradientButtonDefaults.CompactContentPadding
        } else {
            GradientButtonDefaults.ContentPadding
        }
        val renderButtons: @Composable (Modifier) -> Unit = { buttonModifier ->
            GradientButton(
                text = addMoneyText,
                onClick = onAddMoney,
                leadingIcon = TablerIcons.Outline.Plus,
                contentPadding = contentPadding,
                modifier = buttonModifier.then(addMoneySharedModifier)
            )
            GradientButton(
                text = withdrawText,
                onClick = onWithdrawCash,
                colors = GradientButtonDefaults.secondaryColors(),
                leadingIcon = TablerIcons.Outline.CashBanknote,
                contentPadding = contentPadding,
                modifier = buttonModifier.then(withdrawSharedModifier)
            )
        }
        if (layoutMode == ActionButtonsLayoutMode.COLUMN_STACKED) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) { renderButtons(Modifier.fillMaxWidth()) }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) { renderButtons(Modifier.weight(1f)) }
        }
    }
}

private fun computeLayoutMode(
    availableWidthPx: Int,
    addMoneyText: String,
    withdrawText: String,
    labelStyle: TextStyle,
    textMeasurer: TextMeasurer,
    density: Density,
    layoutDirection: LayoutDirection
): ActionButtonsLayoutMode {
    val maxLabelWidthPx = maxOf(
        textMeasurer.measure(text = addMoneyText, style = labelStyle, maxLines = 1, softWrap = false).size.width,
        textMeasurer.measure(text = withdrawText, style = labelStyle, maxLines = 1, softWrap = false).size.width
    )
    val chromeWidthPx = with(density) { (GradientButtonDefaults.IconSize + SplitTripSpacing.Small).roundToPx() }
    val spacingPx = with(density) { SplitTripSpacing.Medium.roundToPx() }
    val standardPaddingPx = GradientButtonDefaults.ContentPadding.horizontalPx(density, layoutDirection)
    val compactPaddingPx = GradientButtonDefaults.CompactContentPadding.horizontalPx(density, layoutDirection)

    return resolveActionButtonsLayoutMode(
        availableWidthPx = availableWidthPx,
        spacingPx = spacingPx,
        standardRequiredButtonWidthPx = maxLabelWidthPx + chromeWidthPx + standardPaddingPx,
        compactRequiredButtonWidthPx = maxLabelWidthPx + chromeWidthPx + compactPaddingPx
    )
}

private fun PaddingValues.horizontalPx(density: Density, layoutDirection: LayoutDirection): Int =
    with(density) {
        (calculateLeftPadding(layoutDirection) + calculateRightPadding(layoutDirection)).roundToPx()
    }
