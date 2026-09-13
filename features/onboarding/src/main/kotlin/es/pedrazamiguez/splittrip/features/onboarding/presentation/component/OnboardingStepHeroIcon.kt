package es.pedrazamiguez.splittrip.features.onboarding.presentation.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.features.onboarding.presentation.model.OnboardingStep

private const val ICON_CONTAINER_ALPHA = 0.35f
private const val BREATHING_DURATION_MS = 2600
private const val BREATHING_MIN_SCALE = 0.95f
private const val BREATHING_MAX_SCALE = 1.0f

private val HERO_CONTAINER_SIZE = 88.dp
private val HERO_ICON_SIZE = 44.dp

@Composable
fun OnboardingStepHeroIcon(
    step: OnboardingStep,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = BREATHING_MAX_SCALE,
        targetValue = BREATHING_MIN_SCALE,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BREATHING_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboarding_breathing_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = breathingScale
                scaleY = breathingScale
            }
            .size(HERO_CONTAINER_SIZE)
            .clip(CircleShape)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = ICON_CONTAINER_ALPHA),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = step.icon,
            contentDescription = null,
            modifier = Modifier.size(HERO_ICON_SIZE),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
