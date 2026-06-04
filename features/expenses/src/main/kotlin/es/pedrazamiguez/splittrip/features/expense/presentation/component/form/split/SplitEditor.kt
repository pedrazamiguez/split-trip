package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Displays per-user split entries.
 *
 * - EQUAL mode: shows read-only calculated amounts with currency symbol.
 * - EXACT mode: editable amount input + currency display. Remainder auto-distributes.
 * - PERCENT mode: editable percentage input + currency display. Remainder auto-distributes.
 */
@Suppress("LongParameterList") // Compose UI — params are inherent to the split editor surface
@Composable
fun SplitEditor(
    splits: ImmutableList<SplitUiModel>,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    onAmountChanged: (userId: String, amount: String) -> Unit,
    onPercentageChanged: (userId: String, percentage: String) -> Unit,
    onExcludedToggled: (userId: String) -> Unit,
    onShareLockToggled: (userId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        splits.forEach { split ->
            SplitMemberRow(
                split = split,
                isEqualMode = isEqualMode,
                isPercentMode = isPercentMode,
                onAmountChanged = { amount -> onAmountChanged(split.userId, amount) },
                onPercentageChanged = { pct -> onPercentageChanged(split.userId, pct) },
                onExcludedToggled = { onExcludedToggled(split.userId) },
                onShareLockToggled = { onShareLockToggled(split.userId) },
                onDone = { focusManager.clearFocus() }
            )
        }
    }
}
