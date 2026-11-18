package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Level 2 intra-subunit split editor, shown inside the expanded subunit card.
 *
 * Displays a per-subunit split type selector followed by [IntraSubunitMemberRow] items aligned
 * with [SplitMemberRow] and [EntitySplitRow].
 */
@Suppress("LongParameterList")
@Composable
fun IntraSubunitSplitEditor(
    members: ImmutableList<SplitUiModel>,
    entitySplitType: SplitTypeUiModel?,
    availableSplitTypes: ImmutableList<SplitTypeUiModel>,
    onSplitTypeChanged: (String) -> Unit,
    onAmountChanged: (userId: String, amount: String) -> Unit,
    onPercentageChanged: (userId: String, percentage: String) -> Unit,
    onShareLockToggled: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String? = null
) {
    val focusManager = LocalFocusManager.current
    val isEqualMode = entitySplitType?.id == SplitType.EQUAL.name
    val isPercentMode = entitySplitType?.id == SplitType.PERCENT.name

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
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
            verticalArrangement = Arrangement.spacedBy(
                if (isEqualMode) MaterialTheme.spacing.Small else MaterialTheme.spacing.Default
            )
        ) {
            members.forEach { member ->
                IntraSubunitMemberRow(
                    member = member,
                    isEqualMode = isEqualMode,
                    isPercentMode = isPercentMode,
                    currencySymbol = currencySymbol,
                    onAmountChanged = { amount -> onAmountChanged(member.userId, amount) },
                    onPercentageChanged = { pct -> onPercentageChanged(member.userId, pct) },
                    onShareLockToggled = { onShareLockToggled(member.userId) },
                    onDone = { focusManager.clearFocus() }
                )
            }
        }
    }
}
