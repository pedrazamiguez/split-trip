package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
internal fun PlanAllowancesUpgradeCard(
    description: UiText,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlatCard(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.Large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            SectionHeadingText(text = stringResource(R.string.plan_allowances_upgrade_title))
            BodyText(
                text = description.asString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GradientButton(
                text = stringResource(R.string.subscriptions_cta_upgrade_pro),
                onClick = onUpgradeClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
