package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet

import androidx.compose.runtime.Immutable

@Immutable
data class ProFeaturePreviewPrompt(
    val previewType: ProFeaturePreviewType,
    val entrySource: String
)
