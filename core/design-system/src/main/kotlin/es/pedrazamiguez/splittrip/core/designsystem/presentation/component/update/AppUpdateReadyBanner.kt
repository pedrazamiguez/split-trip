package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Refresh
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText

const val APP_UPDATE_READY_BANNER_TEST_TAG = "app_update_ready_banner"
const val APP_UPDATE_READY_ACTION_TEST_TAG = "app_update_ready_action"

private val BANNER_ELEVATION = 6.dp
private val BANNER_ICON_SIZE = 20.dp
private val BANNER_ACTION_MIN_HEIGHT = 36.dp

/**
 * Non-blocking top banner prompting the user to restart and complete a downloaded update.
 */
@Composable
fun AppUpdateReadyBanner(
    visible: Boolean,
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        AppUpdateReadyBannerContent(onRestartClick = onRestartClick)
    }
}

@Composable
private fun AppUpdateReadyBannerContent(
    onRestartClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .padding(
                horizontal = MaterialTheme.spacing.Default,
                vertical = MaterialTheme.spacing.Small
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        FlatCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(APP_UPDATE_READY_BANNER_TEST_TAG),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
            elevation = BANNER_ELEVATION
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.Default,
                        vertical = MaterialTheme.spacing.Medium
                    ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                AppUpdateReadyHeaderRow()
                AppUpdateReadyActionPill(
                    onRestartClick = onRestartClick,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun AppUpdateReadyHeaderRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Icon(
            imageVector = TablerIcons.Outline.Refresh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(BANNER_ICON_SIZE)
        )
        Column(modifier = Modifier.weight(1f)) {
            CardTitleText(
                text = stringResource(R.string.app_update_ready_title),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            SecondaryBodyText(
                text = stringResource(R.string.app_update_ready_description),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun AppUpdateReadyActionPill(
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = BANNER_ACTION_MIN_HEIGHT)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary, CircleShape)
            .debouncedClickable(role = Role.Button, onClick = onRestartClick)
            .padding(
                horizontal = MaterialTheme.spacing.Default,
                vertical = MaterialTheme.spacing.Small
            )
            .testTag(APP_UPDATE_READY_ACTION_TEST_TAG),
        contentAlignment = Alignment.Center
    ) {
        LabelText(
            text = stringResource(R.string.app_update_ready_button),
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}
