package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BuildingBank
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.domain.enums.NotificationCategory
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.NotificationPreferencesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.model.NotificationPreferencesUiState

@Suppress("LongMethod") // Compose UI builder DSL
@Composable
fun NotificationPreferencesScreen(
    uiState: NotificationPreferencesUiState = NotificationPreferencesUiState(),
    onEvent: (NotificationPreferencesUiEvent) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.Small),
        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.Small)
    ) {
        item(key = "header") {
            BodyText(
                text = stringResource(R.string.notification_prefs_header),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.Default,
                    vertical = MaterialTheme.spacing.Medium
                )
            )
        }

        item(key = "membership") {
            NotificationCategoryItem(
                icon = { Icon(TablerIcons.Outline.UsersGroup, contentDescription = null) },
                title = stringResource(R.string.notification_prefs_membership_title),
                description = stringResource(R.string.notification_prefs_membership_description),
                checked = uiState.membershipEnabled,
                onCheckedChange = { enabled ->
                    onEvent(
                        NotificationPreferencesUiEvent.ToggleCategory(
                            NotificationCategory.MEMBERSHIP,
                            enabled
                        )
                    )
                }
            )
        }

        item(key = "expenses") {
            NotificationCategoryItem(
                icon = { Icon(TablerIcons.Outline.Receipt, contentDescription = null) },
                title = stringResource(R.string.notification_prefs_expenses_title),
                description = stringResource(R.string.notification_prefs_expenses_description),
                checked = uiState.expensesEnabled,
                onCheckedChange = { enabled ->
                    onEvent(
                        NotificationPreferencesUiEvent.ToggleCategory(
                            NotificationCategory.EXPENSES,
                            enabled
                        )
                    )
                }
            )
        }

        item(key = "financial") {
            NotificationCategoryItem(
                icon = { Icon(TablerIcons.Outline.BuildingBank, contentDescription = null) },
                title = stringResource(R.string.notification_prefs_financial_title),
                description = stringResource(R.string.notification_prefs_financial_description),
                checked = uiState.financialEnabled,
                onCheckedChange = { enabled ->
                    onEvent(
                        NotificationPreferencesUiEvent.ToggleCategory(
                            NotificationCategory.FINANCIAL,
                            enabled
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun NotificationCategoryItem(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        leadingContent = icon,
        headlineContent = { Text(title) },
        supportingContent = {
            SecondaryBodyText(text = description, maxLines = Int.MAX_VALUE)
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}
