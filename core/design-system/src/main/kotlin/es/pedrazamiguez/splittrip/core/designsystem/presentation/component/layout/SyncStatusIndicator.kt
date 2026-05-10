package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CloudOff
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.RefreshAlert
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.shape.ExpressiveShapes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.shape.RoundedPolygonShape
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import kotlinx.coroutines.delay

// Animation constants
private const val SCALE_IN_INITIAL = 0.8f
private const val SCALE_OUT_TARGET = 0.8f

// Container constants
private val CONTAINER_PADDING = 4.dp
private val ICON_SIZE = 14.dp
private val BADGE_OFFSET = 4.dp
private const val PENDING_CONTAINER_ALPHA = 0.6f

// How long to wait before showing the PENDING_SYNC badge.
// Suppresses the flash when the entity syncs quickly while online
// (typical online round-trip is <200ms). If still pending after this
// window the badge appears, signalling the user they are working offline.
private const val PENDING_SYNC_DELAY_MS = 300L

/**
 * Compact sync-status indicator that shows an icon + optional text
 * inside a subtle expressive container when the entity has not yet
 * been synced to the cloud.
 *
 * **Visibility rules:**
 * - [SyncStatus.SYNC_FAILED] — shown immediately (genuine write error).
 * - [SyncStatus.PENDING_SYNC] — shown only after [PENDING_SYNC_DELAY_MS].
 *   If the sync completes before the delay expires the badge never appears,
 *   preventing a distracting flash during fast online writes.
 * - [SyncStatus.SYNCED] — hidden immediately.
 *
 * The badge entrance/exit is animated with fade + scale via [AnimatedVisibility].
 * Internal state transitions (e.g. PENDING_SYNC → SYNC_FAILED) cross-fade via
 * [AnimatedContent] inside [SyncStatusContent].
 *
 * **Call-sites should always compose this** (no external `if` guard)
 * and let the built-in [AnimatedVisibility] handle show/hide.
 *
 * @param syncStatus Current synchronization status of the entity.
 * @param showLabel  When true, displays a short text label next to the icon.
 * @param modifier   Outer modifier applied to the animated wrapper.
 */
@Composable
fun SyncStatusIndicator(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    // Start immediately visible only for SYNC_FAILED so that genuine errors on
    // already-persisted items are never hidden on first composition.
    // PENDING_SYNC starts hidden and becomes visible only after the delay window.
    var visible by remember { mutableStateOf(syncStatus == SyncStatus.SYNC_FAILED) }

    LaunchedEffect(syncStatus) {
        when (syncStatus) {
            SyncStatus.SYNCED -> visible = false
            SyncStatus.SYNC_FAILED -> visible = true
            SyncStatus.PENDING_SYNC -> {
                // Delay before showing: if the entity syncs within this window
                // (normal online path) the coroutine is cancelled and visible
                // stays false, so the badge never flashes.
                delay(PENDING_SYNC_DELAY_MS)
                visible = true
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = SCALE_IN_INITIAL),
        exit = fadeOut() + scaleOut(targetScale = SCALE_OUT_TARGET)
    ) {
        SyncStatusContent(syncStatus = syncStatus, showLabel = showLabel)
    }
}

/**
 * Positions a [SyncStatusIndicator] as a floating overlay badge at the
 * bottom-end corner of the parent `Box`.
 *
 * The badge is offset slightly outside the card bounds (like a notification
 * badge on an app icon) to minimise overlap with in-card content. The parent
 * `Box` does not clip by default, so the badge remains fully visible even
 * when it extends beyond the card edges.
 *
 * **Usage:** Wrap the card content and this badge in a `Box`:
 *
 * ```kotlin
 * Box {
 *     FlatCard { /* item content */ }
 *     SyncStatusBadge(syncStatus = model.syncStatus)
 * }
 * ```
 *
 * @param syncStatus Current synchronization status of the entity.
 * @param modifier   Modifier applied to the badge wrapper.
 */
@Composable
fun BoxScope.SyncStatusBadge(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    SyncStatusIndicator(
        syncStatus = syncStatus,
        modifier = modifier
            .align(Alignment.BottomEnd)
            .offset(x = BADGE_OFFSET, y = BADGE_OFFSET)
    )
}

// Compose DSL: four parallel if/else branches all test the same single boolean (isPending).
// Detekt counts each independently; suppress to avoid inflating the score artificially.
@Suppress("CognitiveComplexMethod")
@Composable
private fun SyncStatusContent(syncStatus: SyncStatus, showLabel: Boolean) {
    val shape = remember { RoundedPolygonShape(ExpressiveShapes.softScallopedCircle()) }

    AnimatedContent(
        targetState = syncStatus,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "sync_status_content"
    ) { status ->
        val isPending = status == SyncStatus.PENDING_SYNC

        val icon = if (isPending) TablerIcons.Outline.CloudOff else TablerIcons.Outline.RefreshAlert
        val containerColor = if (isPending) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = PENDING_CONTAINER_ALPHA)
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
        val contentColor = if (isPending) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        }
        val contentDesc = if (isPending) {
            stringResource(R.string.sync_status_pending)
        } else {
            stringResource(R.string.sync_status_failed)
        }

        Row(
            modifier = Modifier
                .clip(shape)
                .background(containerColor)
                .padding(CONTAINER_PADDING),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                modifier = Modifier.size(ICON_SIZE),
                tint = contentColor
            )
            if (showLabel) {
                Text(
                    text = contentDesc,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor
                )
            }
        }
    }
}
