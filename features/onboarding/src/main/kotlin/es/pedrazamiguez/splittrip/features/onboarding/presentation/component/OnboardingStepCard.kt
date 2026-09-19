package es.pedrazamiguez.splittrip.features.onboarding.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.ScreenTitleText
import es.pedrazamiguez.splittrip.features.onboarding.presentation.model.OnboardingStep

@Composable
fun OnboardingStepCard(
    step: OnboardingStep,
    modifier: Modifier = Modifier
) {
    FlatCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OnboardingStepHeroIcon(
                step = step
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))

            ScreenTitleText(
                text = stringResource(step.titleRes),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

            BodyText(
                text = stringResource(step.descriptionRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
