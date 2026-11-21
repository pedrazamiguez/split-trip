package es.pedrazamiguez.splittrip.core.designsystem.preview.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.AnimatedProgressBar
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper

@PreviewComplete
@Composable
private fun AnimatedProgressBarStatesPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Empty (0f)
            AnimatedProgressBar(
                progress = 0f,
                fillColor = MaterialTheme.colorScheme.secondary
            )
            // Healthy (0.33f)
            AnimatedProgressBar(
                progress = 0.33f,
                fillColor = MaterialTheme.colorScheme.secondary
            )
            // Approaching limit (0.80f)
            AnimatedProgressBar(
                progress = 0.80f,
                fillColor = MaterialTheme.colorScheme.tertiary
            )
            // Exhausted (1f)
            AnimatedProgressBar(
                progress = 1f,
                fillColor = MaterialTheme.colorScheme.error
            )
        }
    }
}
