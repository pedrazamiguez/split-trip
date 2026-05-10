package es.pedrazamiguez.splittrip.features.subunit.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.SubunitUiModel

@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongMethod") // Compose UI builder DSL
@Composable
fun SubunitItem(subunitUiModel: SubunitUiModel, modifier: Modifier = Modifier, onLongClick: () -> Unit = {}) {
    val haptics = LocalHapticFeedback.current

    FlatCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = { /* No click action for now */ },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            // Name + member count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeadingText(
                    text = subunitUiModel.name,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = MaterialTheme.spacing.Small)
                )
                SecondaryBodyText(
                    text = subunitUiModel.memberCount,
                    maxLines = Int.MAX_VALUE
                )
            }

            // Member shares — each member on its own row with a percentage chip
            if (subunitUiModel.memberShares.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
                    subunitUiModel.memberShares.forEach { memberShare ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
                        ) {
                            BodyText(
                                text = memberShare.displayName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text(
                                    text = memberShare.shareText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(
                                        horizontal = MaterialTheme.spacing.Small,
                                        vertical = 2.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
