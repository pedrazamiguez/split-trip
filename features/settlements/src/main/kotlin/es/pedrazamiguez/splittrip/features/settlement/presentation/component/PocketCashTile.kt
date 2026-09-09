package es.pedrazamiguez.splittrip.features.settlement.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickableNoRipple
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.features.settlement.R

@Composable
internal fun RowScope.PocketCashTile(
    icon: ImageVector,
    label: String,
    formattedAmount: String,
    amountColor: Color = MaterialTheme.colorScheme.onSurface,
    showInfoIcon: Boolean = false,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null
) {
    val isClickable = showInfoIcon && onInfoClick != null
    val cardModifier = if (isClickable) {
        Modifier
            .fillMaxWidth()
            .debouncedClickableNoRipple(
                role = Role.Button,
                onClickLabel = stringResource(R.string.your_balance_cash_breakdown_view_cd),
                onClick = onInfoClick
            )
    } else {
        Modifier.fillMaxWidth()
    }

    Box(
        modifier = modifier.weight(1f)
    ) {
        FlatCard(
            modifier = cardModifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CaptionText(text = label)
                    }

                    if (showInfoIcon) {
                        Icon(
                            imageVector = TablerIcons.Outline.InfoCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                AmountText(text = formattedAmount, color = amountColor)

                content?.invoke()
            }
        }
    }
}
