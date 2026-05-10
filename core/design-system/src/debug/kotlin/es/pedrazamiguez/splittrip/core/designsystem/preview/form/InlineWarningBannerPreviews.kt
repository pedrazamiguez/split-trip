package es.pedrazamiguez.splittrip.core.designsystem.preview.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.InlineWarningBanner
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

@PreviewThemes
@Composable
private fun InlineWarningBannerVisiblePreview() {
    PreviewThemeWrapper {
        InlineWarningBanner(
            warning = UiText.DynamicString("You're spending from personal cash but others are included in the split."),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun InlineWarningBannerNullPreview() {
    PreviewThemeWrapper {
        InlineWarningBanner(
            warning = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}
