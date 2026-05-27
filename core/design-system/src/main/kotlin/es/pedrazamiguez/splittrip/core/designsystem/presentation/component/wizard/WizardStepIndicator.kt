package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

private const val STEP_CIRCLE_SIZE = 28
private const val CONNECTOR_HEIGHT = 3

// Vertical offset so connectors visually centre on the step circles:
// (circle_size - connector_height) / 2
private const val CONNECTOR_TOP_OFFSET = (STEP_CIRCLE_SIZE - CONNECTOR_HEIGHT) / 2

/** Maximum number of steps visible at once before scrolling kicks in. */
private const val MAX_VISIBLE_STEPS = 5

/** Fixed connector width used in the scrollable variant. */
private val SCROLLABLE_CONNECTOR_WIDTH = 16.dp

/** Horizontal padding applied to the step row. */
private val HORIZONTAL_PADDING = 20.dp

/** Dash interval for the dashed border on optional step circles (on / off). */
private const val DASH_ON_INTERVAL = 4f
private const val DASH_OFF_INTERVAL = 4f

/**
 * Horizontal step indicator for a multi-step wizard.
 *
 * When the number of steps exceeds [MAX_VISIBLE_STEPS], the indicator becomes
 * horizontally scrollable and smoothly auto-centres the current step.
 * Otherwise a static, non-scrolling row is used with weight-based connectors.
 *
 * @param stepLabels           Ordered list of localised step labels.
 * @param currentStepIndex     Zero-based index of the currently active step.
 * @param optionalStepIndices  Zero-based indices of steps that are optional (shown with
 *                             a dashed border when not yet completed).
 * @param skipToReviewLabel    When non-null **and** [onSkipToReview] is also non-null, a
 *                             "Skip to Review" text link is rendered below the step row.
 *                             Typically provided only when the current step is optional.
 *                             If either value is null, the link is hidden gracefully.
 * @param onSkipToReview       Callback invoked when the skip link is tapped. Both this
 *                             and [skipToReviewLabel] must be non-null for the link to
 *                             appear.
 * @param onStepClicked        Optional callback invoked when a **completed** step circle
 *                             is tapped, with the zero-based index of the tapped step.
 *                             When `null` (the default) all circles are non-interactive
 *                             and existing callers continue to work unchanged.
 *                             Current and future steps are never tappable regardless of
 *                             this parameter.
 *
 * **Detekt note:** `LongMethod` and `CognitiveComplexMethod` are suppressed here. The body
 * hosts an `AnimatedContent` + `AnimatedVisibility` block — both Compose DSL contracts that
 * require inline lambdas. Length and cognitive-complexity scores are structural artefacts of
 * the Compose animation API, not a sign of semantic complexity.
 */
