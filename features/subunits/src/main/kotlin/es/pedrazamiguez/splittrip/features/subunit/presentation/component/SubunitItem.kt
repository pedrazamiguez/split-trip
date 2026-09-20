package es.pedrazamiguez.splittrip.features.subunit.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedCombinedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.UserFilled
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.DotsVertical
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.SubunitUiModel

private val MEMBER_AVATAR_SIZE = 28.dp

@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongMethod")
@Composable
fun SubunitItem(
    subunitUiModel: SubunitUiModel,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onLongClick: () -> Unit = onMenuClick
) {
    val haptics = LocalHapticFeedback.current

    FlatCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .debouncedCombinedClickable(
                enableSpringPress = true,
                onClick = onMenuClick,
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
            // Name + member count + options menu
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecondaryBodyText(
                        text = subunitUiModel.memberCount,
                        maxLines = 1
                    )
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.DotsVertical,
                            contentDescription = stringResource(R.string.subunit_actions_title, subunitUiModel.name),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Member shares — each member on its own row with avatar + name + percentage chip
            if (subunitUiModel.memberShares.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
                    subunitUiModel.memberShares.forEach { memberShare ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (memberShare.avatarUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(memberShare.avatarUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(MEMBER_AVATAR_SIZE)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.size(MEMBER_AVATAR_SIZE),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = TablerIcons.Filled.UserFilled,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                BodyText(
                                    text = memberShare.displayName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
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
