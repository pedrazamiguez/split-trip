package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing * 1.5f,
        modifier = Modifier.padding(
            start = 32.dp,
            end = 16.dp,
            top = 24.dp,
            bottom = 8.dp
        )
    )
}
