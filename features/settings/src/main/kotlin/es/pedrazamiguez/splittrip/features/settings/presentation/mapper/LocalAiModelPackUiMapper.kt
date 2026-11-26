package es.pedrazamiguez.splittrip.features.settings.presentation.mapper

import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.LocalAiModelPackUiState

interface LocalAiModelPackUiMapper {
    fun map(state: LocalAiModelPackState, showDeleteConfirmation: Boolean): LocalAiModelPackUiState
}
