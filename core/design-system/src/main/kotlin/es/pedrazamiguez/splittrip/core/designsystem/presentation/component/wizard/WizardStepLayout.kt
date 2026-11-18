package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

/**
 * Standard outer wrapper for a single wizard step.
 *
 * Uses `MaterialTheme.spacing.Large` (`20.dp`) horizontal padding on standard screens (`> 360dp`)
 * and `MaterialTheme.spacing.Default` (`16.dp`) on narrow screens (`<= 360dp`), with
 * `ExtraLarge` (`24.dp`) vertical padding and `Default` (`16.dp`) inter-section spacing.
 *
 * @param modifier Optional modifier applied to the outer [Column].
 * @param content  Step content.
 */
@Composable
fun WizardStepLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isNarrowScreen = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val horizontalPadding = if (isNarrowScreen) {
        MaterialTheme.spacing.Default
    } else {
        MaterialTheme.spacing.Large
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = MaterialTheme.spacing.ExtraLarge),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
        content = content
    )
}
