package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Refresh
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopNotificationContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillColors
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillDefaults
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle

const val APP_UPDATE_READY_BANNER_TEST_TAG = "app_update_ready_banner"
const val APP_UPDATE_READY_ACTION_TEST_TAG = "app_update_ready_action"

private const val DESCRIPTION_MAX_LINES = 2

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
        enter = TopPillDefaults.EnterTransition,
        exit = TopPillDefaults.ExitTransition,
        modifier = modifier
    ) {
        AppUpdateReadyBannerContent(onRestartClick = onRestartClick)
    }
}

@Composable
private fun AppUpdateReadyBannerContent(
    onRestartClick: () -> Unit
) {
    val colors = TopPillDefaults.colors(TopPillStyle.INFO)

    TopNotificationContainer(
        style = TopPillStyle.INFO,
        surfaceModifier = Modifier.testTag(APP_UPDATE_READY_BANNER_TEST_TAG)
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
            AppUpdateReadyHeaderRow(colors = colors)
            AppUpdateReadyActionPill(
                colors = colors,
                onRestartClick = onRestartClick,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun AppUpdateReadyHeaderRow(
    colors: TopPillColors
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Icon(
            imageVector = TablerIcons.Outline.Refresh,
            contentDescription = null,
            tint = colors.iconColor,
            modifier = Modifier.size(TopPillDefaults.LeadingIconSize)
        )
        Column(modifier = Modifier.weight(1f)) {
            CardTitleText(
                text = stringResource(R.string.app_update_ready_title),
                color = colors.contentColor
            )
            SecondaryBodyText(
                text = stringResource(R.string.app_update_ready_description),
                color = colors.secondaryContentColor,
                maxLines = DESCRIPTION_MAX_LINES
            )
        }
    }
}

@Composable
private fun AppUpdateReadyActionPill(
    colors: TopPillColors,
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = TopPillDefaults.ActionMinHeight)
            .clip(CircleShape)
            .background(colors.actionContainerColor, CircleShape)
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
            color = colors.actionContentColor
        )
    }
}
