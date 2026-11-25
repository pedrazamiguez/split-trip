package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.service.featuregate.AiReceiptQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.MediaUploadQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import es.pedrazamiguez.splittrip.domain.service.featuregate.ReportExportQuota
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetReceiptExtractionCapabilityUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObservePlanAllowancesUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.PlanAllowancesUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceUsageStatus
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.PlanAllowancesUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.PlanAllowancesUiEvent
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanAllowancesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var observePlanAllowancesUseCase: ObservePlanAllowancesUseCase
    private lateinit var getReceiptExtractionCapabilityUseCase: GetReceiptExtractionCapabilityUseCase
    private lateinit var localeProvider: LocaleProvider
    private lateinit var mapper: PlanAllowancesUiMapperImpl
    private lateinit var upstreamFlow: MutableSharedFlow<PlanAllowances>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        observePlanAllowancesUseCase = mockk()
        getReceiptExtractionCapabilityUseCase = mockk()
        localeProvider = mockk()
        every { localeProvider.getCurrentLocale() } returns Locale.ENGLISH
        every { getReceiptExtractionCapabilityUseCase() } returns ExtractionCapability.ON_DEVICE_AI
        mapper = PlanAllowancesUiMapperImpl(localeProvider = localeProvider)
        upstreamFlow = MutableSharedFlow(replay = 1)
        every { observePlanAllowancesUseCase() } returns upstreamFlow
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PlanAllowancesViewModel = PlanAllowancesViewModel(
        observePlanAllowancesUseCase = observePlanAllowancesUseCase,
        getReceiptExtractionCapabilityUseCase = getReceiptExtractionCapabilityUseCase,
        planAllowancesUiMapper = mapper
    )

    @Test
    fun `initial uiState has isLoading true before upstream emits`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `uiState emits mapped Free state from observePlanAllowancesUseCase`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        upstreamFlow.emit(createPlanAllowances(tier = SubscriptionTier.FREE, usedScans = 1, usedUploads = 2))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isProTier)
        assertTrue(state.showUpgradeCta)
        assertEquals(4, state.quotas.size)
        assertEquals(AllowanceUsageStatus.HEALTHY, state.quotas[0].status)

        collectJob.cancel()
    }

    @Test
    fun `uiState emits mapped Pro state when upstream updates from Free to Pro`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        upstreamFlow.emit(createPlanAllowances(tier = SubscriptionTier.FREE))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isProTier)

        upstreamFlow.emit(
            createPlanAllowances(
                tier = SubscriptionTier.PRO,
                usedScans = 12,
                aiMonthlyLimit = null,
                usedUploads = 18,
                maxOwnedGroups = null,
                maxMembersPerGroup = 20,
                maxSubunitsPerGroup = null,
                isAdFreeEnabled = true
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isProTier)
        assertFalse(state.showUpgradeCta)
        assertEquals(AllowanceUsageStatus.UNLIMITED, state.quotas[0].status)
        assertTrue(state.quotas[0].showProgressBar)
        assertEquals(AllowanceUsageStatus.UNLIMITED, state.quotas[1].status)
        assertEquals(AllowanceUsageStatus.UNLIMITED, state.quotas[3].status)
        assertTrue(state.quotas.all { it.showProgressBar })

        collectJob.cancel()
    }

    @Test
    fun `uiState emits mapped exhausted state when quotas reach limits`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        upstreamFlow.emit(
            createPlanAllowances(
                tier = SubscriptionTier.FREE,
                usedScans = 3,
                aiMonthlyLimit = 3,
                usedUploads = 5,
                mediaMonthlyFreeLimit = 5,
                usedExports = 1,
                reportExportMonthlyFreeLimit = 1,
                ownedGroupsUsed = 1,
                maxOwnedGroups = 1
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.quotas.all { it.status == AllowanceUsageStatus.EXHAUSTED })

        collectJob.cancel()
    }

    @Test
    fun `uiState emits mapped ungated state when subscriptionGatingEnabled becomes false`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        upstreamFlow.emit(
            createPlanAllowances(
                tier = SubscriptionTier.FREE,
                isGatingEnabled = false,
                maxSubunitsPerGroup = null,
                isAdFreeEnabled = true
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isGatingEnabled)
        assertFalse(state.showUpgradeCta)
        assertTrue(state.quotas.all { it.status == AllowanceUsageStatus.UNLIMITED && it.showProgressBar })

        collectJob.cancel()
    }

    @Test
    fun `onEvent RequestUpgrade emits NavigateToUpgrade action`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onEvent(PlanAllowancesUiEvent.RequestUpgrade)
        val action = viewModel.actions.first()

        assertEquals(PlanAllowancesUiAction.NavigateToUpgrade, action)
    }

    @Test
    fun `uiState maps AI_RECEIPT_SCANS as UNSUPPORTED_ON_DEVICE and adapts upgrade description`() =
        runTest(testDispatcher) {
            every { getReceiptExtractionCapabilityUseCase() } returns ExtractionCapability.UNSUPPORTED
            val viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            upstreamFlow.emit(createPlanAllowances(tier = SubscriptionTier.FREE))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(AllowanceUsageStatus.UNSUPPORTED_ON_DEVICE, state.quotas[0].status)
            assertNull(state.quotas[0].usageText)
            assertFalse(state.quotas[0].showProgressBar)
            assertEquals(
                UiText.StringResource(R.string.plan_allowances_upgrade_description_no_ai),
                state.upgradeDescription
            )

            collectJob.cancel()
        }

    @Test
    fun `uiState maps AI_RECEIPT_SCANS as UNSUPPORTED_ON_DEVICE for Pro tier when device is UNSUPPORTED`() =
        runTest(testDispatcher) {
            every { getReceiptExtractionCapabilityUseCase() } returns ExtractionCapability.UNSUPPORTED
            val viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            upstreamFlow.emit(
                createPlanAllowances(
                    tier = SubscriptionTier.PRO,
                    usedScans = 0,
                    aiMonthlyLimit = null,
                    maxOwnedGroups = null,
                    maxSubunitsPerGroup = null
                )
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(AllowanceUsageStatus.UNSUPPORTED_ON_DEVICE, state.quotas[0].status)
            assertEquals(AllowanceUsageStatus.UNLIMITED, state.quotas[1].status)

            collectJob.cancel()
        }

    @Suppress("LongParameterList")
    private fun createPlanAllowances(
        tier: SubscriptionTier = SubscriptionTier.FREE,
        isGatingEnabled: Boolean = true,
        usedScans: Int = 1,
        aiMonthlyLimit: Int? = 3,
        usedUploads: Int = 2,
        mediaMonthlyFreeLimit: Int = 5,
        usedExports: Int = 0,
        reportExportMonthlyFreeLimit: Int = 1,
        ownedGroupsUsed: Int = 0,
        maxOwnedGroups: Int? = 1,
        maxMembersPerGroup: Int = 4,
        maxSubunitsPerGroup: Int? = 1,
        isAdFreeEnabled: Boolean = tier == SubscriptionTier.PRO || !isGatingEnabled
    ): PlanAllowances {
        val isPro = tier == SubscriptionTier.PRO
        return PlanAllowances(
            tier = tier,
            isGatingEnabled = isGatingEnabled,
            aiReceiptQuota = AiReceiptQuota(
                usedScans = usedScans,
                monthlyLimit = aiMonthlyLimit,
                isProTier = isPro,
                isGatingEnabled = isGatingEnabled
            ),
            mediaUploadQuota = MediaUploadQuota(
                usedUploads = usedUploads,
                monthlyFreeLimit = mediaMonthlyFreeLimit,
                isProTier = isPro,
                isGatingEnabled = isGatingEnabled
            ),
            reportExportQuota = ReportExportQuota(
                usedExports = usedExports,
                monthlyFreeLimit = reportExportMonthlyFreeLimit,
                isProTier = isPro,
                isGatingEnabled = isGatingEnabled
            ),
            ownedGroupsUsed = ownedGroupsUsed,
            maxOwnedGroups = maxOwnedGroups,
            maxMembersPerGroup = maxMembersPerGroup,
            maxSubunitsPerGroup = maxSubunitsPerGroup,
            isAdFreeEnabled = isAdFreeEnabled
        )
    }
}
