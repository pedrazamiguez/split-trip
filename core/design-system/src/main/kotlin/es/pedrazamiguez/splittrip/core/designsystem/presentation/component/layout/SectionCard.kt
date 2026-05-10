package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

/**
 * Reusable card for grouped content sections across the app.
 *
 * Renders an optional section [title] above a [FlatCard] that uses the
 * `surfaceContainerLow` container colour and `shapes.large` corner radius.
 * The card body is a [Column] with consistent internal padding and spacing.
 *
 * @param modifier Optional modifier applied to the outer wrapper.
 * @param title    Optional section title rendered above the card.
 * @param content  Card body content.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.Large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
                content = content
            )
        }
    }
}
