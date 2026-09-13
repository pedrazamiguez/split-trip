package es.pedrazamiguez.splittrip.features.onboarding.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ArrowLeft
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ArrowRight
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.onboarding.R

@Composable
fun OnboardingNavigationBar(
    isFirstStep: Boolean,
    isLastStep: Boolean,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isFirstStep) {
        GradientButton(
            text = stringResource(R.string.onboarding_next_button),
            onClick = onNextClick,
            trailingIcon = TablerIcons.Outline.ArrowRight,
            modifier = modifier.fillMaxWidth()
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            SecondaryButton(
                text = stringResource(R.string.onboarding_back_button),
                onClick = onPreviousClick,
                leadingIcon = TablerIcons.Outline.ArrowLeft,
                modifier = Modifier.weight(1f)
            )
            if (isLastStep) {
                GradientButton(
                    text = stringResource(R.string.onboarding_complete_button),
                    onClick = onCompleteClick,
                    modifier = Modifier.weight(1f)
                )
            } else {
                GradientButton(
                    text = stringResource(R.string.onboarding_next_button),
                    onClick = onNextClick,
                    trailingIcon = TablerIcons.Outline.ArrowRight,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
