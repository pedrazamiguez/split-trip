package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.features.expense.presentation.component.AutoFillBanner
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AutoFillBanner as AutoFillBannerState
import kotlinx.collections.immutable.persistentListOf

@PreviewComplete
@Composable
private fun AutoFillBannerPreview() {
    PreviewThemeWrapper {
        AutoFillBanner(
            banner = AutoFillBannerState(
                fields = persistentListOf(
                    UiText.DynamicString("Total: 45.00 €"),
                    UiText.DynamicString("Vendor: Thai Bistro"),
                    UiText.DynamicString("Date: 15/01/2026")
                ),
                source = ExtractionSource.AI_CORE
            ),
            onDismiss = {}
        )
    }
}
