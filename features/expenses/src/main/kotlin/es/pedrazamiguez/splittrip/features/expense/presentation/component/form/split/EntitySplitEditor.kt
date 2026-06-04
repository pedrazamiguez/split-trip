package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Displays entity-level split rows for subunit mode (Level 1).
 *
 * Each entity row is either a solo user or a subunit header.
 * Subunit headers are expandable accordions that reveal intra-subunit splits (Level 2).
 *
 * @param entitySplits The entity-level split rows (solo users + subunit headers).
 * @param isEqualMode Whether the Level 1 split type is EQUAL.
 * @param isPercentMode Whether the Level 1 split type is PERCENT.
 * @param availableSplitTypes Available split types for Level 2 intra-subunit selector.
 * @param events Grouped event callbacks for all user interactions.
 */
@Composable
fun EntitySplitEditor(
    entitySplits: ImmutableList<SplitUiModel>,
    isEqualMode: Boolean,
    isPercentMode: Boolean,
    availableSplitTypes: ImmutableList<SplitTypeUiModel>,
    events: EntitySplitEditorEvents,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        entitySplits.forEach { entity ->
            EntitySplitRow(
                entity = entity,
                isEqualMode = isEqualMode,
                isPercentMode = isPercentMode,
                availableSplitTypes = availableSplitTypes,
                events = events,
                onDone = { focusManager.clearFocus() }
            )
        }
    }
}
