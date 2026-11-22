package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepInfoBottomSheet
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep

@Composable
internal fun ExpenseStepInfoBottomSheet(
    step: AddExpenseStep,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    WizardStepInfoBottomSheet(
        titleRes = step.infoTitleRes,
        descriptionRes = step.infoDescriptionRes,
        infoItems = step.infoItems,
        onDismiss = onDismiss,
        modifier = modifier
    )
}
