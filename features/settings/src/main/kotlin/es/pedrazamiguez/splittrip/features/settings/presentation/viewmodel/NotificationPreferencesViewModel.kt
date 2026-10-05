package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.usecase.notification.GetNotificationPreferencesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UpdateNotificationPreferenceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserReminderPreferencesUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.NotificationPreferencesUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.model.NotificationPreferencesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.model.NotificationPreferencesUiState
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.NotificationPreferencesUiAction
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class NotificationPreferencesViewModel(
    private val getNotificationPreferencesUseCase: GetNotificationPreferencesUseCase,
    private val updateNotificationPreferenceUseCase: UpdateNotificationPreferenceUseCase,
    private val observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase,
    private val updateUserReminderPreferencesUseCase: UpdateUserReminderPreferencesUseCase,
    private val notificationPreferencesUiMapper: NotificationPreferencesUiMapper
) : ViewModel() {

    private val _actions = Channel<NotificationPreferencesUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeCurrentUserProfileUseCase().collect { profile ->
                if (profile != null && profile.timezone == null) {
                    try {
                        val deviceTimezone = ZoneId.systemDefault().id
                        updateUserReminderPreferencesUseCase(
                            userId = profile.userId,
                            timezone = deviceTimezone,
                            preferredReminderTime = profile.preferredReminderTime
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to auto-save default device timezone")
                    }
                }
            }
        }
    }

    val uiState: StateFlow<NotificationPreferencesUiState> = combine(
        getNotificationPreferencesUseCase(),
        observeCurrentUserProfileUseCase()
    ) { prefs, profile ->
        notificationPreferencesUiMapper.toUiState(prefs, profile)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = NotificationPreferencesUiState()
    )

    fun onEvent(event: NotificationPreferencesUiEvent) {
        when (event) {
            is NotificationPreferencesUiEvent.ToggleCategory -> handleToggleCategory(event)
            is NotificationPreferencesUiEvent.UpdateTimezone -> handleUpdateTimezone(event)
            is NotificationPreferencesUiEvent.UpdateReminderTime -> handleUpdateReminderTime(event)
        }
    }

    private fun handleToggleCategory(event: NotificationPreferencesUiEvent.ToggleCategory) {
        viewModelScope.launch {
            try {
                updateNotificationPreferenceUseCase(event.category, event.enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to update notification preference")
                _actions.send(
                    NotificationPreferencesUiAction.ShowTopPill(
                        UiText.StringResource(R.string.notification_prefs_update_failed)
                    )
                )
            }
        }
    }

    private fun handleUpdateTimezone(event: NotificationPreferencesUiEvent.UpdateTimezone) {
        viewModelScope.launch {
            try {
                val profile = observeCurrentUserProfileUseCase().firstOrNull()
                if (profile != null) {
                    updateUserReminderPreferencesUseCase(
                        userId = profile.userId,
                        timezone = event.timezone,
                        preferredReminderTime = profile.preferredReminderTime
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to update timezone preference")
                _actions.send(
                    NotificationPreferencesUiAction.ShowTopPill(
                        UiText.StringResource(R.string.notification_prefs_update_failed)
                    )
                )
            }
        }
    }

    private fun handleUpdateReminderTime(event: NotificationPreferencesUiEvent.UpdateReminderTime) {
        viewModelScope.launch {
            try {
                val profile = observeCurrentUserProfileUseCase().firstOrNull()
                if (profile != null) {
                    val formattedTime = notificationPreferencesUiMapper.formatTime(event.hour, event.minute)
                    updateUserReminderPreferencesUseCase(
                        userId = profile.userId,
                        timezone = profile.timezone,
                        preferredReminderTime = formattedTime
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to update reminder time preference")
                _actions.send(
                    NotificationPreferencesUiAction.ShowTopPill(
                        UiText.StringResource(R.string.notification_prefs_update_failed)
                    )
                )
            }
        }
    }
}
