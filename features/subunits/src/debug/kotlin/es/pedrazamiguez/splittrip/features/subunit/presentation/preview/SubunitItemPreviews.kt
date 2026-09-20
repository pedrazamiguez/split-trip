package es.pedrazamiguez.splittrip.features.subunit.presentation.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.SubunitItem

@PreviewComplete
@Composable
fun SubunitItemEvenSplitPreview() {
    PreviewThemeWrapper {
        SubunitItem(
            subunitUiModel = PREVIEW_SUBUNIT_UI_MODELS[0],
            modifier = Modifier.padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewComplete
@Composable
fun SubunitItemManualSharesPreview() {
    PreviewThemeWrapper {
        SubunitItem(
            subunitUiModel = PREVIEW_SUBUNIT_UI_MODELS[1],
            modifier = Modifier.padding(MaterialTheme.spacing.Default)
        )
    }
}
