package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.usecase.setting.CancelLocalAiModelPackDownloadUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.DeleteLocalAiModelPackUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.DownloadLocalAiModelPackUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObserveLocalAiModelPackStateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLocalAiModelPackWifiOnlyUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.LocalAiModelPackUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.LocalAiModelPackUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.LocalAiModelPackUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.LocalAiModelPackUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalAiModelPackViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var observeLocalAiModelPackStateUseCase: ObserveLocalAiModelPackStateUseCase
    private lateinit var downloadLocalAiModelPackUseCase: DownloadLocalAiModelPackUseCase
    private lateinit var cancelLocalAiModelPackDownloadUseCase: CancelLocalAiModelPackDownloadUseCase
    private lateinit var deleteLocalAiModelPackUseCase: DeleteLocalAiModelPackUseCase
    private lateinit var setLocalAiModelPackWifiOnlyUseCase: SetLocalAiModelPackWifiOnlyUseCase
    private lateinit var localAiModelPackUiMapper: LocalAiModelPackUiMapper
    private lateinit var viewModel: LocalAiModelPackViewModel

    private val domainStateFlow = MutableStateFlow(
        LocalAiModelPackState(
            status = ModelPackStatus.NotInstalled("1.0.0", 500_000_000L),
            compatibility = DeviceCompatibilityResult(
                isCompatible = true,
                reason = null,
                availableRamMb = 8192L,
                requiredRamMb = 4096,
                availableStorageBytes = 4_000_000_000L,
                requiredStorageBytes = 1_073_741_824L,
                supportedAbis = listOf("arm64-v8a")
            ),
            isEntitled = true,
            isWifiOnly = true,
            isAiCoreSupported = false,
            activeEngine = AiEngineType.AI_CORE_GEMMA_4
        )
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        observeLocalAiModelPackStateUseCase = mockk()
        downloadLocalAiModelPackUseCase = mockk()
        cancelLocalAiModelPackDownloadUseCase = mockk()
        deleteLocalAiModelPackUseCase = mockk()
        setLocalAiModelPackWifiOnlyUseCase = mockk()
        localAiModelPackUiMapper = mockk()

        every { observeLocalAiModelPackStateUseCase() } returns domainStateFlow
        every { localAiModelPackUiMapper.map(any(), any()) } answers {
            val state = firstArg<LocalAiModelPackState>()
            val showDialog = secondArg<Boolean>()
            LocalAiModelPackUiState(
                isLoading = false,
                isInstalled = state.status is ModelPackStatus.Installed,
                showDeleteConfirmation = showDialog
            )
        }

        viewModel = LocalAiModelPackViewModel(
            observeLocalAiModelPackStateUseCase = observeLocalAiModelPackStateUseCase,
            downloadLocalAiModelPackUseCase = downloadLocalAiModelPackUseCase,
            cancelLocalAiModelPackDownloadUseCase = cancelLocalAiModelPackDownloadUseCase,
            deleteLocalAiModelPackUseCase = deleteLocalAiModelPackUseCase,
            setLocalAiModelPackWifiOnlyUseCase = setLocalAiModelPackWifiOnlyUseCase,
            localAiModelPackUiMapper = localAiModelPackUiMapper
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `observes state flow and maps updates into uiState`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isInstalled)

        domainStateFlow.value = domainStateFlow.value.copy(
            status = ModelPackStatus.Installed("1.0.0", 500_000_000L)
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isInstalled)
    }

    @Test
    fun `RequestDownload invokes DownloadLocalAiModelPackUseCase and shows error pill on failure`() =
        runTest(testDispatcher) {
            coEvery { downloadLocalAiModelPackUseCase(null) } returns Result.failure(IllegalStateException("Network"))
            val actions = mutableListOf<LocalAiModelPackUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(LocalAiModelPackUiEvent.RequestDownload())
            advanceUntilIdle()
            job.cancel()

            coVerify(exactly = 1) { downloadLocalAiModelPackUseCase(null) }
            assertEquals(
                listOf(
                    LocalAiModelPackUiAction.ShowTopPill(
                        UiText.StringResource(R.string.local_ai_pack_pill_download_failed)
                    )
                ),
                actions
            )
        }

    @Test
    fun `RequestDownload with packId forwards packId to DownloadLocalAiModelPackUseCase`() =
        runTest(testDispatcher) {
            coEvery { downloadLocalAiModelPackUseCase("qwen3-0.6b-litert-v1") } returns Result.success(Unit)

            viewModel.onEvent(LocalAiModelPackUiEvent.RequestDownload("qwen3-0.6b-litert-v1"))
            advanceUntilIdle()

            coVerify(exactly = 1) { downloadLocalAiModelPackUseCase("qwen3-0.6b-litert-v1") }
            assertEquals(
                LocalAiModelPackUiAction.ShowTopPill(
                    UiText.StringResource(R.string.local_ai_pack_pill_download_started)
                ),
                viewModel.actions.first()
            )
        }

    @Test
    fun `RetryDownload with packId forwards packId and falls back to activeTargetPackId when null`() =
        runTest(testDispatcher) {
            every { localAiModelPackUiMapper.map(any(), any()) } returns LocalAiModelPackUiState(
                isLoading = false,
                activeTargetPackId = "qwen2.5-1.5b-litert-v1"
            )
            val reViewModel = LocalAiModelPackViewModel(
                observeLocalAiModelPackStateUseCase = observeLocalAiModelPackStateUseCase,
                downloadLocalAiModelPackUseCase = downloadLocalAiModelPackUseCase,
                cancelLocalAiModelPackDownloadUseCase = cancelLocalAiModelPackDownloadUseCase,
                deleteLocalAiModelPackUseCase = deleteLocalAiModelPackUseCase,
                setLocalAiModelPackWifiOnlyUseCase = setLocalAiModelPackWifiOnlyUseCase,
                localAiModelPackUiMapper = localAiModelPackUiMapper
            )
            advanceUntilIdle()
            coEvery { downloadLocalAiModelPackUseCase(any()) } returns Result.success(Unit)

            reViewModel.onEvent(LocalAiModelPackUiEvent.RetryDownload("gemma4-e2b-litert-v1"))
            reViewModel.onEvent(LocalAiModelPackUiEvent.RetryDownload())
            advanceUntilIdle()

            coVerify(exactly = 1) { downloadLocalAiModelPackUseCase("gemma4-e2b-litert-v1") }
            coVerify(exactly = 1) { downloadLocalAiModelPackUseCase("qwen2.5-1.5b-litert-v1") }
        }

    @Test
    fun `RequestDownload emits RequestUpgrade action when use case fails with LocalAiModelNotEntitledException`() =
        runTest(testDispatcher) {
            coEvery { downloadLocalAiModelPackUseCase(null) } returns Result.failure(LocalAiModelNotEntitledException())

            viewModel.onEvent(LocalAiModelPackUiEvent.RequestDownload())
            advanceUntilIdle()

            assertEquals(LocalAiModelPackUiAction.RequestUpgrade, viewModel.actions.first())
        }

    @Test
    fun `CancelDownload invokes CancelLocalAiModelPackDownloadUseCase`() = runTest(testDispatcher) {
        coEvery { cancelLocalAiModelPackDownloadUseCase(null) } returns Result.success(Unit)

        viewModel.onEvent(LocalAiModelPackUiEvent.CancelDownload())
        advanceUntilIdle()

        coVerify(exactly = 1) { cancelLocalAiModelPackDownloadUseCase(null) }
        assertEquals(
            LocalAiModelPackUiAction.ShowTopPill(
                UiText.StringResource(R.string.local_ai_pack_pill_download_cancelled)
            ),
            viewModel.actions.first()
        )
    }

    @Test
    fun `CancelDownload with packId forwards packId to CancelLocalAiModelPackDownloadUseCase`() =
        runTest(testDispatcher) {
            coEvery { cancelLocalAiModelPackDownloadUseCase("qwen3-0.6b-litert-v1") } returns Result.success(Unit)

            viewModel.onEvent(LocalAiModelPackUiEvent.CancelDownload("qwen3-0.6b-litert-v1"))
            advanceUntilIdle()

            coVerify(exactly = 1) { cancelLocalAiModelPackDownloadUseCase("qwen3-0.6b-litert-v1") }
        }

    @Test
    fun `RequestDelete opens confirmation dialog and DismissDeleteDialog closes it`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.onEvent(LocalAiModelPackUiEvent.RequestDelete)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.onEvent(LocalAiModelPackUiEvent.DismissDeleteDialog)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun `ConfirmDelete invokes DeleteLocalAiModelPackUseCase, closes dialog, and shows confirmation pill`() =
        runTest(testDispatcher) {
            coEvery { deleteLocalAiModelPackUseCase() } returns Result.success(Unit)
            viewModel.onEvent(LocalAiModelPackUiEvent.RequestDelete)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showDeleteConfirmation)

            viewModel.onEvent(LocalAiModelPackUiEvent.ConfirmDelete)
            advanceUntilIdle()

            coVerify(exactly = 1) { deleteLocalAiModelPackUseCase() }
            assertFalse(viewModel.uiState.value.showDeleteConfirmation)
            assertEquals(
                LocalAiModelPackUiAction.ShowTopPill(UiText.StringResource(R.string.local_ai_pack_pill_deleted)),
                viewModel.actions.first()
            )
        }

    @Test
    fun `ToggleWifiOnly invokes SetLocalAiModelPackWifiOnlyUseCase`() = runTest(testDispatcher) {
        coEvery { setLocalAiModelPackWifiOnlyUseCase(false) } just runs

        viewModel.onEvent(LocalAiModelPackUiEvent.ToggleWifiOnly(false))
        advanceUntilIdle()

        coVerify(exactly = 1) { setLocalAiModelPackWifiOnlyUseCase(false) }
    }

    @Test
    fun `RequestUpgrade emits RequestUpgrade action`() = runTest(testDispatcher) {
        viewModel.onEvent(LocalAiModelPackUiEvent.RequestUpgrade)
        advanceUntilIdle()

        assertEquals(LocalAiModelPackUiAction.RequestUpgrade, viewModel.actions.first())
    }
}
