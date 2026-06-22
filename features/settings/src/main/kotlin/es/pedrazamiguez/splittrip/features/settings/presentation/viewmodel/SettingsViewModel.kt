package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.domain.enums.AppLanguage
import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import es.pedrazamiguez.splittrip.domain.enums.Currency
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignOutUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ConsumeLanguagePillUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppLanguageUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppThemeUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetShouldShowLanguagePillUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsViewModel(
    private val signOutUseCase: SignOutUseCase,
    private val getUserDefaultCurrencyUseCase: GetUserDefaultCurrencyUseCase,
    private val getAppLanguageUseCase: GetAppLanguageUseCase,
    private val getShouldShowLanguagePillUseCase: GetShouldShowLanguagePillUseCase,
    private val consumeLanguagePillUseCase: ConsumeLanguagePillUseCase,
    private val getAppThemeUseCase: GetAppThemeUseCase,
    private val isUserAnonymousUseCase: IsUserAnonymousUseCase
) : ViewModel() {

    val isAnonymous: StateFlow<Boolean> = isUserAnonymousUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = false
    )

    val currentCurrency: StateFlow<Currency?> = getUserDefaultCurrencyUseCase().map { code ->
        try {
            Currency.fromString(code)
        } catch (_: Exception) {
            Currency.EUR // Fallback to EUR if parsing fails
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = null
    )

    val currentLanguageCode: StateFlow<String> = getAppLanguageUseCase().map { langCode ->
        AppLanguage.fromCode(langCode).code
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = AppLanguage.fromCode(null).code
    )

    val currentThemeCode: StateFlow<String> = getAppThemeUseCase().map { themeCode ->
        AppTheme.fromCode(themeCode).code
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = AppTheme.fromCode(null).code
    )

    val shouldShowLanguagePill: StateFlow<Boolean> = getShouldShowLanguagePillUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = false
    )

    fun consumeLanguagePill() {
        viewModelScope.launch {
            consumeLanguagePillUseCase()
        }
    }

    private val _hasNotificationPermission = MutableStateFlow(false)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    fun updateNotificationPermission(hasPermission: Boolean) {
        _hasNotificationPermission.value = hasPermission
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            signOutUseCase()
                .onSuccess { onSignedOut() }
                .onFailure { Timber.e(it, "Sign-out failed") }
        }
    }
}
