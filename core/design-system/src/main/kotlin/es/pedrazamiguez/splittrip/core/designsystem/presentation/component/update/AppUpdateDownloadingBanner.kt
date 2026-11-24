package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Refresh
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText

const val APP_UPDATE_DOWNLOADING_BANNER_TEST_TAG = "app_update_downloading_banner"
const val APP_UPDATE_DOWNLOADING_PERCENT_TEST_TAG = "app_update_downloading_percent"
const val APP_UPDATE_DOWNLOADING_DETERMINATE_PROGRESS_TEST_TAG =
    "app_update_downloading_determinate_progress"
const val APP_UPDATE_DOWNLOADING_INDETERMINATE_PROGRESS_TEST_TAG =
    "app_update_downloading_indeterminate_progress"

private val BANNER_ELEVATION = 6.dp
private val BANNER_ICON_SIZE = 20.dp
private val PROGRESS_BAR_HEIGHT = 6.dp
private const val MIN_PROGRESS_FRACTION = 0f
private const val MAX_PROGRESS_FRACTION = 1f
private const val DESCRIPTION_MAX_LINES = 2

/**
 * Non-blocking top banner showing background download progress for a flexible app update.
 */
@Composable
fun AppUpdateDownloadingBanner(
    visible: Boolean,
    progressFraction: Float?,
    progressPercent: Int?,
    modifier: Modifier = Modifier
) {
    // Preserve last visible progress values so exit animation doesn't flash indeterminate.
    var displayedProgressFraction by remember { mutableStateOf(progressFraction) }
    var displayedProgressPercent by remember { mutableStateOf(progressPercent) }
    if (visible) {
        displayedProgressFraction = progressFraction
        displayedProgressPercent = progressPercent
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        AppUpdateDownloadingBannerContent(
            progressFraction = displayedProgressFraction,
            progressPercent = displayedProgressPercent
        )
    }
}

@Composable
private fun AppUpdateDownloadingBannerContent(
    progressFraction: Float?,
    progressPercent: Int?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = MaterialTheme.spacing.Default,
                vertical = MaterialTheme.spacing.Small
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        FlatCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(APP_UPDATE_DOWNLOADING_BANNER_TEST_TAG),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ghostBorder = true,
            elevation = BANNER_ELEVATION
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.Default,
                        vertical = MaterialTheme.spacing.Medium
                    ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                AppUpdateDownloadingHeaderRow(progressPercent = progressPercent)
                AppUpdateDownloadingProgressBar(progressFraction = progressFraction)
            }
        }
    }
}

@Composable
private fun AppUpdateDownloadingHeaderRow(
    progressPercent: Int?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Icon(
            imageVector = TablerIcons.Outline.Refresh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(BANNER_ICON_SIZE)
        )
        Column(modifier = Modifier.weight(1f)) {
            CardTitleText(
                text = stringResource(R.string.app_update_downloading_title),
                color = MaterialTheme.colorScheme.onSurface
            )
            SecondaryBodyText(
                text = stringResource(R.string.app_update_downloading_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = DESCRIPTION_MAX_LINES
            )
        }
        if (progressPercent != null) {
            LabelText(
                text = stringResource(R.string.app_update_downloading_percent, progressPercent),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag(APP_UPDATE_DOWNLOADING_PERCENT_TEST_TAG)
            )
        }
    }
}

@Composable
private fun AppUpdateDownloadingProgressBar(
    progressFraction: Float?
) {
    val baseModifier = Modifier
        .fillMaxWidth()
        .height(PROGRESS_BAR_HEIGHT)
        .clip(CircleShape)

    if (progressFraction != null) {
        val animatedProgress by animateFloatAsState(
            targetValue = progressFraction.coerceIn(MIN_PROGRESS_FRACTION, MAX_PROGRESS_FRACTION),
            label = "app_update_download_progress"
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = baseModifier.testTag(APP_UPDATE_DOWNLOADING_DETERMINATE_PROGRESS_TEST_TAG),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    } else {
        LinearProgressIndicator(
            modifier = baseModifier.testTag(APP_UPDATE_DOWNLOADING_INDETERMINATE_PROGRESS_TEST_TAG),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp
        )
    }
}
