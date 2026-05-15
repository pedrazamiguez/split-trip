package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.CategorySection
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 6: Category selection.
 * Auto-advances to the next step after a category is selected.
 */
@Composable
fun CategoryStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onAutoAdvance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        CategorySection(
            availableCategories = uiState.availableCategories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { categoryId ->
                onEvent(AddExpenseUiEvent.CategorySelected(categoryId))
                onAutoAdvance()
            }
        )
    }
}
