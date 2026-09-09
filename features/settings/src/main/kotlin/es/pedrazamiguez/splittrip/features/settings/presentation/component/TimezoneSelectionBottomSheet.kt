package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.features.settings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimezoneSelectionBottomSheet(
    timezones: List<String>,
    onTimezoneSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredZones = remember(searchQuery, timezones) {
        if (searchQuery.isBlank()) {
            timezones
        } else {
            timezones.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        ),
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets.safeDrawing }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = MaterialTheme.spacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimezoneSelectionHeader()

            StyledOutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = stringResource(R.string.notification_prefs_select_timezone),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.Default,
                        vertical = MaterialTheme.spacing.Small
                    )
            )

            if (filteredZones.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.notification_prefs_no_timezones_found),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredZones, key = { it }) { zone ->
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.debouncedClickable {
                                onTimezoneSelected(zone)
                            }
                        ) {
                            Text(zone)
                        }
                    }
                }
            }
        }
    }
}
