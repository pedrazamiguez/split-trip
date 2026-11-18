package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedCombinedClickableNoRipple
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.ShareLockButton
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel

private val STANDARD_AMOUNT_MAX_WIDTH = 120.dp
private val NARROW_AMOUNT_MAX_WIDTH = 96.dp
private val STANDARD_PERCENT_MAX_WIDTH = 100.dp
private val NARROW_PERCENT_MAX_WIDTH = 84.dp

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
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
    modifier: Modifier = Modifier
) {
    val isNarrowScreen = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val amountMaxWidth = if (isNarrowScreen) NARROW_AMOUNT_MAX_WIDTH else STANDARD_AMOUNT_MAX_WIDTH
    val percentMaxWidth = if (isNarrowScreen) NARROW_PERCENT_MAX_WIDTH else STANDARD_PERCENT_MAX_WIDTH
    val lockDescription = stringResource(
        if (split.isShareLocked) {
            R.string.add_expense_split_share_unlock
        } else {
            R.string.add_expense_split_share_lock
        }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                vertical = if (isEqualMode) {
                    MaterialTheme.spacing.ExtraSmall
                } else {
                    MaterialTheme.spacing.Small
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            if (isNarrowScreen) MaterialTheme.spacing.ExtraSmall else MaterialTheme.spacing.Small
        )
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
                }
            )
            if (!split.isExcluded && !isEqualMode && split.formattedAmount.isNotBlank()) {
                SecondaryBodyText(
                    text = split.formattedAmount,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = Int.MAX_VALUE
                )
            }
        }

        AnimatedVisibility(visible = !split.isExcluded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                if (isEqualMode) {
                    Text(
                        text = split.formattedAmount,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isPercentMode) {
                    StyledOutlinedTextField(
                        value = split.percentageInput,
                        onValueChange = onPercentageChanged,
                        label = stringResource(R.string.add_expense_split_percentage_label),
                        modifier = Modifier.widthIn(max = percentMaxWidth),
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { onDone() })
                    )
                    ShareLockButton(
                        isLocked = split.isShareLocked,
                        contentDescription = lockDescription,
                        onClick = onShareLockToggled
                    )
                } else {
                    StyledOutlinedTextField(
                        value = split.amountInput,
                        onValueChange = onAmountChanged,
                        label = stringResource(R.string.add_expense_split_amount_label),
                        modifier = Modifier.widthIn(max = amountMaxWidth),
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { onDone() })
                    )
                    ShareLockButton(
                        isLocked = split.isShareLocked,
                        contentDescription = lockDescription,
                        onClick = onShareLockToggled
                    )
                }
            }
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
}
