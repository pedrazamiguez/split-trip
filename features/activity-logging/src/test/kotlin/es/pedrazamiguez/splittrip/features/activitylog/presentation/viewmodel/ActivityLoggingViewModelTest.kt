package es.pedrazamiguez.splittrip.features.activitylog.presentation.viewmodel

import es.pedrazamiguez.splittrip.features.activitylog.presentation.viewmodel.event.ActivityLoggingUiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityLoggingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ActivityLoggingViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ActivityLoggingViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenInitialState_isLoadingIsFalseAndActivitiesIsEmpty() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.activities.isEmpty())
    }

    @Test
    fun whenRefreshEventReceived_stateRemainsConsistent() = runTest(testDispatcher) {
        viewModel.onEvent(ActivityLoggingUiEvent.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.activities.isEmpty())
    }
}
