package es.pedrazamiguez.splittrip.features.settlement.presentation.component.step.archive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.settlement.R

@Composable
fun ArchiveConfirmationStep(
    groupName: String,
    hasUnresolvedSettlements: Boolean,
    onGoToSettlementsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        Text(
            text = stringResource(R.string.archive_wizard_confirm_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        BodyText(
            text = stringResource(R.string.archive_wizard_confirm_subtitle, groupName),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (hasUnresolvedSettlements) {
            FormErrorBanner(
                error = UiText.StringResource(R.string.archive_wizard_confirm_warning_banner)
            )

            TextButton(
                onClick = onGoToSettlementsClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.archive_wizard_go_to_settlements),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        SectionCard(
            title = stringResource(R.string.archive_wizard_confirm_impact_title),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(top = MaterialTheme.spacing.Small)) {
                BodyText(
                    text = stringResource(R.string.archive_wizard_confirm_desc),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
