package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split.SplitSection
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
private fun previewSplitSectionTypes(): ImmutableList<SplitTypeUiModel> = persistentListOf(
    SplitTypeUiModel("EQUAL", stringResource(R.string.split_type_equal)),
    SplitTypeUiModel("EXACT", stringResource(R.string.split_type_exact)),
    SplitTypeUiModel("PERCENT", stringResource(R.string.split_type_percent))
)

private val previewFlatSplits = persistentListOf(
    SplitUiModel(
        userId = "user-1",
        displayName = "Antonio García",
        amountCents = 3334L,
        formattedAmount = "33.34 €",
        amountInput = "33.34",
        percentageInput = "33.34"
    ),
    SplitUiModel(
        userId = "user-2",
        displayName = "María López",
        amountCents = 3333L,
        formattedAmount = "33.33 €",
        amountInput = "33.33",
        percentageInput = "33.33"
    ),
    SplitUiModel(
        userId = "user-3",
        displayName = "Andrés Pedraza",
        amountCents = 3333L,
        formattedAmount = "33.33 €",
        amountInput = "33.33",
        percentageInput = "33.33"
    )
)

@Composable
private fun previewSubunitEntitySplits(splitTypes: ImmutableList<SplitTypeUiModel>): ImmutableList<SplitUiModel> =
    persistentListOf(
        SplitUiModel(
            userId = "subunit-1",
            displayName = "Cantalobos",
            amountCents = 5000L,
            formattedAmount = "50.00 €",
            amountInput = "50.00",
            percentageInput = "50",
            isEntityRow = true,
            isExpanded = true,
            entitySplitType = splitTypes[1],
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
private fun SplitSectionFlatPreview() {
    PreviewThemeWrapper {
        val splitTypes = previewSplitSectionTypes()
        val euro = CurrencyUiModel("EUR", "EUR - Euro", 2)
        SplitSection(
            uiState = AddExpenseUiState(
                sourceAmount = "100.00",
                selectedCurrency = euro,
                groupCurrency = euro,
                availableSplitTypes = splitTypes,
                selectedSplitType = splitTypes.first(),
                splits = previewFlatSplits
            ),
            onEvent = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewComplete
@Composable
private fun SplitSectionSubunitExpandedPreview() {
    PreviewThemeWrapper {
        val splitTypes = previewSplitSectionTypes()
        val euro = CurrencyUiModel("EUR", "EUR - Euro", 2)
        SplitSection(
            uiState = AddExpenseUiState(
                sourceAmount = "100.00",
                selectedCurrency = euro,
                groupCurrency = euro,
                availableSplitTypes = splitTypes,
                selectedSplitType = splitTypes.first(),
                hasSubunits = true,
                isSubunitMode = true,
                entitySplits = previewSubunitEntitySplits(splitTypes)
            ),
            onEvent = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun SplitSectionSubunitNarrowPreview() {
    PreviewThemeWrapper {
        val splitTypes = previewSplitSectionTypes()
        val euro = CurrencyUiModel("EUR", "EUR - Euro", 2)
        SplitSection(
            uiState = AddExpenseUiState(
                sourceAmount = "100.00",
                selectedCurrency = euro,
                groupCurrency = euro,
                availableSplitTypes = splitTypes,
                selectedSplitType = splitTypes.first(),
                hasSubunits = true,
                isSubunitMode = true,
                entitySplits = previewSubunitEntitySplits(splitTypes)
            ),
            onEvent = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default)
        )
    }
}