@Suppress("LongMethod", "CognitiveComplexMethod")
@Composable
fun WizardStepIndicator(
    stepLabels: List<String>,
    currentStepIndex: Int,
    modifier: Modifier = Modifier,
    optionalStepIndices: Set<Int> = emptySet(),
    skipToReviewLabel: String? = null,
    onSkipToReview: (() -> Unit)? = null,
    allowForwardJumps: Boolean = false,
    onStepClicked: ((stepIndex: Int) -> Unit)? = null
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            AnimatedContent(
                targetState = stepLabels,
                transitionSpec = {
                    // Slide right when a step is added, slide left when one is removed.
                    val direction = if (targetState.size >= initialState.size) 1 else -1
                    (
                        slideInHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            initialOffsetX = { fullWidth -> direction * fullWidth / 3 }
                        ) + fadeIn(animationSpec = tween(durationMillis = 250))
                        )
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                targetOffsetX = { fullWidth -> -direction * fullWidth / 3 }
                            ) + fadeOut(animationSpec = tween(durationMillis = 200))
                        )
                        .using(
                            SizeTransform(
                                clip = false,
                                sizeAnimationSpec = { _, _ ->
                                    spring(stiffness = Spring.StiffnessMediumLow)
                                }
                            )
                        )
                },
                label = "wizardStepIndicator"
            ) { labels ->
                if (labels.size > MAX_VISIBLE_STEPS) {
                    ScrollableStepIndicator(
                        stepLabels = labels,
                        currentStepIndex = currentStepIndex,
                        optionalStepIndices = optionalStepIndices,
                        allowForwardJumps = allowForwardJumps,
                        onStepClicked = onStepClicked
                    )
                } else {
                    StaticStepIndicator(
                        stepLabels = labels,
                        currentStepIndex = currentStepIndex,
                        optionalStepIndices = optionalStepIndices,
                        allowForwardJumps = allowForwardJumps,
                        onStepClicked = onStepClicked
                    )
                }
            }

            // ── Skip-to-review link ──────────────────────────────────────
            AnimatedVisibility(
                visible = skipToReviewLabel != null && onSkipToReview != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (skipToReviewLabel != null && onSkipToReview != null) {
                    Text(
                        text = skipToReviewLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                role = Role.Button,
                                onClick = onSkipToReview
                            )
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ── Static (≤ MAX_VISIBLE_STEPS) ─────────────────────────────────────────

@Composable
private fun StaticStepIndicator(
    stepLabels: List<String>,
    currentStepIndex: Int,
    optionalStepIndices: Set<Int>,
    allowForwardJumps: Boolean,
    onStepClicked: ((Int) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING, vertical = MaterialTheme.spacing.Medium),
        verticalAlignment = Alignment.Top
    ) {
        stepLabels.forEachIndexed { index, label ->
            val isCompleted = index < currentStepIndex
            WizardStepItem(
                stepNumber = index + 1,
                label = label,
                isCompleted = isCompleted,
                isCurrent = index == currentStepIndex,
                isOptional = index in optionalStepIndices,
                onClick = if ((isCompleted || allowForwardJumps) && onStepClicked != null) {
                    { onStepClicked(index) }
                } else {
                    null
                }
            )
            if (index < stepLabels.lastIndex) {
                WizardStepConnector(
                    isCompleted = isCompleted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Scrollable (> MAX_VISIBLE_STEPS) ─────────────────────────────────────

@Composable
private fun ScrollableStepIndicator(
    stepLabels: List<String>,
    currentStepIndex: Int,
    optionalStepIndices: Set<Int>,
    allowForwardJumps: Boolean,
    onStepClicked: ((Int) -> Unit)? = null
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.Medium)
    ) {
        val usableWidth = maxWidth - HORIZONTAL_PADDING * 2
        val totalConnectorSpace = SCROLLABLE_CONNECTOR_WIDTH * (MAX_VISIBLE_STEPS - 1)
        val stepItemWidth = (usableWidth - totalConnectorSpace) / MAX_VISIBLE_STEPS
        val unitWidth = stepItemWidth + SCROLLABLE_CONNECTOR_WIDTH

        // Smoothly centre the current step in the visible window
        LaunchedEffect(currentStepIndex) {
            val unitPx = with(density) { unitWidth.toPx() }
            val itemPx = with(density) { stepItemWidth.toPx() }
            val viewportPx = with(density) { maxWidth.toPx() }

            val stepCenterPx =
                with(density) { HORIZONTAL_PADDING.toPx() } +
                    currentStepIndex * unitPx + itemPx / 2
            val targetScroll = (stepCenterPx - viewportPx / 2)
                .coerceIn(0f, scrollState.maxValue.toFloat())
            scrollState.animateScrollTo(targetScroll.toInt())
        }

        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = HORIZONTAL_PADDING),
            verticalAlignment = Alignment.Top
        ) {
            stepLabels.forEachIndexed { index, label ->
                val isCompleted = index < currentStepIndex
                WizardStepItem(
                    stepNumber = index + 1,
                    label = label,
                    isCompleted = isCompleted,
                    isCurrent = index == currentStepIndex,
                    isOptional = index in optionalStepIndices,
                    onClick = if ((isCompleted || allowForwardJumps) && onStepClicked != null) {
                        { onStepClicked(index) }
                    } else {
                        null
                    },
                    modifier = Modifier.width(stepItemWidth)
                )
                if (index < stepLabels.lastIndex) {
                    WizardStepConnector(
                        isCompleted = isCompleted,
                        modifier = Modifier.width(SCROLLABLE_CONNECTOR_WIDTH)
                    )
                }
            }
        }
    }
}

// ── Shared sub-components ────────────────────────────────────────────────

@Composable
private fun WizardStepItem(
    stepNumber: Int,
    label: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isOptional: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClick = onClick)
            } else {
                Modifier
            }
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepCircle(
            stepNumber = stepNumber,
            isCompleted = isCompleted,
            isCurrent = isCurrent,
            isOptional = isOptional
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent || isCompleted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = MaterialTheme.spacing.ExtraSmall)
        )
    }
}

@Composable
private fun WizardStepConnector(
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val connectorColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        label = "wizardConnector"
    )
    Box(
        modifier = modifier
            .padding(
                top = CONNECTOR_TOP_OFFSET.dp,
                start = MaterialTheme.spacing.ExtraSmall,
                end = MaterialTheme.spacing.ExtraSmall
            )
            .height(CONNECTOR_HEIGHT.dp)
            .clip(CircleShape)
            .background(connectorColor)
    )
}

@Composable
private fun StepCircle(
    stepNumber: Int,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isOptional: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCurrent -> MaterialTheme.colorScheme.primary
            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        label = "stepBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isCurrent || isCompleted) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "stepContent"
    )

    // Show dashed border for optional steps that are not yet completed
    val showDashedBorder = isOptional && !isCompleted
    val dashedBorderColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = Modifier
            .size(STEP_CIRCLE_SIZE.dp)
            .then(
                if (showDashedBorder) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = dashedBorderColor,
                            cornerRadius = CornerRadius(size.minDimension / 2),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(
                                        DASH_ON_INTERVAL.dp.toPx(),
                                        DASH_OFF_INTERVAL.dp.toPx()
                                    )
                                )
                            )
                        )
                    }
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isCompleted) "✓" else stepNumber.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}
