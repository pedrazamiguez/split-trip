package es.pedrazamiguez.splittrip.core.designsystem.preview.wizard

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepInfoBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepInfoItem
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import kotlinx.collections.immutable.persistentListOf

@PreviewComplete
@Composable
private fun WizardStepInfoBottomSheetPlainPreview() {
    PreviewThemeWrapper {
        WizardStepInfoBottomSheet(
            titleRes = R.string.preview_wizard_step_info_title,
            descriptionRes = R.string.preview_wizard_step_info_description,
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun WizardStepInfoBottomSheetStructuredPreview() {
    PreviewThemeWrapper {
        WizardStepInfoBottomSheet(
            titleRes = R.string.preview_wizard_step_info_title,
            descriptionRes = R.string.preview_wizard_step_info_description,
            infoItems = persistentListOf(
                WizardStepInfoItem(
                    titleRes = R.string.preview_wizard_step_info_item_one_title,
                    descriptionRes = R.string.preview_wizard_step_info_item_one_description
                ),
                WizardStepInfoItem(
                    titleRes = R.string.preview_wizard_step_info_item_two_title,
                    descriptionRes = R.string.preview_wizard_step_info_item_two_description
                )
            ),
            onDismiss = {}
        )
    }
}
