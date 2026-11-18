package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText

private const val LABEL_WEIGHT = 0.4f
private const val VALUE_WEIGHT = 0.6f

/**
 * Canonical key-value summary row for wizard `ReviewStep` screens inside a `SectionCard`.
 *
 * Allocates `40%` width to [label] (`onSurfaceVariant`) and `60%` width to end-aligned [value]
 * (`onSurface`, `FontWeight.Medium`) with `Medium` (`12.dp`) horizontal separation so long
 * values wrap cleanly without colliding with labels on compact screens.
 */
@Composable
fun WizardReviewRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        BodyText(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(LABEL_WEIGHT)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(VALUE_WEIGHT)
        )
    }
}
