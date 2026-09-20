package es.pedrazamiguez.splittrip.features.subunit.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.subunit.presentation.screen.SubunitManagementScreen
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.SubunitManagementUiState
import kotlinx.collections.immutable.persistentListOf

@PreviewComplete
@Composable
fun SubunitManagementScreenLoadingPreview() {
    PreviewThemeWrapper {
        SubunitManagementScreen(
            uiState = SubunitManagementUiState(
                isLoading = true
            )
        )
    }
}

@PreviewComplete
@Composable
fun SubunitManagementScreenEmptyPreview() {
    PreviewThemeWrapper {
        SubunitManagementScreen(
            uiState = SubunitManagementUiState(
                isLoading = false,
                subunits = persistentListOf()
            )
        )
    }
}

@PreviewComplete
@Composable
fun SubunitManagementScreenWithDataPreview() {
    PreviewThemeWrapper {
        SubunitManagementScreen(
            uiState = SubunitManagementUiState(
                isLoading = false,
                groupId = "group-1",
                groupName = "Summer Trip",
                subunits = PREVIEW_SUBUNIT_UI_MODELS
            )
        )
    }
}

@PreviewComplete
@Composable
fun SubunitManagementScreenArchivedGroupPreview() {
    PreviewThemeWrapper {
        SubunitManagementScreen(
            uiState = SubunitManagementUiState(
                isLoading = false,
                groupId = "group-1",
                groupName = "Summer Trip",
                subunits = PREVIEW_SUBUNIT_UI_MODELS,
                isGroupArchived = true,
                isSubunitCreationEnabled = false
            )
        )
    }
}
