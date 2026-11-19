package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Refresh
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.RefreshAlert
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.ScreenTitleText

const val APP_UPDATE_GATE_TEST_TAG = "app_update_gate"
const val APP_UPDATE_GATE_BUTTON_TEST_TAG = "app_update_gate_button"

private val UPDATE_ICON_SIZE = 72.dp

/**
 * Full-screen blocking gate displayed when a mandatory update cannot be bypassed.
 */
@Composable
fun AppUpdateGate(
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier,
    isUpdating: Boolean = false
) {
    BackHandler(enabled = true) {
        // Mandatory update cannot be dismissed via back navigation.
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .systemBarsPadding()
            .testTag(APP_UPDATE_GATE_TEST_TAG),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = TablerIcons.Outline.RefreshAlert,
                contentDescription = null,
                modifier = Modifier.size(UPDATE_ICON_SIZE),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

            ScreenTitleText(
                text = stringResource(R.string.app_update_required_title),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

            BodyText(
                text = stringResource(R.string.app_update_required_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Section))

            GradientButton(
                text = stringResource(R.string.app_update_required_button),
                onClick = onUpdateClick,
                isLoading = isUpdating,
                leadingIcon = TablerIcons.Outline.Refresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(APP_UPDATE_GATE_BUTTON_TEST_TAG)
            )
        }
    }
}
