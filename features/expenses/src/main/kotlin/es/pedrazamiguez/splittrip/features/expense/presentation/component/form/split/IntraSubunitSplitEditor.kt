package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.ShareLockButton
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import kotlinx.collections.immutable.ImmutableList

private val STANDARD_AMOUNT_MAX_WIDTH = 110.dp
private val NARROW_AMOUNT_MAX_WIDTH = 92.dp
private val STANDARD_PERCENT_MAX_WIDTH = 90.dp
private val NARROW_PERCENT_MAX_WIDTH = 80.dp

/**
 * Level 2 intra-subunit split editor, shown inside the subunit accordion.
 *
 * Displays a per-subunit split type selector followed by member rows.
 * Each member row shows the member's share based on the subunit's split type.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
fun IntraSubunitSplitEditor(
    members: ImmutableList<SplitUiModel>,
    entitySplitType: SplitTypeUiModel?,
    availableSplitTypes: ImmutableList<SplitTypeUiModel>,
    onSplitTypeChanged: (String) -> Unit,
    onAmountChanged: (userId: String, amount: String) -> Unit,
    onPercentageChanged: (userId: String, percentage: String) -> Unit,
    onShareLockToggled: (userId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val isEqualMode = entitySplitType?.id == SplitType.EQUAL.name
    val isPercentMode = entitySplitType?.id == SplitType.PERCENT.name
    val isNarrowScreen = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val startIndent = if (isNarrowScreen) MaterialTheme.spacing.Small else MaterialTheme.spacing.Default
    val amountMaxWidth = if (isNarrowScreen) NARROW_AMOUNT_MAX_WIDTH else STANDARD_AMOUNT_MAX_WIDTH
    val percentMaxWidth = if (isNarrowScreen) NARROW_PERCENT_MAX_WIDTH else STANDARD_PERCENT_MAX_WIDTH

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Text(
            text = stringResource(R.string.add_expense_split_subunit_distribution),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SplitTypeSelector(
            splitTypes = availableSplitTypes,
            selectedSplitType = entitySplitType,
            onSplitTypeSelected = onSplitTypeChanged
        )

        Column(
            modifier = Modifier.padding(start = startIndent),
            verticalArrangement = Arrangement.spacedBy(
                if (isEqualMode) MaterialTheme.spacing.ExtraSmall else MaterialTheme.spacing.Medium
            )
        ) {
            members.forEach { member ->
                val lockDescription = stringResource(
                    if (member.isShareLocked) {
                        R.string.add_expense_split_share_unlock
                    } else {
                        R.string.add_expense_split_share_lock
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.ExtraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        if (isNarrowScreen) MaterialTheme.spacing.ExtraSmall else MaterialTheme.spacing.Small
                    )
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isEqualMode && member.formattedAmount.isNotBlank()) {
                            Text(
                                text = member.formattedAmount,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isEqualMode) {
                        Text(
                            text = member.formattedAmount,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (isPercentMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                        ) {
                            StyledOutlinedTextField(
                                value = member.percentageInput,
                                onValueChange = { pct -> onPercentageChanged(member.userId, pct) },
                                label = stringResource(R.string.add_expense_split_percentage_label),
                                modifier = Modifier.widthIn(max = percentMaxWidth),
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() })
                            )
                            ShareLockButton(
                                isLocked = member.isShareLocked,
                                contentDescription = lockDescription,
                                onClick = { onShareLockToggled(member.userId) }
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                        ) {
                            StyledOutlinedTextField(
                                value = member.amountInput,
                                onValueChange = { amount -> onAmountChanged(member.userId, amount) },
                                label = stringResource(R.string.add_expense_split_amount_label),
                                modifier = Modifier.widthIn(max = amountMaxWidth),
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() })
                            )
                            ShareLockButton(
                                isLocked = member.isShareLocked,
                                contentDescription = lockDescription,
                                onClick = { onShareLockToggled(member.userId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
