package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

/**
 * Standard outer wrapper for a single wizard step.
 *
 * Every step in the app's wizard flows uses the same padding and spacing.
 * Wrap your step content in this layout to enforce visual consistency
 * without repeating the boilerplate Column/padding/arrangement.
 *
 * @param modifier Optional modifier applied to the outer [Column].
 * @param content  Step content.
 */
@Composable
fun WizardStepLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.Large)
            .padding(top = MaterialTheme.spacing.ExtraLarge, bottom = MaterialTheme.spacing.ExtraLarge),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
        content = content
    )
}
