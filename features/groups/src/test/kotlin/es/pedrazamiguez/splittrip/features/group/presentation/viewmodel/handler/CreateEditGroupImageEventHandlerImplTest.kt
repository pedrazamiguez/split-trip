package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateEditGroupImageEventHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var groupImageStorageService: GroupImageStorageService
    private lateinit var featureGateService: FeatureGateService
    private lateinit var stateFlow: MutableStateFlow<CreateEditGroupUiState>
    private lateinit var actionsFlow: MutableSharedFlow<CreateEditGroupUiAction>
    private lateinit var handler: CreateEditGroupImageEventHandlerImpl

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        groupImageStorageService = mockk(relaxed = true)
        featureGateService = mockk(relaxed = true) {
            every { isFeatureEnabled(any()) } returns flowOf(true)
        }
        stateFlow = MutableStateFlow(CreateEditGroupUiState())
        actionsFlow = MutableSharedFlow(replay = 1)
        handler = CreateEditGroupImageEventHandlerImpl(groupImageStorageService, featureGateService)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class HandleGroupImagePicked {

        @Test
        fun `saves temp image and updates state on success`() = runTest(testDispatcher) {
            coEvery { groupImageStorageService.saveTempGroupImage("uri-123") } returns "temp-uri-123"
            handler.bind(stateFlow, actionsFlow, this)

            handler.handleGroupImagePicked("uri-123")
            advanceUntilIdle()

            assertEquals("temp-uri-123", stateFlow.value.localGroupImagePath)
            assertFalse(stateFlow.value.isLoading)
        }

        @Test
        fun `sets error on image processing failure`() = runTest(testDispatcher) {
            coEvery { groupImageStorageService.saveTempGroupImage(any()) } throws RuntimeException("Fail")
            handler.bind(stateFlow, actionsFlow, this)

            handler.handleGroupImagePicked("uri-123")
            advanceUntilIdle()

            assertNull(stateFlow.value.localGroupImagePath)
            assertNotNull(stateFlow.value.error)
            assertFalse(stateFlow.value.isLoading)
        }
    }

    @Nested
    inner class HandleGroupImageRemoved {

        @Test
        fun `clears local image path and image url`() = runTest(testDispatcher) {
            stateFlow.value = CreateEditGroupUiState(
                localGroupImagePath = "file:///temp/img.webp",
                imageUrl = "https://storage/img.webp"
            )
            handler.bind(stateFlow, actionsFlow, this)

            handler.handleGroupImageRemoved()

            assertNull(stateFlow.value.localGroupImagePath)
            assertNull(stateFlow.value.imageUrl)
        }
    }

    @Nested
    inner class HandleShowImageSourceSheet {

        @Test
        fun `shows image source sheet`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)

            handler.handleShowImageSourceSheet(true)
            assertTrue(stateFlow.value.showImageSourceSheet)

            handler.handleShowImageSourceSheet(false)
            assertFalse(stateFlow.value.showImageSourceSheet)
        }
    }

    @Nested
    inner class CleanTempImages {

        @Test
        fun `calls cleanTempGroupImages on service`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)

            handler.cleanTempImages()
            advanceUntilIdle()

            coVerify(exactly = 1) { groupImageStorageService.cleanTempGroupImages() }
        }

        @Test
        fun `handles cleanTempGroupImages failure gracefully`() = runTest(testDispatcher) {
            coEvery { groupImageStorageService.cleanTempGroupImages() } throws RuntimeException("Cleanup failed")
            handler.bind(stateFlow, actionsFlow, this)

            handler.cleanTempImages()
            advanceUntilIdle()

            // No exception propagated
            coVerify(exactly = 1) { groupImageStorageService.cleanTempGroupImages() }
        }
    }
}
