package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ArrowRight
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.ButtonContentRow
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText

private val FAST_TRACK_PILL_HEIGHT = 36.dp
private const val FAST_TRACK_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val HINT_MAX_LINES = 2

/**
 * Contextual fast-track header placed at the top of [WizardScrollableStepContainer]
 * (beneath [WizardStepIndicator]) when jumping directly to the review step is available.
 *
 * Renders directly on the page `surface` canvas without a card wrapper so there are no
 * nested surfaces, centering both the optional smart-defaults caption and the `"Skip to Review"`
 * action pill horizontally.
 *
 * @param label                Localised label for the fast-track action pill (e.g., `"Skip to Review"`).
 * @param onSkipToReview       Callback invoked when the fast-track pill is tapped.
 * @param modifier             Optional modifier applied to the root [Column].
 * @param optionalDefaultsHint Optional caption explaining smart defaults applied to skipped steps.
 * @param enabled              Whether the fast-track pill is interactive.
 */
@Composable
fun WizardFastTrackBanner(
    label: String,
    onSkipToReview: () -> Unit,
    modifier: Modifier = Modifier,
    optionalDefaultsHint: String? = null,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.spacing.Large,
                end = MaterialTheme.spacing.Large,
                top = MaterialTheme.spacing.Small
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        optionalDefaultsHint?.let { hint ->
            CaptionText(
                text = hint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = HINT_MAX_LINES
            )
        }
        WizardFastTrackPill(
            label = label,
            enabled = enabled,
            onClick = onSkipToReview
        )
    }
}

@Composable
private fun WizardFastTrackPill(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = FAST_TRACK_PILL_HEIGHT)
            .clip(CircleShape)
            .background(color = containerColor, shape = CircleShape)
            .debouncedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(
                horizontal = MaterialTheme.spacing.Medium,
                vertical = MaterialTheme.spacing.ExtraSmall
            )
    ) {
        ButtonContentRow(
            text = label,
            contentColor = contentColor,
            trailingIcon = TablerIcons.Outline.ArrowRight
        )
    }
}
