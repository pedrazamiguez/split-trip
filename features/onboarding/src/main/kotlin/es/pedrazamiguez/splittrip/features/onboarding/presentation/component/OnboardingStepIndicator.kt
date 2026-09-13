package es.pedrazamiguez.splittrip.features.onboarding.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private val ACTIVE_STEP_WIDTH = 28.dp
private val INACTIVE_STEP_WIDTH = 8.dp
private val STEP_HEIGHT = 8.dp
private val STEP_SPACING = 8.dp

@Composable
fun OnboardingStepIndicator(
    totalSteps: Int,
    currentStepIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(STEP_SPACING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until totalSteps) {
            val isSelected = index == currentStepIndex
            val width by animateDpAsState(
                targetValue = if (isSelected) ACTIVE_STEP_WIDTH else INACTIVE_STEP_WIDTH,
                label = "step_pill_width"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                label = "step_pill_color"
            )

            Box(
                modifier = Modifier
                    .size(width = width, height = STEP_HEIGHT)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
