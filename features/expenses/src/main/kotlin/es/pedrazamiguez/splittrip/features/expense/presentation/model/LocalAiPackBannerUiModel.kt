package es.pedrazamiguez.splittrip.features.expense.presentation.model

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

data class LocalAiPackBannerUiModel(
    val title: UiText,
    val description: UiText,
    val sizeBadge: String,
    val actionLabel: UiText
)
