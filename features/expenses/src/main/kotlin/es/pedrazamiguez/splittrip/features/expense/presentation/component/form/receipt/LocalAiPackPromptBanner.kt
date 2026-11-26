package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.LocalAiPackBannerUiModel

private val BANNER_ICON_SIZE = 20.dp
private val DISMISS_ICON_SIZE = 18.dp

@Composable
fun LocalAiPackPromptBanner(
    banner: LocalAiPackBannerUiModel,
    onManageClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.PhotoAi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(BANNER_ICON_SIZE)
                )
                Text(
                    text = banner.title.asString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                PassportTag(
                    text = banner.sizeBadge,
                    style = PassportTagStyle.INFO
                )
                IconButton(onClick = onDismissClick) {
                    Icon(
                        imageVector = TablerIcons.Outline.X,
                        contentDescription = stringResource(R.string.expense_local_ai_banner_dismiss_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DISMISS_ICON_SIZE)
                    )
                }
            }

            Text(
                text = banner.description.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SecondaryButton(
                text = banner.actionLabel.asString(),
                onClick = onManageClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
