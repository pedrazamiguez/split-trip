package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateGroupImageEventHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var groupImageStorageService: GroupImageStorageService
    private lateinit var handler: CreateGroupImageEventHandlerImpl
    private lateinit var stateFlow: MutableStateFlow<CreateGroupUiState>
    private lateinit var actionsFlow: MutableSharedFlow<CreateGroupUiAction>

    @BeforeEach
    fun setUp() {
        groupImageStorageService = mockk(relaxed = true)
        handler = CreateGroupImageEventHandlerImpl(groupImageStorageService)
        stateFlow = MutableStateFlow(CreateGroupUiState())
        actionsFlow = MutableSharedFlow()
    }

    @Test
    fun `handleGroupImagePicked success updates state with path`() = runTest(testDispatcher) {
        // Given
        handler.bind(stateFlow, actionsFlow, this)
        val pickedUri = "content://picker/image.jpg"
        val tempUri = "file:///temp/group_temp.webp"
        coEvery { groupImageStorageService.saveTempGroupImage(pickedUri) } returns tempUri

        // When
        handler.handleGroupImagePicked(pickedUri)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { groupImageStorageService.saveTempGroupImage(pickedUri) }
        assertEquals(tempUri, stateFlow.value.localGroupImagePath)
        assertFalse(stateFlow.value.isLoading)
        assertNull(stateFlow.value.error)
    }

    @Test
    fun `handleGroupImagePicked failure sets error state`() = runTest(testDispatcher) {
        // Given
        handler.bind(stateFlow, actionsFlow, this)
        val pickedUri = "content://picker/image.jpg"
        coEvery { groupImageStorageService.saveTempGroupImage(pickedUri) } throws RuntimeException("Failed processing")

        // When
        handler.handleGroupImagePicked(pickedUri)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { groupImageStorageService.saveTempGroupImage(pickedUri) }
        assertNull(stateFlow.value.localGroupImagePath)
        assertNotNull(stateFlow.value.error)
        assertFalse(stateFlow.value.isLoading)
    }

    @Test
    fun `handleGroupImageRemoved clears localGroupImagePath`() = runTest(testDispatcher) {
        // Given
        handler.bind(stateFlow, actionsFlow, this)
        stateFlow.value = stateFlow.value.copy(localGroupImagePath = "file:///path.webp")

        // When
        handler.handleGroupImageRemoved()

        // Then
        assertNull(stateFlow.value.localGroupImagePath)
    }

    @Test
    fun `handleShowImageSourceSheet updates showImageSourceSheet state`() = runTest(testDispatcher) {
        // Given
        handler.bind(stateFlow, actionsFlow, this)

        // When
        handler.handleShowImageSourceSheet(true)

        // Then
        assertTrue(stateFlow.value.showImageSourceSheet)

        // When
        handler.handleShowImageSourceSheet(false)

        // Then
        assertFalse(stateFlow.value.showImageSourceSheet)
    }

    @Test
    fun `cleanTempImages calls groupImageStorageService`() = runTest(testDispatcher) {
        // Given
        handler.bind(stateFlow, actionsFlow, this)

        // When
        handler.cleanTempImages()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { groupImageStorageService.cleanTempGroupImages() }
    }
}
