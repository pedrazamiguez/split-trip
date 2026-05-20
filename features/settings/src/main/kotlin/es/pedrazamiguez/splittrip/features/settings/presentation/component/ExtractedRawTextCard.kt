package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
fun ExtractedRawTextCard(
    extractedText: String,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = stringResource(R.string.developer_services_extracted_raw_text),
        modifier = modifier
    ) {
        SelectionContainer {
            Text(
                text = extractedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
