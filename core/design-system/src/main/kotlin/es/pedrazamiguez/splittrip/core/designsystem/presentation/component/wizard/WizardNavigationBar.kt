package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ArrowLeft
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ArrowRight
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.ButtonContentRow
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText

private val FAST_TRACK_SHADOW_ELEVATION = 2.dp
private val FAST_TRACK_PILL_HEIGHT = 48.dp
private const val FAST_TRACK_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val HINT_MAX_LINES = 2

/**
 * Navigation bar for a multi-step wizard.
 *
 * Domain-agnostic: all configuration is passed via [WizardNavigationBarConfig] so this
 * component can be reused across any feature (AddCashWithdrawal, AddExpense, etc.).
 *
 * Adapts automatically between Back/Next and Back/Submit on the last step, and renders an
 * optional fast-track "Skip to Review" [CircleShape] tonal pill above the button row when
 * [WizardNavigationBarConfig.skipToReviewLabel] and [onSkipToReview] are non-null and
 * `!config.isOnLastStep`. When [WizardNavigationBarConfig.optionalDefaultsHint] is non-null,
 * a subtle [CaptionText] defaults hint is rendered directly below the pill.
 *
 * **Positioning:** This bar is designed to be placed as a fixed-bottom overlay
 * inside a `Box(Modifier.align(BottomCenter))` in the wizard orchestrator.
 * The keyboard slides *over* the bar — the buttons never move. The scrollable
 * step content must reserve bottom padding equal to
 * `UiConstants.WIZARD_NAV_BAR_HEIGHT` (or `UiConstants.WIZARD_NAV_BAR_WITH_SKIP_HEIGHT`
 * when the fast-track pill is visible) so the last items are not obscured.
 *
 * All buttons delegate to the design-system's canonical button components
 * ([SecondaryButton], [GradientButton], [ButtonContentRow]) which provide consistent
 * pill geometry, typography, and tonal hierarchy across the app.
 *
 * @param config         Combined state and labels for the navigation bar.
 * @param onBack         Called when the Back button is tapped.
 * @param onNext         Called when the Next button is tapped.
 * @param onSubmit       Called when the Submit button is tapped.
 * @param modifier       Optional modifier applied to the root container.
 * @param onSkipToReview Optional callback invoked when the fast-track "Skip to Review" pill
 *                       is tapped.
 */
@Composable
fun WizardNavigationBar(
    config: WizardNavigationBarConfig,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    onSkipToReview: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.Large,
                vertical = MaterialTheme.spacing.Medium
            )
    ) {
        WizardFastTrackSection(
            skipToReviewLabel = config.skipToReviewLabel,
            optionalDefaultsHint = config.optionalDefaultsHint,
            isVisible = config.skipToReviewLabel != null &&
                onSkipToReview != null &&
                !config.isOnLastStep,
            enabled = !config.isLoading,
            onSkipToReview = { onSkipToReview?.invoke() }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
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
private fun WizardFastTrackSection(
    skipToReviewLabel: String?,
    optionalDefaultsHint: String?,
    isVisible: Boolean,
    enabled: Boolean,
    onSkipToReview: () -> Unit
) {
    // Cache last non-null label/hint while visible so exit animation never blanks mid-fade.
    var displayedLabel by remember { mutableStateOf(skipToReviewLabel) }
    var displayedHint by remember { mutableStateOf(optionalDefaultsHint) }
    if (isVisible) {
        displayedLabel = skipToReviewLabel
        displayedHint = optionalDefaultsHint
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.spacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
        ) {
            displayedLabel?.let { label ->
                WizardFastTrackPill(
                    label = label,
                    enabled = enabled,
                    onClick = onSkipToReview
                )
            }
            displayedHint?.let { hint ->
                CaptionText(
                    text = hint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = HINT_MAX_LINES,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WizardFastTrackPill(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (enabled) {
        colorScheme.primary.copy(alpha = FAST_TRACK_CONTAINER_ALPHA).compositeOver(colorScheme.surface)
    } else {
        colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA).compositeOver(colorScheme.surface)
    }
    val contentColor = if (enabled) {
        colorScheme.primary
    } else {
        colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    }
    val shadowElevation = if (enabled && !isSystemInDarkTheme()) {
        FAST_TRACK_SHADOW_ELEVATION
    } else {
        0.dp
    }

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(FAST_TRACK_PILL_HEIGHT)
            .shadow(elevation = shadowElevation, shape = CircleShape)
            .clip(CircleShape)
            .background(color = containerColor, shape = CircleShape)
            .debouncedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.ExtraLarge),
            contentAlignment = Alignment.Center
        ) {
            ButtonContentRow(
                text = label,
                contentColor = contentColor,
                trailingIcon = TablerIcons.Outline.ArrowRight
            )
        }
    }
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
 * Animates bottom content clearance for [WizardNavigationBar] between its standard height and
 * [UiConstants.WIZARD_NAV_BAR_WITH_SKIP_HEIGHT] when [hasSkipToReview] is `true`.
 */
@Composable
fun rememberWizardNavBarPadding(hasSkipToReview: Boolean): Dp {
    val padding by animateDpAsState(
        targetValue = if (hasSkipToReview) {
            UiConstants.WIZARD_NAV_BAR_WITH_SKIP_HEIGHT
        } else {
            UiConstants.WIZARD_NAV_BAR_HEIGHT
        },
        label = "wizardNavBarPadding"
    )
    return padding
}
