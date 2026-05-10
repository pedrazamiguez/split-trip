package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing * 1.5f,
        modifier = Modifier.padding(
            start = MaterialTheme.spacing.Section,
            end = MaterialTheme.spacing.Default,
            top = MaterialTheme.spacing.ExtraLarge,
            bottom = MaterialTheme.spacing.Small
        )
    )
}
