package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppLanguageUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetDeveloperInfoUseCase
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.DeveloperInfoUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.model.DeveloperInfoUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DeveloperInfoViewModel(
    getDeveloperInfoUseCase: GetDeveloperInfoUseCase,
    getAppLanguageUseCase: GetAppLanguageUseCase,
    private val developerInfoUiMapper: DeveloperInfoUiMapper
) : ViewModel() {

    val uiState: StateFlow<DeveloperInfoUiState> = combine(
        getDeveloperInfoUseCase(),
        getAppLanguageUseCase()
    ) { developerInfo, languageCode ->
        developerInfoUiMapper.mapToUiState(developerInfo, languageCode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = developerInfoUiMapper.mapToUiState(getDeveloperInfoUseCase().value, null)
    )

    companion object {
        private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
