package es.pedrazamiguez.splittrip.features.onboarding.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Bell
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.ScreenTitleText
import es.pedrazamiguez.splittrip.features.onboarding.R
import es.pedrazamiguez.splittrip.features.onboarding.presentation.model.OnboardingStep

private val BADGE_ICON_SIZE = 20.dp

@Composable
fun OnboardingStepCard(
    step: OnboardingStep,
    hasNotificationPermission: Boolean = false,
    onRequestNotificationPermissionClick: () -> Unit = {},
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

            if (step == OnboardingStep.REAL_TIME_NOTIFICATIONS) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
                if (hasNotificationPermission) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.CircleCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(BADGE_ICON_SIZE)
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.Small))
                        Text(
                            text = stringResource(R.string.onboarding_notifications_enabled),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    SecondaryButton(
                        text = stringResource(R.string.onboarding_enable_notifications_button),
                        onClick = onRequestNotificationPermissionClick,
                        leadingIcon = TablerIcons.Outline.Bell
                    )
                }
            }
        }
    }
}
