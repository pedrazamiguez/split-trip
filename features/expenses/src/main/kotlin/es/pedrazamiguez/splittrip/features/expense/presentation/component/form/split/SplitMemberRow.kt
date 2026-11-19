package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedCombinedClickableNoRipple
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.ShareAllocationInputRow
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel

private val TRAILING_ICON_SLOT_SIZE = 24.dp

@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun SplitMemberRow(
    split: SplitUiModel,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    onAmountChanged: (String) -> Unit,
    onPercentageChanged: (String) -> Unit,
    onExcludedToggled: () -> Unit,
    onSoloSelected: () -> Unit,
    onShareLockToggled: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String? = null
) {
    val isNarrowScreen = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val rowGap = if (isNarrowScreen) MaterialTheme.spacing.ExtraSmall else MaterialTheme.spacing.Small

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.ExtraSmall),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(rowGap)
        ) {
            Text(
                text = split.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (split.isExcluded) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )

            if (!split.isExcluded && split.formattedAmount.isNotBlank()) {
                Text(
                    text = split.formattedAmount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Reserve the same 24.dp trailing slot as subunit rows so prices stay
                // aligned and separated from the toggle by an identical gap.
                Spacer(modifier = Modifier.size(TRAILING_ICON_SLOT_SIZE))
            }

            val haptic = LocalHapticFeedback.current
            Box(
                modifier = Modifier.debouncedCombinedClickableNoRipple(
                    onClick = onExcludedToggled,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSoloSelected()
                    },
                    role = Role.Switch
                )
            ) {
                Switch(
                    checked = !split.isExcluded,
                    onCheckedChange = null
                )
            }
        }

        AnimatedVisibility(visible = !isEqualMode && !split.isExcluded) {
            val inputConfig = resolveSplitInputConfig(
                split = split,
                isPercentMode = isPercentMode,
                currencySymbol = currencySymbol,
                onAmountChanged = onAmountChanged,
                onPercentageChanged = onPercentageChanged
            )
            ShareAllocationInputRow(
                value = inputConfig.value,
                onValueChange = inputConfig.onValueChange,
                unitText = inputConfig.unitRes?.let { stringResource(it) } ?: inputConfig.unitLiteral,
                isLocked = split.isShareLocked,
                lockContentDescription = stringResource(splitLockDescriptionRes(split.isShareLocked)),
                onLockToggle = onShareLockToggled,
                fieldContentDescription = stringResource(inputConfig.fieldDescriptionRes),
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { onDone() })
            )
        }
    }
}
