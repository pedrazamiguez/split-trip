package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt

sealed interface CreateEditSubunitUiAction {
    data class ShowSuccess(val message: UiText) : CreateEditSubunitUiAction
    data class ShowError(val message: UiText) : CreateEditSubunitUiAction
    data object NavigateBack : CreateEditSubunitUiAction
    data object RequestExitConfirmation : CreateEditSubunitUiAction
    data class ShowProFeaturePreview(
        val prompt: ProFeaturePreviewPrompt,
        val navigateBackOnDismiss: Boolean = false
    ) : CreateEditSubunitUiAction
}
