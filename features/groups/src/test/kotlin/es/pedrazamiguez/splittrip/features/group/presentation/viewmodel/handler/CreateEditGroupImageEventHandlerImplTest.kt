package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewType
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.MediaUploadQuota
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private lateinit var telemetryTracker: TelemetryTracker
    private lateinit var stateFlow: MutableStateFlow<CreateEditGroupUiState>
    private lateinit var actionsFlow: MutableSharedFlow<CreateEditGroupUiAction>
    private lateinit var handler: CreateEditGroupImageEventHandlerImpl

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        groupImageStorageService = mockk(relaxed = true)
        featureGateService = mockk(relaxed = true) {
            every { observeMediaUploadQuota() } returns flowOf(
                MediaUploadQuota(usedUploads = 0, monthlyFreeLimit = 5, isProTier = false)
            )
        }
        telemetryTracker = mockk(relaxed = true)
        stateFlow = MutableStateFlow(CreateEditGroupUiState())
        actionsFlow = MutableSharedFlow(replay = 1)
        handler = CreateEditGroupImageEventHandlerImpl(groupImageStorageService, featureGateService, telemetryTracker)
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

        @Test
        fun `sets proPreviewPrompt when monthly media upload quota is exhausted`() = runTest(testDispatcher) {
            every { featureGateService.observeMediaUploadQuota() } returns flowOf(
                MediaUploadQuota(usedUploads = 5, monthlyFreeLimit = 5, isProTier = false)
            )
            handler.bind(stateFlow, actionsFlow, this)

            val actionsList = mutableListOf<CreateEditGroupUiAction>()
            val job = launch { actionsFlow.collect { actionsList.add(it) } }

            handler.handleGroupImagePicked("uri-123")
            advanceUntilIdle()

            coVerify(exactly = 0) { groupImageStorageService.saveTempGroupImage(any()) }
            assertNull(stateFlow.value.localGroupImagePath)
            assertEquals(
                ProFeaturePreviewPrompt(
                    previewType = ProFeaturePreviewType.GROUP_COVER_PHOTO,
                    entrySource = MonetizationTelemetry.EntrySource.GROUP_COVER_UPLOAD
                ),
                stateFlow.value.proPreviewPrompt
            )
            assertFalse(stateFlow.value.isLoading)
            assertTrue(actionsList.none { it is CreateEditGroupUiAction.ShowError })
            verify(exactly = 1) {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                    mapOf(
                        MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.LIMIT,
                        MonetizationTelemetry.Param.GATE_NAME to "manual_receipt_monthly_attachments",
                        MonetizationTelemetry.Param.ENTRY_POINT to
                            MonetizationTelemetry.GateEntryPoint.GROUP_COVER_PICK,
                        MonetizationTelemetry.Param.UPGRADE_PROMPTED to true
                    )
                )
            }
            job.cancel()
        }

        @Test
        fun `saves temp image when initial group is set and quota allows upload`() = runTest(testDispatcher) {
            val group = Group(id = "group-abc")
            coEvery { groupImageStorageService.saveTempGroupImage("uri-123") } returns "temp-uri-123"
            handler.setInitialGroup(group)
            handler.bind(stateFlow, actionsFlow, this)

            handler.handleGroupImagePicked("uri-123")
            advanceUntilIdle()

            assertEquals("temp-uri-123", stateFlow.value.localGroupImagePath)
            verify { featureGateService.observeMediaUploadQuota() }
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
