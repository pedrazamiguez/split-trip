package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.NotificationCategory
import es.pedrazamiguez.splittrip.domain.model.NotificationPreferences
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.notification.GetNotificationPreferencesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UpdateNotificationPreferenceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserReminderPreferencesUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.NotificationPreferencesUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.model.NotificationPreferencesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.model.NotificationPreferencesUiState
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.NotificationPreferencesUiAction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("NotificationPreferencesViewModel")
class NotificationPreferencesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getPreferencesUseCase: GetNotificationPreferencesUseCase
    private lateinit var updatePreferenceUseCase: UpdateNotificationPreferenceUseCase
    private lateinit var observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase
    private lateinit var updateUserReminderPreferencesUseCase: UpdateUserReminderPreferencesUseCase
    private lateinit var uiMapper: NotificationPreferencesUiMapper
    private lateinit var viewModel: NotificationPreferencesViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getPreferencesUseCase = mockk()
        updatePreferenceUseCase = mockk(relaxed = true)
        observeCurrentUserProfileUseCase = mockk(relaxed = true)
        updateUserReminderPreferencesUseCase = mockk(relaxed = true)
        uiMapper = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        prefs: NotificationPreferences = NotificationPreferences(),
        user: User = User(
            userId = "testUser",
            email = "test@test.com",
            displayName = "Test User"
        )
    ): NotificationPreferencesViewModel {
        every { getPreferencesUseCase() } returns flowOf(prefs)
        every { observeCurrentUserProfileUseCase() } returns flowOf(user)
        every { uiMapper.toUiState(any(), any()) } answers {
            val p = firstArg<NotificationPreferences>()
            val u = secondArg<User?>()
            NotificationPreferencesUiState(
                membershipEnabled = p.membershipEnabled,
                expensesEnabled = p.expensesEnabled,
                financialEnabled = p.financialEnabled,
                timezone = u?.timezone,
                preferredReminderTime = u?.preferredReminderTime,
                isLoading = false
            )
        }
        every { uiMapper.formatTime(any(), any()) } answers {
            val hour = firstArg<Int>()
            val minute = secondArg<Int>()
            String.format(Locale.ROOT, "%02d:%02d", hour, minute)
        }
        return NotificationPreferencesViewModel(
            getPreferencesUseCase,
            updatePreferenceUseCase,
            observeCurrentUserProfileUseCase,
            updateUserReminderPreferencesUseCase,
            uiMapper
        )
    }

    @Nested
    @DisplayName("uiState")
    inner class UiStateTests {

        @Test
        fun `initial state has isLoading true`() = runTest(testDispatcher) {
            viewModel = createViewModel()
            val state = viewModel.uiState.value
            assertTrue(state.isLoading)
        }

        @Test
        fun `emits preferences from use case`() = runTest(testDispatcher) {
            val prefs = NotificationPreferences(
                membershipEnabled = false,
                expensesEnabled = true,
                financialEnabled = false
            )
            viewModel = createViewModel(prefs)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.membershipEnabled)
            assertTrue(state.expensesEnabled)
            assertFalse(state.financialEnabled)

            collectJob.cancel()
        }

        @Test
        fun `all defaults are true`() = runTest(testDispatcher) {
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.membershipEnabled)
            assertTrue(state.expensesEnabled)
            assertTrue(state.financialEnabled)

            collectJob.cancel()
        }

        @Test
        fun `ToggleCategory EXPENSES delegates to use case`() = runTest(testDispatcher) {
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.onEvent(
                NotificationPreferencesUiEvent.ToggleCategory(
                    NotificationCategory.EXPENSES,
                    true
                )
            )
            advanceUntilIdle()

            coVerify { updatePreferenceUseCase(NotificationCategory.EXPENSES, true) }
            collectJob.cancel()
        }

        @Test
        fun `ToggleCategory FINANCIAL delegates to use case`() = runTest(testDispatcher) {
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.onEvent(
                NotificationPreferencesUiEvent.ToggleCategory(
                    NotificationCategory.FINANCIAL,
                    false
                )
            )
            advanceUntilIdle()

            coVerify { updatePreferenceUseCase(NotificationCategory.FINANCIAL, false) }
            collectJob.cancel()
        }

        @Test
        fun `UpdateTimezone delegates to use case`() = runTest(testDispatcher) {
            val user = User(
                userId = "testUser",
                email = "test@test.com",
                displayName = "Test User",
                preferredReminderTime = "10:00"
            )
            viewModel = createViewModel(user = user)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.onEvent(
                NotificationPreferencesUiEvent.UpdateTimezone(
                    timezone = "Europe/London"
                )
            )
            advanceUntilIdle()

            coVerify { updateUserReminderPreferencesUseCase("testUser", "Europe/London", "10:00") }
            collectJob.cancel()
        }

        @Test
        fun `UpdateReminderTime delegates to use case`() = runTest(testDispatcher) {
            val user = User(
                userId = "testUser",
                email = "test@test.com",
                displayName = "Test User",
                timezone = "Europe/London"
            )
            viewModel = createViewModel(user = user)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.onEvent(
                NotificationPreferencesUiEvent.UpdateReminderTime(
                    hour = 10,
                    minute = 30
                )
            )
            advanceUntilIdle()

            coVerify { updateUserReminderPreferencesUseCase("testUser", "Europe/London", "10:30") }
            collectJob.cancel()
        }

        @Test
        fun `automatically saves device timezone on load if user timezone is null`() = runTest(testDispatcher) {
            val userWithoutTimezone = User(
                userId = "testUser",
                email = "test@test.com",
                displayName = "Test User",
                timezone = null,
                preferredReminderTime = "09:00"
            )
            viewModel = createViewModel(user = userWithoutTimezone)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val expectedTimezone = ZoneId.systemDefault().id
            coVerify { updateUserReminderPreferencesUseCase("testUser", expectedTimezone, "09:00") }
            collectJob.cancel()
        }

        @Test
        fun `does not overwrite timezone on load if user timezone is already saved`() = runTest(testDispatcher) {
            val userWithTimezone = User(
                userId = "testUser",
                email = "test@test.com",
                displayName = "Test User",
                timezone = "America/New_York",
                preferredReminderTime = "09:00"
            )
            viewModel = createViewModel(user = userWithTimezone)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            coVerify(exactly = 0) { updateUserReminderPreferencesUseCase(any(), any(), any()) }
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("error handling")
    inner class ErrorHandling {

        @Test
        fun `toggle does not crash on use case exception`() = runTest(testDispatcher) {
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            coEvery {
                updatePreferenceUseCase(any(), any())
            } throws RuntimeException("Network error")

            viewModel.onEvent(
                NotificationPreferencesUiEvent.ToggleCategory(
                    NotificationCategory.MEMBERSHIP,
                    false
                )
            )
            advanceUntilIdle()

            // Should not crash — error is caught and logged
            assertEquals(true, viewModel.uiState.value.membershipEnabled)
            collectJob.cancel()
        }

        @Test
        fun `toggle handles CancellationException gracefully`() = runTest(testDispatcher) {
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            coEvery {
                updatePreferenceUseCase(any(), any())
            } throws CancellationException("Scope cancelled")

            // CancellationException is rethrown (not swallowed by the generic catch),
            // and the launch builder handles it as normal coroutine cancellation.
            // The ViewModel should remain functional afterwards.
            viewModel.onEvent(
                NotificationPreferencesUiEvent.ToggleCategory(
                    NotificationCategory.MEMBERSHIP,
                    false
                )
            )
            advanceUntilIdle()

            // ViewModel is still alive and responsive
            assertFalse(viewModel.uiState.value.isLoading)
            collectJob.cancel()
        }

        @Test
        fun `ToggleCategory emits ShowTopPill action on use case exception`() = runTest(testDispatcher) {
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            coEvery {
                updatePreferenceUseCase(any(), any())
            } throws RuntimeException("Storage error")

            val actions = mutableListOf<NotificationPreferencesUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(
                NotificationPreferencesUiEvent.ToggleCategory(
                    NotificationCategory.MEMBERSHIP,
                    false
                )
            )
            advanceUntilIdle()

            assertEquals(1, actions.size)
            val action = assertInstanceOf(NotificationPreferencesUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.notification_prefs_update_failed, message.resId)

            job.cancel()
            collectJob.cancel()
        }

        @Test
        fun `UpdateTimezone emits ShowTopPill action on use case exception`() = runTest(testDispatcher) {
            val user = User(
                userId = "testUser",
                email = "test@test.com",
                displayName = "Test User",
                preferredReminderTime = "10:00"
            )
            viewModel = createViewModel(user = user)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            coEvery {
                updateUserReminderPreferencesUseCase(any(), any(), any())
            } throws RuntimeException("Network error")

            val actions = mutableListOf<NotificationPreferencesUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(
                NotificationPreferencesUiEvent.UpdateTimezone(
                    timezone = "Europe/London"
                )
            )
            advanceUntilIdle()

            assertEquals(1, actions.size)
            val action = assertInstanceOf(NotificationPreferencesUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.notification_prefs_update_failed, message.resId)

            job.cancel()
            collectJob.cancel()
        }

        @Test
        fun `UpdateReminderTime emits ShowTopPill action on use case exception`() = runTest(testDispatcher) {
            val user = User(
                userId = "testUser",
                email = "test@test.com",
                displayName = "Test User",
                timezone = "Europe/London"
            )
            viewModel = createViewModel(user = user)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            coEvery {
                updateUserReminderPreferencesUseCase(any(), any(), any())
            } throws RuntimeException("Network error")

            val actions = mutableListOf<NotificationPreferencesUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(
                NotificationPreferencesUiEvent.UpdateReminderTime(
                    hour = 10,
                    minute = 30
                )
            )
            advanceUntilIdle()

            assertEquals(1, actions.size)
            val action = assertInstanceOf(NotificationPreferencesUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.notification_prefs_update_failed, message.resId)

            job.cancel()
            collectJob.cancel()
        }
    }
}
