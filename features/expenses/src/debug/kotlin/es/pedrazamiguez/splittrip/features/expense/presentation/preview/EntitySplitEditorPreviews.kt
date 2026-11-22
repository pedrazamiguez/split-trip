package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split.EntitySplitEditor
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split.EntitySplitEditorEvents
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split.SplitEditor
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
private fun previewSplitTypes(): ImmutableList<SplitTypeUiModel> = persistentListOf(
    SplitTypeUiModel("EQUAL", stringResource(R.string.split_type_equal)),
    SplitTypeUiModel("EXACT", stringResource(R.string.split_type_exact)),
    SplitTypeUiModel("PERCENT", stringResource(R.string.split_type_percent))
)

private val PREVIEW_EVENTS = EntitySplitEditorEvents(
    onAmountChanged = { _, _ -> },
    onPercentageChanged = { _, _ -> },
    onExcludedToggled = { _ -> },
    onSoloSelected = { _ -> },
    onShareLockToggled = { _ -> },
    onAccordionToggled = { _ -> },
    onIntraSubunitSplitTypeChanged = { _, _ -> },
    onIntraSubunitAmountChanged = { _, _, _ -> },
    onIntraSubunitPercentageChanged = { _, _, _ -> },
    onIntraSubunitShareLockToggled = { _, _ -> }
)

@Composable
private fun previewEntitySplits(): ImmutableList<SplitUiModel> = persistentListOf(
    SplitUiModel(
        userId = "subunit-1",
        displayName = "Cantalobos",
        amountCents = 5000L,
        formattedAmount = "50.00 €",
        amountInput = "50.00",
        percentageInput = "50",
        isEntityRow = true,
        isExpanded = true,
        entitySplitType = SplitTypeUiModel("EXACT", stringResource(R.string.split_type_exact)),
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
            entitySplits = previewEntitySplits(),
            isEqualMode = true,
            isPercentMode = false,
            availableSplitTypes = previewSplitTypes(),
            events = PREVIEW_EVENTS
        )
    }
}

@PreviewComplete
@Composable
private fun EntitySplitEditorExactPreview() {
    PreviewThemeWrapper {
        EntitySplitEditor(
            entitySplits = previewEntitySplits(),
            isEqualMode = false,
            isPercentMode = false,
            availableSplitTypes = previewSplitTypes(),
            events = PREVIEW_EVENTS,
            currencySymbol = "€"
        )
    }
}

@PreviewComplete
@Composable
private fun EntitySplitEditorPercentagePreview() {
    PreviewThemeWrapper {
        EntitySplitEditor(
            entitySplits = previewEntitySplits(),
            isEqualMode = false,
            isPercentMode = true,
            availableSplitTypes = previewSplitTypes(),
            events = PREVIEW_EVENTS,
            currencySymbol = "€"
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun EntitySplitEditorExactNarrowPreview() {
    PreviewThemeWrapper {
        EntitySplitEditor(
            entitySplits = previewEntitySplits(),
            isEqualMode = false,
            isPercentMode = false,
            availableSplitTypes = previewSplitTypes(),
            events = PREVIEW_EVENTS,
            currencySymbol = "€"
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun SplitEditorExactNarrowPreview() {
    PreviewThemeWrapper {
        SplitEditor(
            splits = previewEntitySplits().first().entityMembers,
            isEqualMode = false,
            isPercentMode = false,
            onAmountChanged = { _, _ -> },
            onPercentageChanged = { _, _ -> },
            onExcludedToggled = {},
            onSoloSelected = {},
            onShareLockToggled = {},
            currencySymbol = "€"
        )
    }
}
