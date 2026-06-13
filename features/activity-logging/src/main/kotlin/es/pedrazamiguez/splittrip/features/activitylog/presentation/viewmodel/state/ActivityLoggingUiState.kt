package es.pedrazamiguez.splittrip.features.activitylog.presentation.viewmodel.state

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ActivityLoggingUiState(
    val isLoading: Boolean = false,
    val activities: ImmutableList<String> = persistentListOf()
)
