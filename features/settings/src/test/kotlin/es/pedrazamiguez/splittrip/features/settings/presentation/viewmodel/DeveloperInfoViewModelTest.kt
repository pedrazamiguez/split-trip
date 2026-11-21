package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.model.DeveloperInfo
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppLanguageUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetDeveloperInfoUseCase
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.DeveloperInfoUiMapper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperInfoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleInfo = DeveloperInfo(
        name = "Andrés Pedraza Míguez",
        avatarUrl = "https://example.com/avatar.jpg",
        githubUrl = "https://github.com/pedrazamiguez",
        splitTripRepoUrl = "https://github.com/pedrazamiguez/split-trip",
        linkedinUrl = "https://www.linkedin.com/in/andres-pedraza-miguez/",
        portfolioUrl = "https://pedrazamiguez.es",
        roleMap = mapOf(
            "en" to "Lead Mobile & Systems Engineer",
            "es" to "Ingeniero Principal de Sistemas y Móviles"
        ),
        bioMap = mapOf(
            "en" to "Passionate Android Engineer.",
            "es" to "Ingeniero Android apasionado."
        ),
        creditsMap = mapOf(
            "en" to "Built with open-source love.",
            "es" to "Creado con amor open-source."
        ),
        copyrightMap = mapOf(
            "en" to "© 2026 Andrés Pedraza Míguez. All rights reserved.",
            "es" to "© 2026 Andrés Pedraza Míguez. Todos los derechos reservados."
        )
    )

    private val getDeveloperInfoUseCase: GetDeveloperInfoUseCase = mockk()
    private val getAppLanguageUseCase: GetAppLanguageUseCase = mockk()
    private val mapper = DeveloperInfoUiMapper()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val infoFlow = MutableStateFlow(sampleInfo)
        every { getDeveloperInfoUseCase() } returns infoFlow
        every { getAppLanguageUseCase() } returns flowOf("es")
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits mapped state using language and developer info`() = runTest(testDispatcher) {
        val viewModel = DeveloperInfoViewModel(
            getDeveloperInfoUseCase = getDeveloperInfoUseCase,
            getAppLanguageUseCase = getAppLanguageUseCase,
            developerInfoUiMapper = mapper
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("Andrés Pedraza Míguez", viewModel.uiState.value.name)
        assertEquals("Ingeniero Principal de Sistemas y Móviles", viewModel.uiState.value.role)
        assertEquals("Ingeniero Android apasionado.", viewModel.uiState.value.bio)
        assertEquals("Creado con amor open-source.", viewModel.uiState.value.credits)
        assertEquals("© 2026 Andrés Pedraza Míguez. Todos los derechos reservados.", viewModel.uiState.value.copyright)

        collectJob.cancel()
    }
}
