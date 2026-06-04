package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.enums.AppLanguage
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppLanguageUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetAppLanguageUseCase
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
class LanguageViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAppLanguageUseCase: GetAppLanguageUseCase
    private lateinit var setAppLanguageUseCase: SetAppLanguageUseCase
    private lateinit var viewModel: LanguageViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getAppLanguageUseCase = mockk()
        setAppLanguageUseCase = mockk(relaxed = true)
        every { getAppLanguageUseCase() } returns flowOf("en")
        viewModel = LanguageViewModel(
            getAppLanguageUseCase = getAppLanguageUseCase,
            setAppLanguageUseCase = setAppLanguageUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class SelectedLanguageCode {

        @Test
        fun `emits language code from use case flow`() = runTest(testDispatcher) {
            val collectJob = launch { viewModel.selectedLanguageCode.collect {} }
            advanceUntilIdle()

            assertEquals("en", viewModel.selectedLanguageCode.value)
            collectJob.cancel()
        }
    }

    @Nested
    inner class AvailableLanguages {

        @Test
        fun `exposes all AppLanguage entries`() {
            assertEquals(AppLanguage.entries.size, viewModel.availableLanguages.size)
        }
    }

    @Nested
    inner class OnLanguageSelected {

        @Test
        fun `delegates to set use case`() = runTest(testDispatcher) {
            viewModel.onLanguageSelected("es")
            advanceUntilIdle()

            coVerify { setAppLanguageUseCase("es") }
        }
    }
}
