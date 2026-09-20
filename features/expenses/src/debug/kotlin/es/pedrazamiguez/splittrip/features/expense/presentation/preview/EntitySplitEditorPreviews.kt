package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split.EntitySplitEditor
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split.EntitySplitEditorEvents
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import kotlinx.collections.immutable.persistentListOf

private val PREVIEW_SPLIT_TYPES = persistentListOf(
    SplitTypeUiModel("EQUAL", "Equal"),
    SplitTypeUiModel("EXACT", "Exact"),
    SplitTypeUiModel("PERCENT", "Percent")
)

private val PREVIEW_EVENTS = EntitySplitEditorEvents(
    onAmountChanged = { _, _ -> },
    onPercentageChanged = { _, _ -> },
    onExcludedToggled = { _ -> },
    onShareLockToggled = { _ -> },
    onAccordionToggled = { _ -> },
    onIntraSubunitSplitTypeChanged = { _, _ -> },
    onIntraSubunitAmountChanged = { _, _, _ -> },
    onIntraSubunitPercentageChanged = { _, _, _ -> },
    onIntraSubunitShareLockToggled = { _, _ -> }
)

private val PREVIEW_ENTITY_SPLITS = persistentListOf(
    SplitUiModel(
        userId = "subunit-1",
        displayName = "Cantalobos",
        amountCents = 5000L,
        formattedAmount = "50.00 €",
        amountInput = "50.00",
        percentageInput = "50",
        isEntityRow = true,
        isExpanded = true,
        entitySplitType = SplitTypeUiModel("EQUAL", "Equal"),
        entityMembers = persistentListOf(
            SplitUiModel(
                userId = "user-1",
                displayName = "Antonio García",
                amountCents = 2500L,
                formattedAmount = "25.00 €",
                amountInput = "25.00",
                percentageInput = "50",
                subunitId = "subunit-1"
            ),
            SplitUiModel(
                userId = "user-2",
                displayName = "María López",
                amountCents = 2500L,
                formattedAmount = "25.00 €",
                amountInput = "25.00",
                percentageInput = "50",
                subunitId = "subunit-1"
            )
        )
    ),
    SplitUiModel(
        userId = "user-3",
        displayName = "Andrés Pedraza",
        amountCents = 5000L,
        formattedAmount = "50.00 €",
        amountInput = "50.00",
        percentageInput = "50",
        isEntityRow = false
    )
)

@PreviewComplete
@Composable
private fun EntitySplitEditorEqualPreview() {
    PreviewThemeWrapper {
        EntitySplitEditor(
            entitySplits = PREVIEW_ENTITY_SPLITS,
            isEqualMode = true,
            isPercentMode = false,
            availableSplitTypes = PREVIEW_SPLIT_TYPES,
            events = PREVIEW_EVENTS
        )
    }
}

@PreviewComplete
@Composable
private fun EntitySplitEditorExactPreview() {
    PreviewThemeWrapper {
        EntitySplitEditor(
            entitySplits = PREVIEW_ENTITY_SPLITS,
            isEqualMode = false,
            isPercentMode = false,
            availableSplitTypes = PREVIEW_SPLIT_TYPES,
            events = PREVIEW_EVENTS
        )
    }
}

@PreviewComplete
@Composable
private fun EntitySplitEditorPercentagePreview() {
    PreviewThemeWrapper {
        EntitySplitEditor(
            entitySplits = PREVIEW_ENTITY_SPLITS,
            isEqualMode = false,
            isPercentMode = true,
            availableSplitTypes = PREVIEW_SPLIT_TYPES,
            events = PREVIEW_EVENTS
        )
    }
}
