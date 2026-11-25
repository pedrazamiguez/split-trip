package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.horizonGlassEffect
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ArrowLeft
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ArrowRight
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton

private const val SCRIM_MID_STOP = 0.25f
private const val SCRIM_MID_ALPHA = 0.90f

/**
 * Single-row fixed-bottom navigation bar for a multi-step wizard.
 *
 * Domain-agnostic: all configuration is passed via [WizardNavigationBarConfig] so this
 * component can be reused across any feature (`AddExpense`, `AddCashWithdrawal`, `AddContribution`,
 * `CreateEditGroup`, `CreateEditSubunit`, and `GroupSettlementOverview`).
 *
 * **Backdrop Scrim:** Renders over a frosted-glass [horizonGlassEffect] (when [hazeState] is provided)
 * plus a vertical tonal surface gradient wash (`0% → 90% → 100%`) so scrolled form content never
 * bleeds behind the `Back` / `Next` / `Submit` action buttons.
 *
 * **Positioning:** Designed to be placed as a fixed-bottom overlay inside `WizardScaffold`.
 * The keyboard slides *over* the bar — the buttons never move. Scrollable step content reserves
 * bottom padding via [rememberWizardNavBarPadding].
 *
 * @param config            Combined state and labels for the navigation bar.
 * @param onBack            Called when the Back button is tapped.
 * @param onNext            Called when the Next button is tapped.
 * @param onSubmit          Called when the Submit button is tapped.
 * @param modifier          Optional modifier applied to the root container.
 * @param hazeState         Optional [HazeState] shared with the step content for frosted-glass blur.
 * @param applyWindowInsets When `true` (default when `BottomNavigationBar` is hidden and
 *                          `LocalBottomPadding.current == 0.dp`), applies [navigationBarsPadding]
 *                          so the bar clears the system navigation bar.
 */
@Composable
fun WizardNavigationBar(
    config: WizardNavigationBarConfig,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    applyWindowInsets: Boolean = LocalBottomPadding.current == 0.dp
) {
    val insetModifier = if (applyWindowInsets) {
        Modifier.windowInsetsPadding(
            WindowInsets.navigationBars
                .union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        WizardNavigationBarScrim(
            hazeState = hazeState,
            modifier = Modifier.matchParentSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(insetModifier)
                .padding(
                    horizontal = MaterialTheme.spacing.Large,
                    vertical = MaterialTheme.spacing.Medium
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            SecondaryButton(
                text = config.backLabel,
                onClick = onBack,
                leadingIcon = TablerIcons.Outline.ArrowLeft,
                modifier = Modifier.weight(1f)
            )
            WizardForwardButton(
                config = config,
                onNext = onNext,
                onSubmit = onSubmit,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WizardNavigationBarScrim(
    hazeState: HazeState?,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val glassModifier = if (hazeState != null) {
        Modifier.horizonGlassEffect(hazeState = hazeState) {
            mask(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black, Color.Black)
                )
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(glassModifier)
            .background(
                Brush.verticalGradient(
                    0f to surfaceColor.copy(alpha = 0f),
                    SCRIM_MID_STOP to surfaceColor.copy(alpha = SCRIM_MID_ALPHA),
                    1f to surfaceColor
                )
            )
    )
}

@Composable
private fun WizardForwardButton(
    config: WizardNavigationBarConfig,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (config.isOnLastStep) {
        GradientButton(
            text = config.submitLabel,
            onClick = onSubmit,
            enabled = config.isCurrentStepValid,
            isLoading = config.isLoading,
            modifier = modifier
        )
    } else {
        GradientButton(
            text = config.nextLabel,
            onClick = onNext,
            enabled = config.isCurrentStepValid && config.canGoNext,
            trailingIcon = TablerIcons.Outline.ArrowRight,
            modifier = modifier
        )
    }
}

/**
 * Returns the bottom content clearance required for [WizardNavigationBar], including
 * system `navigationBars` inset when the main bottom tab bar is hidden (`LocalBottomPadding == 0.dp`).
 */
@Composable
fun rememberWizardNavBarPadding(): Dp {
    val navBarInset = if (LocalBottomPadding.current == 0.dp) {
        WindowInsets.navigationBars
            .union(WindowInsets.displayCutout)
            .asPaddingValues()
            .calculateBottomPadding()
    } else {
        0.dp
    }
    return UiConstants.WIZARD_NAV_BAR_HEIGHT + navBarInset
}
