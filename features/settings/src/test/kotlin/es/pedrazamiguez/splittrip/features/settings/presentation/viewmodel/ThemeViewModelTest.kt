package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppThemeUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetAppThemeUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAppThemeUseCase: GetAppThemeUseCase
    private lateinit var setAppThemeUseCase: SetAppThemeUseCase
    private lateinit var viewModel: ThemeViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAppThemeUseCase = mockk()
        setAppThemeUseCase = mockk(relaxed = true)
        every { getAppThemeUseCase() } returns flowOf("system")
        viewModel = ThemeViewModel(
            getAppThemeUseCase = getAppThemeUseCase,
            setAppThemeUseCase = setAppThemeUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class SelectedThemeCode {

        @Test
        fun `emits theme code from use case flow`() = runTest(testDispatcher) {
            val collectJob = launch { viewModel.selectedThemeCode.collect {} }
            advanceUntilIdle()

            assertEquals("system", viewModel.selectedThemeCode.value)
            collectJob.cancel()
        }
    }

    @Nested
    inner class AvailableThemes {

        @Test
        fun `exposes all AppTheme entries`() {
            assertEquals(AppTheme.entries.size, viewModel.availableThemes.size)
        }
    }

    @Nested
    inner class OnThemeSelected {

        @Test
        fun `delegates to set use case`() = runTest(testDispatcher) {
            viewModel.onThemeSelected("dark")
            advanceUntilIdle()

            coVerify { setAppThemeUseCase("dark") }
        }
    }
}
