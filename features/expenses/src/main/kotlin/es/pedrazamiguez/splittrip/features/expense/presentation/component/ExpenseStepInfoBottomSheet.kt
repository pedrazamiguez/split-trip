package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepInfoBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepInfoItem
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun ExpenseStepInfoBottomSheet(
    step: AddExpenseStep,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    WizardStepInfoBottomSheet(
        titleRes = step.infoTitleRes,
        descriptionRes = step.infoDescriptionRes,
        items = step.infoItems.map { item ->
            WizardStepInfoItem(
                titleRes = item.titleRes,
                descriptionRes = item.descriptionRes
            )
        }.toImmutableList(),
        onDismiss = onDismiss,
        modifier = modifier
    )
}
