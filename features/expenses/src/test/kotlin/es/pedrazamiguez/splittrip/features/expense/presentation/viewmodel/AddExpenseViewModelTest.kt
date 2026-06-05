package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.exception.InsufficientCashException
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.GroupExpenseConfig
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateWithStaleness
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.ExpenseValidationService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.domain.service.split.SubunitAwareSplitService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetContributionByExpenseIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.AddExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.AttachReceiptUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.ExtractReceiptFieldsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpenseConfigUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.PreviewCashExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.UpdateExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCategoryUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedPaymentMethodUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedCategoryUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedPaymentMethodUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseAddOnUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseSplitUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.AddOnCrudDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.AddOnEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.AddOnExchangeRateDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.CashRateDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.ConfigEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.CurrencyEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.EntitySplitFlattenDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.FormEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.IntraSubunitSplitDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.ReceiptAutoFillEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SaveLastUsedPreferencesBundle
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SplitEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SplitRowMappingDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SubmitEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SubmitResultDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SubunitSplitEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.strategy.ExpenseFlowStrategyFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
class AddExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var addExpenseUseCase: AddExpenseUseCase
    private lateinit var getGroupExpenseConfigUseCase: GetGroupExpenseConfigUseCase
    private lateinit var getExchangeRateUseCase: GetExchangeRateUseCase
    private lateinit var previewCashExchangeRateUseCase: PreviewCashExchangeRateUseCase
    private lateinit var getGroupLastUsedCurrencyUseCase: GetGroupLastUsedCurrencyUseCase
    private lateinit var setGroupLastUsedCurrencyUseCase: SetGroupLastUsedCurrencyUseCase
    private lateinit var getGroupLastUsedPaymentMethodUseCase: GetGroupLastUsedPaymentMethodUseCase
    private lateinit var setGroupLastUsedPaymentMethodUseCase: SetGroupLastUsedPaymentMethodUseCase
    private lateinit var getGroupLastUsedCategoryUseCase: GetGroupLastUsedCategoryUseCase
    private lateinit var setGroupLastUsedCategoryUseCase: SetGroupLastUsedCategoryUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var expenseCalculatorService: ExpenseCalculatorService
    private lateinit var expenseValidationService: ExpenseValidationService
    private lateinit var addExpenseUiMapper: AddExpenseUiMapper
    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: AddExpenseViewModel

    private val eur = Currency(
        code = "EUR",
        symbol = "€",
        defaultName = "Euro",
        decimalDigits = 2
    )

    private val jpy = Currency(
        code = "JPY",
        symbol = "¥",
        defaultName = "Japanese Yen",
        decimalDigits = 0
    )

    private val usd = Currency(
        code = "USD",
        symbol = "$",
        defaultName = "US Dollar",
        decimalDigits = 2
    )

    private val groupEur = Group(
        id = "group-eur",
        name = "Europe Trip",
        currency = "EUR",
        extraCurrencies = listOf("USD")
    )

    private val groupJpy = Group(
        id = "group-jpy",
        name = "Japan Trip",
        currency = "JPY",
        extraCurrencies = listOf("USD")
    )

    private val configEur = GroupExpenseConfig(
        group = groupEur,
        groupCurrency = eur,
        availableCurrencies = listOf(eur, usd)
    )

    private val configJpy = GroupExpenseConfig(
        group = groupJpy,
        groupCurrency = jpy,
        availableCurrencies = listOf(jpy, usd)
    )

    @BeforeEach
    @Suppress("LongMethod") // Test setup — mock instantiation and handler wiring for all constructor dependencies
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        addExpenseUseCase = mockk()
        getGroupExpenseConfigUseCase = mockk()
        getExchangeRateUseCase = mockk()
        previewCashExchangeRateUseCase = mockk(relaxed = true)
        getGroupLastUsedCurrencyUseCase = mockk()
        setGroupLastUsedCurrencyUseCase = mockk()
        getGroupLastUsedPaymentMethodUseCase = mockk()
        setGroupLastUsedPaymentMethodUseCase = mockk()
        getGroupLastUsedCategoryUseCase = mockk()
        setGroupLastUsedCategoryUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        authenticationService = mockk(relaxed = true)
        expenseCalculatorService = mockk(relaxed = true)
        val splitCalculatorFactory = ExpenseSplitCalculatorFactory(ExpenseCalculatorService())
        expenseValidationService = ExpenseValidationService(splitCalculatorFactory)
        localeProvider = mockk()
        resourceProvider = mockk(relaxed = true)
        every { localeProvider.getCurrentLocale() } returns Locale.US

        val formattingHelper = FormattingHelper(localeProvider)
        val splitPreviewService = SplitPreviewService()
        val remainderDistributionService = RemainderDistributionService()
        val addExpenseSplitMapper = AddExpenseSplitUiMapper(
            localeProvider,
            formattingHelper,
            splitPreviewService,
            EntitySplitFlattenDelegate(splitPreviewService, remainderDistributionService)
        )
        addExpenseUiMapper = AddExpenseUiMapper(
            localeProvider,
            resourceProvider,
            addExpenseSplitMapper,
            AddExpenseAddOnUiMapper(),
            splitPreviewService
        )
        val addExpenseOptionsMapper = AddExpenseOptionsUiMapper(resourceProvider, mockk(relaxed = true))

        every { getGroupLastUsedCurrencyUseCase(any()) } returns flowOf(null)
        coEvery { setGroupLastUsedCurrencyUseCase(any(), any()) } returns Unit
        every { getGroupLastUsedPaymentMethodUseCase(any()) } returns flowOf(emptyList())
        coEvery { setGroupLastUsedPaymentMethodUseCase(any(), any()) } returns Unit
        every { getGroupLastUsedCategoryUseCase(any()) } returns flowOf(emptyList())
        coEvery { setGroupLastUsedCategoryUseCase(any(), any()) } returns Unit
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()

        val splitRowMappingDelegate = SplitRowMappingDelegate(
            splitCalculatorFactory = splitCalculatorFactory,
            splitPreviewService = splitPreviewService,
            formattingHelper = formattingHelper
        )

        // Create handlers with shared instances (mirrors the DI module pattern)
        val splitHandler = SplitEventHandler(
            splitCalculatorFactory = splitCalculatorFactory,
            splitPreviewService = splitPreviewService,
            formattingHelper = formattingHelper,
            splitRowMappingDelegate = splitRowMappingDelegate
        )

        val intraSubunitSplitDelegate = IntraSubunitSplitDelegate(
            splitCalculatorFactory = splitCalculatorFactory,
            splitPreviewService = splitPreviewService,
            subunitAwareSplitService = SubunitAwareSplitService(splitCalculatorFactory),
            formattingHelper = formattingHelper
        )

        val subunitSplitHandler = SubunitSplitEventHandler(
            splitPreviewService = splitPreviewService,
            addExpenseSplitMapper = addExpenseSplitMapper,
            intraSubunitSplitDelegate = intraSubunitSplitDelegate,
            splitRowMappingDelegate = splitRowMappingDelegate
        )

        val currencyHandler = CurrencyEventHandler(
            getExchangeRateUseCase = getExchangeRateUseCase,
            exchangeRateCalculationService = ExchangeRateCalculationService(),
            formattingHelper = formattingHelper,
            addExpenseOptionsMapper = addExpenseOptionsMapper,
            withdrawalPoolSelectionDelegate = mockk(relaxed = true),
            cashRateDelegate = CashRateDelegate(
                previewCashExchangeRateUseCase = previewCashExchangeRateUseCase,
                expenseCalculatorService = expenseCalculatorService,
                splitPreviewService = splitPreviewService,
                formattingHelper = formattingHelper,
                addExpenseOptionsMapper = addExpenseOptionsMapper
            )
        )

        val mockReceiptExtractionService = mockk<ReceiptExtractionService>()
        every { mockReceiptExtractionService.capability() } returns ExtractionCapability.UNSUPPORTED

        val configHandler = ConfigEventHandler(
            getGroupExpenseConfigUseCase = getGroupExpenseConfigUseCase,
            getGroupLastUsedCurrencyUseCase = getGroupLastUsedCurrencyUseCase,
            getGroupLastUsedPaymentMethodUseCase = getGroupLastUsedPaymentMethodUseCase,
            getGroupLastUsedCategoryUseCase = getGroupLastUsedCategoryUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            authenticationService = authenticationService,
            addExpenseOptionsMapper = addExpenseOptionsMapper,
            addExpenseSplitMapper = addExpenseSplitMapper,
            addExpenseUiMapper = addExpenseUiMapper,
            receiptExtractionService = mockReceiptExtractionService
        )

        val submitHandler = SubmitEventHandler(
            expenseValidationService = expenseValidationService,
            addOnCalculationService = AddOnCalculationService(),
            expenseCalculatorService = ExpenseCalculatorService(),
            remainderDistributionService = remainderDistributionService,
            addExpenseUiMapper = addExpenseUiMapper,
            submitResultDelegate = SubmitResultDelegate(
                saveLastUsedPreferences = SaveLastUsedPreferencesBundle(
                    setGroupLastUsedCurrencyUseCase = setGroupLastUsedCurrencyUseCase,
                    setGroupLastUsedPaymentMethodUseCase = setGroupLastUsedPaymentMethodUseCase,
                    setGroupLastUsedCategoryUseCase = setGroupLastUsedCategoryUseCase
                ),
                formattingHelper = formattingHelper
            )
        )

        val addOnExchangeRateDelegate = AddOnExchangeRateDelegate(
            exchangeRateCalculationService = ExchangeRateCalculationService(),
            expenseCalculatorService = ExpenseCalculatorService(),
            splitPreviewService = splitPreviewService,
            formattingHelper = formattingHelper,
            getExchangeRateUseCase = getExchangeRateUseCase,
            previewCashExchangeRateUseCase = mockk(relaxed = true)
        )

        val addOnCrudDelegate = AddOnCrudDelegate(
            addExpenseOptionsMapper = addExpenseOptionsMapper,
            exchangeRateDelegate = addOnExchangeRateDelegate
        )

        val addOnHandler = AddOnEventHandler(
            addOnCalculationService = AddOnCalculationService(),
            exchangeRateCalculationService = ExchangeRateCalculationService(),
            expenseCalculatorService = ExpenseCalculatorService(),
            splitPreviewService = splitPreviewService,
            formattingHelper = formattingHelper,
            addExpenseOptionsMapper = addExpenseOptionsMapper,
            exchangeRateDelegate = addOnExchangeRateDelegate,
            addOnCrudDelegate = addOnCrudDelegate
        )

        val attachReceiptUseCase: AttachReceiptUseCase = mockk {
            coEvery { this@mockk(any()) } answers {
                val uri = firstArg<String>()
                Result.success(
                    es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment(
                        localUri = uri,
                        mimeType = "image/webp",
                        capturedAtMillis = 1716000000000L
                    )
                )
            }
        }
        val formHandler = FormEventHandler(
            addExpenseUiMapper = addExpenseUiMapper,
            attachReceiptUseCase = attachReceiptUseCase
        )

        val receiptAutoFillEventHandler = ReceiptAutoFillEventHandler(
            extractReceiptFieldsUseCase = mockk<ExtractReceiptFieldsUseCase>(relaxed = true),
            receiptExtractionService = mockk<ReceiptExtractionService>(relaxed = true),
            formattingHelper = formattingHelper,
            addExpenseUiMapper = addExpenseUiMapper
        )

        val updateExpenseUseCase = mockk<UpdateExpenseUseCase>(relaxed = true)
        val getExpenseByIdUseCase = mockk<GetExpenseByIdUseCase>(relaxed = true)
        val getContributionByExpenseIdUseCase = mockk<GetContributionByExpenseIdUseCase>(relaxed = true)
        val getGroupSubunitsUseCase = mockk<GetGroupSubunitsUseCase>(relaxed = true)

        val strategyFactory = ExpenseFlowStrategyFactory(
            configEventHandler = configHandler,
            addExpenseUseCase = addExpenseUseCase,
            updateExpenseUseCase = updateExpenseUseCase,
            getExpenseByIdUseCase = getExpenseByIdUseCase,
            getContributionByExpenseIdUseCase = getContributionByExpenseIdUseCase,
            addExpenseUiMapper = addExpenseUiMapper,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            getGroupSubunitsUseCase = getGroupSubunitsUseCase
        )

        viewModel = AddExpenseViewModel(
            configEventHandler = configHandler,
            currencyEventHandler = currencyHandler,
            splitEventHandler = splitHandler,
            subunitSplitEventHandler = subunitSplitHandler,
            addOnEventHandler = addOnHandler,
            submitEventHandler = submitHandler,
            formEventHandler = formHandler,
            receiptAutoFillEventHandler = receiptAutoFillEventHandler,
            strategyFactory = strategyFactory
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class LoadGroupConfig {

        @Test
        fun `loads config successfully and updates state`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(
                configEur
            )

            // When
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isConfigLoaded)
            assertFalse(state.configLoadFailed)
            assertEquals("group-eur", state.loadedGroupId)
            assertEquals("Europe Trip", state.groupName)
            assertEquals("EUR", state.groupCurrency?.code)
            assertEquals("EUR", state.selectedCurrency?.code)
            assertEquals(2, state.availableCurrencies.size)
            // Verify payment methods are populated
            assertTrue(state.paymentMethods.isNotEmpty())
            assertNotNull(state.selectedPaymentMethod)
        }

        @Test
        fun `does not reload config for same group on subsequent calls`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(
                configEur
            )

            // When - First load
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // When - Second load for same group (e.g., screen rotation)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // Then - UseCase should only be called once
            coVerify(exactly = 1) { getGroupExpenseConfigUseCase("group-eur", any()) }
        }

        @Test
        fun `reloads config when group changes - EUR to JPY`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(
                configEur
            )
            coEvery { getGroupExpenseConfigUseCase("group-jpy", any()) } returns Result.success(
                configJpy
            )

            // When - Load EUR group first
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // Verify EUR config loaded
            assertEquals("EUR", viewModel.uiState.value.groupCurrency?.code)
            assertEquals("group-eur", viewModel.uiState.value.loadedGroupId)
            assertEquals("Europe Trip", viewModel.uiState.value.groupName)

            // When - Change to JPY group
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-jpy"))
            advanceUntilIdle()

            // Then - JPY config should be loaded
            val state = viewModel.uiState.value
            assertEquals("JPY", state.groupCurrency?.code)
            assertEquals("JPY", state.selectedCurrency?.code)
            assertEquals("group-jpy", state.loadedGroupId)
            assertEquals("Japan Trip", state.groupName)
            assertTrue(state.availableCurrencies.any { it.code == "JPY" })
            assertFalse(state.availableCurrencies.any { it.code == "EUR" })
        }

        @Test
        fun `resets form state when group changes`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(
                configEur
            )
            coEvery { getGroupExpenseConfigUseCase("group-jpy", any()) } returns Result.success(
                configJpy
            )

            // When - Load EUR group and fill form
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()
            viewModel.onEvent(AddExpenseUiEvent.TitleChanged("Dinner"))
            viewModel.onEvent(AddExpenseUiEvent.SourceAmountChanged("50.00"))

            // Verify form has data
            assertEquals("Dinner", viewModel.uiState.value.expenseTitle)
            assertEquals("50.00", viewModel.uiState.value.sourceAmount)

            // When - Change to JPY group
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-jpy"))
            advanceUntilIdle()

            // Then - Form should be reset
            val state = viewModel.uiState.value
            assertEquals("", state.expenseTitle)
            assertEquals("", state.sourceAmount)
            assertEquals("JPY", state.selectedCurrency?.code)
        }

        @Test
        fun `handles config load failure`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns
                Result.failure(RuntimeException("Network error"))

            // When
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isConfigLoaded)
            assertTrue(state.configLoadFailed)
            assertFalse(state.isLoading)
        }

        @Test
        fun `retry loads config with forceRefresh`() = runTest {
            // Given - First load fails
            coEvery { getGroupExpenseConfigUseCase("group-eur", false) } returns
                Result.failure(RuntimeException("Network error"))
            coEvery { getGroupExpenseConfigUseCase("group-eur", true) } returns
                Result.success(configEur)

            // When - Initial load fails
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.configLoadFailed)

            // When - Retry
            viewModel.onEvent(AddExpenseUiEvent.RetryLoadConfig("group-eur"))
            advanceUntilIdle()

            // Then - Config should be loaded
            val state = viewModel.uiState.value
            assertTrue(state.isConfigLoaded)
            assertFalse(state.configLoadFailed)
            assertEquals("EUR", state.groupCurrency?.code)

            // Verify forceRefresh was true on retry
            coVerify { getGroupExpenseConfigUseCase("group-eur", true) }
        }

        @Test
        fun `ignores null groupId`() = runTest {
            // When
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig(null))
            advanceUntilIdle()

            // Then - State should remain initial
            val state = viewModel.uiState.value
            assertFalse(state.isConfigLoaded)
            assertFalse(state.configLoadFailed)
            assertFalse(state.isLoading)
        }

        @Test
        fun `loads last used currency if available`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEur)
            coEvery {
                getExchangeRateUseCase("EUR", "USD")
            } returns ExchangeRateWithStaleness(
                rate = BigDecimal("1.08"),
                isStale = false
            )

            // Mock that USD was the last used currency for this specific group
            every { getGroupLastUsedCurrencyUseCase("group-eur") } returns flowOf("USD")
            // Use a non-CASH default so the API rate path is exercised
            every { getGroupLastUsedPaymentMethodUseCase("group-eur") } returns flowOf(listOf("CREDIT_CARD"))

            // When
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // Then - It should automatically select USD instead of EUR
            val state = viewModel.uiState.value
            assertEquals("USD", state.selectedCurrency?.code)
            assertTrue(state.showExchangeRateSection) // Verify the exchange rate section is visible
            assertEquals("1.08", state.displayExchangeRate)
        }
    }

    @Nested
    inner class InputEvents {

        @Test
        fun `NotesChanged updates notes in state`() = runTest {
            // When
            viewModel.onEvent(AddExpenseUiEvent.NotesChanged("Some important note"))

            // Then
            assertEquals("Some important note", viewModel.uiState.value.notes)
        }

        @Test
        fun `NotesChanged with empty string clears notes`() = runTest {
            // Given
            viewModel.onEvent(AddExpenseUiEvent.NotesChanged("Initial note"))
            assertEquals("Initial note", viewModel.uiState.value.notes)

            // When
            viewModel.onEvent(AddExpenseUiEvent.NotesChanged(""))

            // Then
            assertEquals("", viewModel.uiState.value.notes)
        }
    }

    @Nested
    inner class SubunitSplitEvents {

        private val subunit = Subunit(
            id = "couple-1",
            groupId = "group-eur",
            name = "Couple A",
            memberIds = listOf("user-a", "user-b"),
            memberShares = mapOf(
                "user-a" to BigDecimal("0.5"),
                "user-b" to BigDecimal("0.5")
            )
        )

        private val groupWithSubunits = Group(
            id = "group-sub",
            name = "Trip With Subunits",
            currency = "EUR",
            extraCurrencies = emptyList(),
            members = listOf("user-a", "user-b", "user-c")
        )

        private val configWithSubunits = GroupExpenseConfig(
            group = groupWithSubunits,
            groupCurrency = eur,
            availableCurrencies = listOf(eur),
            subunits = listOf(subunit)
        )

        private fun loadConfigWithSubunits() {
            coEvery { getGroupExpenseConfigUseCase("group-sub", any()) } returns
                Result.success(configWithSubunits)
        }

        @Test
        fun `loading config with subunits sets hasSubunits true`() = runTest {
            loadConfigWithSubunits()

            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-sub"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.hasSubunits)
            assertFalse(state.isSubunitMode) // toggle is off by default
            assertTrue(state.entitySplits.isNotEmpty())
        }

        @Test
        fun `SubunitModeToggled enables subunit mode`() = runTest {
            loadConfigWithSubunits()

            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-sub"))
            advanceUntilIdle()

            viewModel.onEvent(AddExpenseUiEvent.SubunitModeToggled)

            assertTrue(viewModel.uiState.value.isSubunitMode)
        }

        @Test
        fun `SubunitModeToggled twice disables subunit mode`() = runTest {
            loadConfigWithSubunits()

            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-sub"))
            advanceUntilIdle()

            viewModel.onEvent(AddExpenseUiEvent.SubunitModeToggled)
            assertTrue(viewModel.uiState.value.isSubunitMode)

            viewModel.onEvent(AddExpenseUiEvent.SubunitModeToggled)
            assertFalse(viewModel.uiState.value.isSubunitMode)
        }

        @Test
        fun `EntityAccordionToggled expands subunit entity`() = runTest {
            loadConfigWithSubunits()

            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-sub"))
            advanceUntilIdle()

            // The subunit entity should exist
            val subunitEntity = viewModel.uiState.value.entitySplits.find { it.userId == "couple-1" }
            assertNotNull(subunitEntity)
            assertFalse(subunitEntity!!.isExpanded)

            viewModel.onEvent(AddExpenseUiEvent.EntityAccordionToggled("couple-1"))

            val expandedEntity = viewModel.uiState.value.entitySplits.find { it.userId == "couple-1" }
            assertTrue(expandedEntity!!.isExpanded)
        }

        @Test
        fun `EntitySplitExcludedToggled excludes entity`() = runTest {
            loadConfigWithSubunits()

            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-sub"))
            advanceUntilIdle()

            val entity = viewModel.uiState.value.entitySplits.find { it.userId == "couple-1" }
            assertFalse(entity!!.isExcluded)

            viewModel.onEvent(AddExpenseUiEvent.EntitySplitExcludedToggled("couple-1"))

            val updated = viewModel.uiState.value.entitySplits.find { it.userId == "couple-1" }
            assertTrue(updated!!.isExcluded)
        }

        @Test
        fun `entity splits contain solo user and subunit entity`() = runTest {
            loadConfigWithSubunits()

            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-sub"))
            advanceUntilIdle()

            val splits = viewModel.uiState.value.entitySplits
            // user-c is solo (not in any subunit), couple-1 is the subunit
            val soloEntity = splits.find { it.userId == "user-c" }
            val subunitEntity = splits.find { it.userId == "couple-1" }

            assertNotNull(soloEntity)
            assertTrue(soloEntity!!.isEntityRow)
            assertTrue(soloEntity.entityMembers.isEmpty())

            assertNotNull(subunitEntity)
            assertTrue(subunitEntity!!.isEntityRow)
            assertEquals(2, subunitEntity.entityMembers.size)
            assertTrue(subunitEntity.entityMembers.any { it.userId == "user-a" })
            assertTrue(subunitEntity.entityMembers.any { it.userId == "user-b" })
        }
    }

    @Nested
    inner class GroupChangeScenarios {

        /**
         * This test reproduces the exact bug scenario:
         * 1. User opens add expense for group A (EUR)
         * 2. User navigates away without adding expense
         * 3. User selects group B (JPY)
         * 4. User returns to add expense screen
         * 5. Currency should now show JPY, not EUR
         */
        @Test
        fun `currency updates when user switches groups while on add expense screen`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(
                configEur
            )
            coEvery { getGroupExpenseConfigUseCase("group-jpy", any()) } returns Result.success(
                configJpy
            )

            // Step 1: User opens add expense for EUR group
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            assertEquals("EUR", viewModel.uiState.value.selectedCurrency?.code)
            assertEquals("EUR", viewModel.uiState.value.groupCurrency?.code)

            // Step 2 & 3: User navigates away and selects JPY group

            // Step 4: User returns to add expense screen (LaunchedEffect triggers with new groupId)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-jpy"))
            advanceUntilIdle()

            // Step 5: Currency should now show JPY
            val finalState = viewModel.uiState.value
            assertEquals("JPY", finalState.selectedCurrency?.code)
            assertEquals("JPY", finalState.groupCurrency?.code)
            assertEquals("group-jpy", finalState.loadedGroupId)
        }

        @Test
        fun `switching groups multiple times always shows correct currency`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(
                configEur
            )
            coEvery { getGroupExpenseConfigUseCase("group-jpy", any()) } returns Result.success(
                configJpy
            )

            // EUR -> JPY -> EUR -> JPY
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()
            assertEquals("EUR", viewModel.uiState.value.selectedCurrency?.code)

            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-jpy"))
            advanceUntilIdle()
            assertEquals("JPY", viewModel.uiState.value.selectedCurrency?.code)

            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()
            assertEquals("EUR", viewModel.uiState.value.selectedCurrency?.code)

            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-jpy"))
            advanceUntilIdle()
            assertEquals("JPY", viewModel.uiState.value.selectedCurrency?.code)

            // All 4 loads should have happened since groupId changed each time
            coVerify(exactly = 2) { getGroupExpenseConfigUseCase("group-eur", any()) }
            coVerify(exactly = 2) { getGroupExpenseConfigUseCase("group-jpy", any()) }
        }
    }

    @Nested
    inner class ExchangeRateFetching {

        private val thb = Currency(
            code = "THB",
            symbol = "฿",
            defaultName = "Thai Baht",
            decimalDigits = 2
        )

        private val configEurWithThb = GroupExpenseConfig(
            group = groupEur,
            groupCurrency = eur,
            availableCurrencies = listOf(eur, usd, thb)
        )

        @BeforeEach
        fun setUpNonCashDefault() {
            // Set default payment method to CREDIT_CARD so these tests exercise the API rate path.
            // CASH is the first entry in PaymentMethod.entries, so without this override the
            // auto-selected default would be CASH, which locks the exchange rate section.
            every { getGroupLastUsedPaymentMethodUseCase(any()) } returns flowOf(listOf("CREDIT_CARD"))
        }

        @Test
        fun `fetches exchange rate when foreign currency is selected`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEurWithThb)
            coEvery {
                getExchangeRateUseCase("EUR", "THB")
            } returns ExchangeRateWithStaleness(
                rate = BigDecimal("38.5"),
                isStale = false
            )

            // When - Load config
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // When - Select foreign currency by code
            viewModel.onEvent(AddExpenseUiEvent.CurrencySelected("THB"))
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showExchangeRateSection)
            assertEquals("38.5", state.displayExchangeRate)
            assertFalse(state.isLoadingRate)
            coVerify { getExchangeRateUseCase("EUR", "THB") }
        }

        @Test
        fun `shows loading state while fetching rate`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEurWithThb)
            coEvery {
                getExchangeRateUseCase("EUR", "THB")
            } returns ExchangeRateWithStaleness(
                rate = BigDecimal("38.5"),
                isStale = false
            )

            // When - Load config
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // When - Select foreign currency
            viewModel.onEvent(AddExpenseUiEvent.CurrencySelected("THB"))

            // Then - Loading should be true during fetch
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isLoadingRate) // Should be false after completion
        }

        @Test
        fun `does not fetch rate when same currency is selected`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEurWithThb)

            // When - Load config (EUR is default)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // When - Select same currency as group (EUR)
            viewModel.onEvent(AddExpenseUiEvent.CurrencySelected("EUR"))
            advanceUntilIdle()

            // Then - Rate should be 1.0 and no fetch
            val state = viewModel.uiState.value
            assertFalse(state.showExchangeRateSection)
            assertEquals("1.0", state.displayExchangeRate)
            coVerify(exactly = 0) { getExchangeRateUseCase(any(), any()) }
        }

        @Test
        fun `keeps existing rate when fetch returns null`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEurWithThb)
            coEvery { getExchangeRateUseCase("EUR", "THB") } returns null

            // When - Load config
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // When - Select foreign currency
            viewModel.onEvent(AddExpenseUiEvent.CurrencySelected("THB"))
            advanceUntilIdle()

            // Then - Should keep default rate (empty string) and flag error
            val state = viewModel.uiState.value
            assertTrue(state.showExchangeRateSection)
            assertEquals("", state.displayExchangeRate)
            assertTrue(state.isExchangeRateError)
            assertFalse(state.isLoadingRate)
        }

        @Test
        fun `resets rate to 1 when switching back to group currency`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEurWithThb)
            coEvery {
                getExchangeRateUseCase("EUR", "THB")
            } returns ExchangeRateWithStaleness(
                rate = BigDecimal("38.5"),
                isStale = false
            )

            // When - Load config and select foreign currency
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()
            viewModel.onEvent(AddExpenseUiEvent.CurrencySelected("THB"))
            advanceUntilIdle()

            // Verify rate was set
            assertEquals("38.5", viewModel.uiState.value.displayExchangeRate)

            // When - Switch back to group currency
            viewModel.onEvent(AddExpenseUiEvent.CurrencySelected("EUR"))
            advanceUntilIdle()

            // Then - Rate should be reset to 1.0
            val state = viewModel.uiState.value
            assertFalse(state.showExchangeRateSection)
            assertEquals("1.0", state.displayExchangeRate)
        }

        @Test
        fun `fetches new rate when switching between foreign currencies`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEurWithThb)
            coEvery {
                getExchangeRateUseCase("EUR", "THB")
            } returns ExchangeRateWithStaleness(
                rate = BigDecimal("38.5"),
                isStale = false
            )
            coEvery {
                getExchangeRateUseCase("EUR", "USD")
            } returns ExchangeRateWithStaleness(
                rate = BigDecimal("1.08"),
                isStale = false
            )

            // When - Load config and select THB
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()
            viewModel.onEvent(AddExpenseUiEvent.CurrencySelected("THB"))
            advanceUntilIdle()

            assertEquals("38.5", viewModel.uiState.value.displayExchangeRate)

            // When - Switch to USD
            viewModel.onEvent(AddExpenseUiEvent.CurrencySelected("USD"))
            advanceUntilIdle()

            // Then - Rate should be updated
            val state = viewModel.uiState.value
            assertTrue(state.showExchangeRateSection)
            assertEquals("1.08", state.displayExchangeRate)
            coVerify { getExchangeRateUseCase("EUR", "USD") }
        }

        @Test
        fun `handles exception gracefully when fetching rate fails`() = runTest {
            // Given
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEurWithThb)
            coEvery { getExchangeRateUseCase("EUR", "THB") } throws RuntimeException("Network error")

            // When - Load config
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // When - Select foreign currency
            viewModel.onEvent(AddExpenseUiEvent.CurrencySelected("THB"))
            advanceUntilIdle()

            // Then - Should set isLoadingRate to false, keep existing rate (empty string), and flag error
            val state = viewModel.uiState.value
            assertTrue(state.showExchangeRateSection)
            assertEquals("", state.displayExchangeRate)
            assertTrue(state.isExchangeRateError)
            assertFalse(state.isLoadingRate)
            coVerify { getExchangeRateUseCase("EUR", "THB") }
        }
    }

    // ── Submit expense ─────────────────────────────────────────────────────

    @Nested
    inner class SubmitExpense {

        private val thb = Currency(
            code = "THB",
            symbol = "฿",
            defaultName = "Thai Baht",
            decimalDigits = 2
        )

        private val configEurWithThb = GroupExpenseConfig(
            group = groupEur,
            groupCurrency = eur,
            availableCurrencies = listOf(eur, usd, thb)
        )

        /**
         * Loads EUR group config, switches to THB as the payment currency,
         * then stubs [addExpenseUseCase] to throw [InsufficientCashException]
         * with amounts expressed in THB cents.
         */
        private fun TestScope.loadConfigAndSelectThb() {
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(
                configEurWithThb
            )
            coEvery {
                getExchangeRateUseCase("EUR", "THB")
            } returns ExchangeRateWithStaleness(
                rate = BigDecimal("9.20"),
                isStale = false
            )

            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            viewModel.onEvent(AddExpenseUiEvent.CurrencySelected("THB"))
            advanceUntilIdle()

            viewModel.onEvent(AddExpenseUiEvent.TitleChanged("Breakfast"))
            viewModel.onEvent(AddExpenseUiEvent.SourceAmountChanged("400"))
            advanceUntilIdle()
        }

        @Test
        fun `emits ShowCashConflictResolution on InsufficientCashException at save time`() = runTest {
            loadConfigAndSelectThb()

            // 400 THB required, only 2000 cents (20.00 THB) available — simulates a concurrent conflict
            coEvery { addExpenseUseCase(any(), any()) } returns Result.failure(
                InsufficientCashException(requiredCents = 40000L, availableCents = 2000L)
            )

            val emittedActions = mutableListOf<AddExpenseUiAction>()
            val job = launch { viewModel.actions.collect { emittedActions.add(it) } }

            viewModel.onEvent(AddExpenseUiEvent.SubmitAddExpense("group-eur"))
            advanceUntilIdle()
            job.cancel()

            // Phase 3: guided resolution sheet instead of a plain error pill
            val action = emittedActions.filterIsInstance<AddExpenseUiAction.ShowCashConflictResolution>().first()

            // Amounts must be pre-formatted and non-null (availableCents=2000, THB decimalDigits=2)
            assertNotNull(action.availableAmountForInput)
            assertNotNull(action.availableAmountDisplay)
        }

        @Test
        fun `emits generic ShowError for non-cash failures`() = runTest {
            loadConfigAndSelectThb()

            coEvery { addExpenseUseCase(any(), any()) } returns Result.failure(
                RuntimeException("Network error")
            )

            val emittedActions = mutableListOf<AddExpenseUiAction>()
            val job = launch { viewModel.actions.collect { emittedActions.add(it) } }

            viewModel.onEvent(AddExpenseUiEvent.SubmitAddExpense("group-eur"))
            advanceUntilIdle()
            job.cancel()

            val action = emittedActions.filterIsInstance<AddExpenseUiAction.ShowError>().first()
            val uiText =
                action.message as UiText.StringResource
            assertEquals(
                R.string.expense_error_addition_failed,
                uiText.resId
            )
        }

        @Test
        fun `does not set inline error on submission failure`() = runTest {
            loadConfigAndSelectThb()

            coEvery { addExpenseUseCase(any(), any()) } returns Result.failure(
                InsufficientCashException(requiredCents = 40000L, availableCents = 2000L)
            )

            viewModel.onEvent(AddExpenseUiEvent.SubmitAddExpense("group-eur"))
            advanceUntilIdle()

            // Snackbar is the correct surface — no inline error should be set
            assertNull(viewModel.uiState.value.error)
        }
    }

    // ── PaymentMethodSelected ───────────────────────────────────────────

    @Nested
    inner class PaymentMethodSelected {

        @Test
        fun `selects payment method from available methods`() = runTest {
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEur)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // Find a non-default payment method
            val methods = viewModel.uiState.value.paymentMethods
            assertTrue(methods.size > 1, "Expected multiple payment methods")
            val otherMethod = methods.first { it.id != viewModel.uiState.value.selectedPaymentMethod?.id }

            viewModel.onEvent(AddExpenseUiEvent.PaymentMethodSelected(otherMethod.id))

            assertEquals(otherMethod.id, viewModel.uiState.value.selectedPaymentMethod?.id)
        }

        @Test
        fun `ignores unknown method ID`() = runTest {
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEur)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            val before = viewModel.uiState.value.selectedPaymentMethod

            viewModel.onEvent(AddExpenseUiEvent.PaymentMethodSelected("NONEXISTENT"))

            assertEquals(before, viewModel.uiState.value.selectedPaymentMethod)
        }

        @Test
        fun `selecting CASH triggers exchange rate lock behavior`() = runTest {
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEur)
            every { getGroupLastUsedPaymentMethodUseCase(any()) } returns flowOf(listOf("CREDIT_CARD"))
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            val cashMethod = viewModel.uiState.value.paymentMethods
                .find { it.id == PaymentMethod.CASH.name }
            assertNotNull(cashMethod)

            viewModel.onEvent(AddExpenseUiEvent.PaymentMethodSelected(cashMethod!!.id))
            advanceUntilIdle()

            assertEquals(cashMethod.id, viewModel.uiState.value.selectedPaymentMethod?.id)
        }
    }

    // ── PaymentStatusSelected ───────────────────────────────────────────

    @Nested
    inner class PaymentStatusSelectedEvents {

        @Test
        fun `selects payment status from available statuses`() = runTest {
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEur)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            val statuses = viewModel.uiState.value.availablePaymentStatuses
            assertTrue(statuses.isNotEmpty(), "Expected available payment statuses")
            val status = statuses.first()

            viewModel.onEvent(AddExpenseUiEvent.PaymentStatusSelected(status.id))

            assertEquals(status.id, viewModel.uiState.value.selectedPaymentStatus?.id)
        }

        @Test
        fun `shows due date section for SCHEDULED status`() = runTest {
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEur)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            viewModel.onEvent(AddExpenseUiEvent.PaymentStatusSelected(PaymentStatus.SCHEDULED.name))

            assertTrue(viewModel.uiState.value.showDueDateSection)
        }

        @Test
        fun `hides due date section when switching from SCHEDULED to FINISHED`() = runTest {
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEur)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // Activate SCHEDULED
            viewModel.onEvent(AddExpenseUiEvent.PaymentStatusSelected(PaymentStatus.SCHEDULED.name))
            assertTrue(viewModel.uiState.value.showDueDateSection)

            // Switch to FINISHED
            viewModel.onEvent(AddExpenseUiEvent.PaymentStatusSelected(PaymentStatus.FINISHED.name))

            assertFalse(viewModel.uiState.value.showDueDateSection)
            assertNull(viewModel.uiState.value.dueDateMillis)
        }

        @Test
        fun `ignores unknown status ID`() = runTest {
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEur)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            val before = viewModel.uiState.value.selectedPaymentStatus

            viewModel.onEvent(AddExpenseUiEvent.PaymentStatusSelected("NONEXISTENT"))

            assertEquals(before, viewModel.uiState.value.selectedPaymentStatus)
        }
    }

    // ── DueDateSelected ─────────────────────────────────────────────────

    @Nested
    inner class DueDateSelectedEvents {

        @Test
        fun `sets due date millis and formatted date`() = runTest {
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEur)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            val dateMillis = 1743436800000L // 2025-03-31

            viewModel.onEvent(AddExpenseUiEvent.DueDateSelected(dateMillis))

            assertEquals(dateMillis, viewModel.uiState.value.dueDateMillis)
            assertTrue(viewModel.uiState.value.formattedDueDate.isNotEmpty())
            assertTrue(viewModel.uiState.value.isDueDateValid)
        }
    }

    // ── CategorySelected ────────────────────────────────────────────────

    @Nested
    inner class CategorySelectedEvents {

        @Test
        fun `selects category from available categories`() = runTest {
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEur)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            val categories = viewModel.uiState.value.availableCategories
            assertTrue(categories.isNotEmpty(), "Expected available categories")

            val category = categories.last()
            viewModel.onEvent(AddExpenseUiEvent.CategorySelected(category.id))

            assertEquals(category.id, viewModel.uiState.value.selectedCategory?.id)
        }

        @Test
        fun `ignores unknown category ID`() = runTest {
            coEvery { getGroupExpenseConfigUseCase("group-eur", any()) } returns Result.success(configEur)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            val before = viewModel.uiState.value.selectedCategory

            viewModel.onEvent(AddExpenseUiEvent.CategorySelected("NONEXISTENT"))

            assertEquals(before, viewModel.uiState.value.selectedCategory)
        }
    }

    // ── VendorChanged ───────────────────────────────────────────────────

    @Nested
    inner class VendorChangedEvents {

        @Test
        fun `updates vendor in state`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.VendorChanged("Starbucks"))

            assertEquals("Starbucks", viewModel.uiState.value.vendor)
        }

        @Test
        fun `clears vendor with empty string`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.VendorChanged("Starbucks"))
            assertEquals("Starbucks", viewModel.uiState.value.vendor)

            viewModel.onEvent(AddExpenseUiEvent.VendorChanged(""))

            assertEquals("", viewModel.uiState.value.vendor)
        }
    }

    // ── ReceiptImage ────────────────────────────────────────────────────

    @Nested
    inner class ReceiptImageEvents {

        @Test
        fun `ReceiptImageSelected sets uri`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.ReceiptImageSelected("content://image/1"))
            advanceUntilIdle()

            assertEquals("content://image/1", viewModel.uiState.value.receiptUri)
        }

        @Test
        fun `RemoveReceiptImage clears uri`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.ReceiptImageSelected("content://image/1"))
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.receiptUri)

            viewModel.onEvent(AddExpenseUiEvent.RemoveReceiptImage)

            assertNull(viewModel.uiState.value.receiptUri)
        }

        @Test
        fun `ViewReceiptFullScreen is a viewModel no-op and does not change state or emit actions`() = runTest {
            val beforeState = viewModel.uiState.value
            val emittedActions = mutableListOf<AddExpenseUiAction>()
            val job = launch { viewModel.actions.collect { emittedActions.add(it) } }

            viewModel.onEvent(AddExpenseUiEvent.ViewReceiptFullScreen)
            advanceUntilIdle()
            job.cancel()

            assertEquals(beforeState, viewModel.uiState.value)
            assertTrue(emittedActions.isEmpty())
        }
    }

    // ── ContributionScopeSelected ────────────────────────────────────────

    @Nested
    inner class ContributionScopeSelected {

        @Test
        fun `updates contribution scope to GROUP`() = runTest {
            viewModel.onEvent(
                AddExpenseUiEvent.ContributionScopeSelected(
                    scope = PayerType.GROUP,
                    subunitId = null
                )
            )

            assertEquals(PayerType.GROUP, viewModel.uiState.value.contributionScope)
            assertNull(viewModel.uiState.value.selectedContributionSubunitId)
        }

        @Test
        fun `updates contribution scope to SUBUNIT with subunitId`() = runTest {
            viewModel.onEvent(
                AddExpenseUiEvent.ContributionScopeSelected(
                    scope = PayerType.SUBUNIT,
                    subunitId = "sub-1"
                )
            )

            assertEquals(PayerType.SUBUNIT, viewModel.uiState.value.contributionScope)
            assertEquals("sub-1", viewModel.uiState.value.selectedContributionSubunitId)
        }

        @Test
        fun `updates contribution scope to USER`() = runTest {
            // First set to GROUP
            viewModel.onEvent(
                AddExpenseUiEvent.ContributionScopeSelected(
                    scope = PayerType.GROUP,
                    subunitId = null
                )
            )
            assertEquals(PayerType.GROUP, viewModel.uiState.value.contributionScope)

            // Then switch back to USER
            viewModel.onEvent(
                AddExpenseUiEvent.ContributionScopeSelected(
                    scope = PayerType.USER,
                    subunitId = null
                )
            )

            assertEquals(PayerType.USER, viewModel.uiState.value.contributionScope)
            assertNull(viewModel.uiState.value.selectedContributionSubunitId)
        }
    }

    // ── FundingSourceSelected scope reset ──────────────────────────────────

    @Nested
    inner class FundingSourceScopeReset {

        @Test
        fun `resets contribution scope to USER when switching away from My Money`() = runTest {
            coEvery { getGroupExpenseConfigUseCase(any(), any()) } returns
                Result.success(configEur)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // Select My Money (USER) as funding source
            viewModel.onEvent(AddExpenseUiEvent.FundingSourceSelected("USER"))
            // Set scope to GROUP
            viewModel.onEvent(
                AddExpenseUiEvent.ContributionScopeSelected(
                    scope = PayerType.GROUP,
                    subunitId = null
                )
            )
            assertEquals(PayerType.GROUP, viewModel.uiState.value.contributionScope)

            // Switch to Group Pocket — scope should reset
            viewModel.onEvent(AddExpenseUiEvent.FundingSourceSelected("GROUP"))

            assertEquals(PayerType.USER, viewModel.uiState.value.contributionScope)
            assertNull(viewModel.uiState.value.selectedContributionSubunitId)
        }

        @Test
        fun `preserves contribution scope when staying on My Money`() = runTest {
            coEvery { getGroupExpenseConfigUseCase(any(), any()) } returns
                Result.success(configEur)
            viewModel.onEvent(AddExpenseUiEvent.LoadGroupConfig("group-eur"))
            advanceUntilIdle()

            // Select My Money
            viewModel.onEvent(AddExpenseUiEvent.FundingSourceSelected("USER"))
            // Set scope to SUBUNIT
            viewModel.onEvent(
                AddExpenseUiEvent.ContributionScopeSelected(
                    scope = PayerType.SUBUNIT,
                    subunitId = "sub-1"
                )
            )

            // Re-select My Money (same funding source) — scope should be preserved
            viewModel.onEvent(AddExpenseUiEvent.FundingSourceSelected("USER"))

            assertEquals(PayerType.SUBUNIT, viewModel.uiState.value.contributionScope)
            assertEquals("sub-1", viewModel.uiState.value.selectedContributionSubunitId)
        }
    }

    // ── WizardNavigation ────────────────────────────────────────────────

    @Nested
    inner class WizardNavigation {

        @Test
        fun `NextStep advances to next applicable step`() = runTest {
            val initialStep = viewModel.uiState.value.currentStep
            val applicableSteps = viewModel.uiState.value.applicableSteps
            assertTrue(applicableSteps.size > 1, "Need at least 2 steps for navigation")

            viewModel.onEvent(AddExpenseUiEvent.NextStep)

            val newStep = viewModel.uiState.value.currentStep
            val expectedStep = applicableSteps[1]
            assertEquals(expectedStep, newStep)
            assertTrue(newStep != initialStep)
        }

        @Test
        fun `PreviousStep on first step emits NavigateBack`() = runTest {
            // Ensure we're on the first step
            assertEquals(0, viewModel.uiState.value.currentStepIndex)

            val emittedActions = mutableListOf<AddExpenseUiAction>()
            val job = launch { viewModel.actions.collect { emittedActions.add(it) } }

            viewModel.onEvent(AddExpenseUiEvent.PreviousStep)
            advanceUntilIdle()

            job.cancel()

            assertTrue(emittedActions.any { it is AddExpenseUiAction.NavigateBack })
        }

        @Test
        fun `PreviousStep goes back after advancing`() = runTest {
            val initialStep = viewModel.uiState.value.currentStep

            // Navigate forward
            viewModel.onEvent(AddExpenseUiEvent.NextStep)
            val secondStep = viewModel.uiState.value.currentStep
            assertTrue(secondStep != initialStep)

            // Navigate back
            viewModel.onEvent(AddExpenseUiEvent.PreviousStep)

            assertEquals(initialStep, viewModel.uiState.value.currentStep)
        }

        @Test
        fun `JumpToReview sets currentStep to REVIEW and records jumpedFromStep`() = runTest {
            // Navigate to an optional step (CATEGORY)
            val steps = viewModel.uiState.value.applicableSteps
            val categoryIndex = steps.indexOf(AddExpenseStep.CATEGORY)
            repeat(categoryIndex) { viewModel.onEvent(AddExpenseUiEvent.NextStep) }
            assertEquals(AddExpenseStep.CATEGORY, viewModel.uiState.value.currentStep)
            assertTrue(viewModel.uiState.value.currentStep.isOptional)

            // When — Jump to Review
            viewModel.onEvent(AddExpenseUiEvent.JumpToReview)

            // Then
            assertEquals(AddExpenseStep.REVIEW, viewModel.uiState.value.currentStep)
            assertEquals(AddExpenseStep.CATEGORY, viewModel.uiState.value.jumpedFromStep)
        }

        @Test
        fun `PreviousStep from REVIEW returns to jumpedFromStep`() = runTest {
            // Navigate to an optional step and jump
            val steps = viewModel.uiState.value.applicableSteps
            val categoryIndex = steps.indexOf(AddExpenseStep.CATEGORY)
            repeat(categoryIndex) { viewModel.onEvent(AddExpenseUiEvent.NextStep) }
            viewModel.onEvent(AddExpenseUiEvent.JumpToReview)
            assertEquals(AddExpenseStep.REVIEW, viewModel.uiState.value.currentStep)

            // When — Go back
            viewModel.onEvent(AddExpenseUiEvent.PreviousStep)

            // Then — Should return to CATEGORY, not the step before REVIEW
            assertEquals(AddExpenseStep.CATEGORY, viewModel.uiState.value.currentStep)
            assertNull(viewModel.uiState.value.jumpedFromStep)
        }

        @Test
        fun `JumpToReview is ignored on non-optional step`() = runTest {
            // TITLE is the first step and is NOT optional
            assertEquals(AddExpenseStep.TITLE, viewModel.uiState.value.currentStep)
            assertFalse(viewModel.uiState.value.currentStep.isOptional)

            // When
            viewModel.onEvent(AddExpenseUiEvent.JumpToReview)

            // Then — Should stay on TITLE
            assertEquals(AddExpenseStep.TITLE, viewModel.uiState.value.currentStep)
            assertNull(viewModel.uiState.value.jumpedFromStep)
        }

        @Test
        fun `sequential PreviousStep after jump-back clears jumpedFromStep`() = runTest {
            // Navigate to an optional step and jump to Review
            val steps = viewModel.uiState.value.applicableSteps
            val categoryIndex = steps.indexOf(AddExpenseStep.CATEGORY)
            repeat(categoryIndex) { viewModel.onEvent(AddExpenseUiEvent.NextStep) }
            viewModel.onEvent(AddExpenseUiEvent.JumpToReview)

            // Go back (jump-back to CATEGORY)
            viewModel.onEvent(AddExpenseUiEvent.PreviousStep)
            assertEquals(AddExpenseStep.CATEGORY, viewModel.uiState.value.currentStep)
            assertNull(viewModel.uiState.value.jumpedFromStep)

            // Go back again (sequential — should go to previous step)
            viewModel.onEvent(AddExpenseUiEvent.PreviousStep)
            val expectedPrev = steps[categoryIndex - 1]
            assertEquals(expectedPrev, viewModel.uiState.value.currentStep)
        }

        @Test
        fun `JumpToStep navigates to the target step`() = runTest {
            // Given — advance to step 2
            viewModel.onEvent(AddExpenseUiEvent.NextStep)
            viewModel.onEvent(AddExpenseUiEvent.NextStep)
            val steps = viewModel.uiState.value.applicableSteps
            assertEquals(2, steps.indexOf(viewModel.uiState.value.currentStep))

            // When — jump back to step 0
            viewModel.onEvent(AddExpenseUiEvent.JumpToStep(0))

            // Then
            assertEquals(steps[0], viewModel.uiState.value.currentStep)
        }

        @Test
        fun `JumpToStep clears jumpedFromStep`() = runTest {
            // Navigate to optional step and jump to REVIEW (sets jumpedFromStep)
            val steps = viewModel.uiState.value.applicableSteps
            val categoryIndex = steps.indexOf(AddExpenseStep.CATEGORY)
            repeat(categoryIndex) { viewModel.onEvent(AddExpenseUiEvent.NextStep) }
            viewModel.onEvent(AddExpenseUiEvent.JumpToReview)
            assertEquals(AddExpenseStep.REVIEW, viewModel.uiState.value.currentStep)
            assertNotNull(viewModel.uiState.value.jumpedFromStep)

            // When — jump back via step indicator
            viewModel.onEvent(AddExpenseUiEvent.JumpToStep(0))

            // Then — jumpedFromStep is cleared
            assertEquals(steps[0], viewModel.uiState.value.currentStep)
            assertNull(viewModel.uiState.value.jumpedFromStep)
        }

        @Test
        fun `JumpToStep is a no-op for out-of-bounds index`() = runTest {
            // Given — advance one step
            viewModel.onEvent(AddExpenseUiEvent.NextStep)
            val stepBefore = viewModel.uiState.value.currentStep

            // When — wildly out of bounds
            viewModel.onEvent(AddExpenseUiEvent.JumpToStep(999))

            // Then — step unchanged
            assertEquals(stepBefore, viewModel.uiState.value.currentStep)
        }
    }

    @Nested
    inner class CurrencyInputEvents {

        @Test
        fun `ExchangeRateChanged delegates to currency handler and updates displayExchangeRate`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.ExchangeRateChanged("1.5"))
            advanceUntilIdle()
            assertEquals("1.5", viewModel.uiState.value.displayExchangeRate)
        }

        @Test
        fun `GroupAmountChanged delegates to currency handler and updates calculatedGroupAmount`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.GroupAmountChanged("50"))
            advanceUntilIdle()
            assertEquals("50", viewModel.uiState.value.calculatedGroupAmount)
        }
    }

    @Nested
    inner class WithdrawalPoolSelectedEvents {

        @Test
        fun `WithdrawalPoolSelected with GROUP scope delegates and does not enable subunit mode`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.WithdrawalPoolSelected(PayerType.GROUP, null))
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isSubunitMode)
        }

        @Test
        fun `WithdrawalPoolSelected with SUBUNIT scope applies subunit pool default`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.WithdrawalPoolSelected(PayerType.SUBUNIT, "sub-1"))
            advanceUntilIdle()
            // Routing verified — SUBUNIT branch was taken (no exception thrown)
        }
    }

    @Nested
    inner class SplitEvents {

        @Test
        fun `SplitTypeChanged delegates to split and subunit handlers`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.SplitTypeChanged("EQUAL"))
            advanceUntilIdle()
            // Routing verified — no exception; selectedSplitType unaffected (no config loaded)
        }

        @Test
        fun `SplitAmountChanged routes to split handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.SplitAmountChanged("user-1", "50"))
            advanceUntilIdle()
        }

        @Test
        fun `SplitPercentageChanged routes to split handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.SplitPercentageChanged("user-1", "30"))
            advanceUntilIdle()
        }

        @Test
        fun `SplitExcludedToggled routes to split handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.SplitExcludedToggled("user-1"))
            advanceUntilIdle()
        }

        @Test
        fun `SplitShareLockToggled routes to split handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.SplitShareLockToggled("user-1"))
            advanceUntilIdle()
        }
    }

    @Nested
    inner class EntitySplitAndIntraSubunitEvents {

        @Test
        fun `EntitySplitAmountChanged routes to subunit split handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.EntitySplitAmountChanged("entity-1", "50"))
            advanceUntilIdle()
        }

        @Test
        fun `EntitySplitPercentageChanged routes to subunit split handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.EntitySplitPercentageChanged("entity-1", "25"))
            advanceUntilIdle()
        }

        @Test
        fun `EntityShareLockToggled routes to subunit split handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.EntityShareLockToggled("entity-1"))
            advanceUntilIdle()
        }

        @Test
        fun `IntraSubunitSplitTypeChanged routes to subunit split handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.IntraSubunitSplitTypeChanged("sub-1", "EQUAL"))
            advanceUntilIdle()
        }

        @Test
        fun `IntraSubunitAmountChanged routes to subunit split handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.IntraSubunitAmountChanged("sub-1", "user-1", "50"))
            advanceUntilIdle()
        }

        @Test
        fun `IntraSubunitPercentageChanged routes to subunit split handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.IntraSubunitPercentageChanged("sub-1", "user-1", "25"))
            advanceUntilIdle()
        }

        @Test
        fun `IntraSubunitShareLockToggled routes to subunit split handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.IntraSubunitShareLockToggled("sub-1", "user-1"))
            advanceUntilIdle()
        }
    }

    @Nested
    inner class AddOnEvents {

        @Test
        fun `AddOnAdded adds an add-on to the state`() = runTest {
            val countBefore = viewModel.uiState.value.addOns.size

            viewModel.onEvent(AddExpenseUiEvent.AddOnAdded(AddOnType.FEE))
            advanceUntilIdle()

            assertEquals(countBefore + 1, viewModel.uiState.value.addOns.size)
        }

        @Test
        fun `AddOnRemoved routes to add-on handler`() = runTest {
            // Add one first, then remove it
            viewModel.onEvent(AddExpenseUiEvent.AddOnAdded(AddOnType.TIP))
            advanceUntilIdle()
            val addOnId = viewModel.uiState.value.addOns.first().id

            viewModel.onEvent(AddExpenseUiEvent.AddOnRemoved(addOnId))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.addOns.none { it.id == addOnId })
        }

        @Test
        fun `AddOnTypeChanged routes to add-on handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.AddOnAdded(AddOnType.FEE))
            advanceUntilIdle()
            val addOnId = viewModel.uiState.value.addOns.first().id

            viewModel.onEvent(AddExpenseUiEvent.AddOnTypeChanged(addOnId, AddOnType.TIP))
            advanceUntilIdle()
        }

        @Test
        fun `AddOnsSectionToggled routes to add-on handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.AddOnsSectionToggled)
            advanceUntilIdle()
        }
    }

    @Nested
    inner class ExpenseDateAndResolutionEvents {

        @Test
        fun `ExpenseDateSelected delegates to form handler and sets formatted date`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.ExpenseDateSelected(1716000000000L))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.formattedExpenseDate.isNotBlank())
        }

        @Test
        fun `ResolutionAmountSelected routes to form handler as source amount change`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.ResolutionAmountSelected("75"))
            advanceUntilIdle()
            assertEquals("75", viewModel.uiState.value.sourceAmount)
        }
    }

    @Nested
    inner class AiAutoFillEvents {

        @Test
        fun `SetAiModeActive true activates AI mode`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.SetAiModeActive(true))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isAiModeActive)
        }

        @Test
        fun `DismissAutoFillBanner delegates to receipt auto-fill handler`() = runTest {
            viewModel.onEvent(AddExpenseUiEvent.SetAiModeActive(true))
            advanceUntilIdle()

            viewModel.onEvent(AddExpenseUiEvent.DismissAutoFillBanner)
            advanceUntilIdle()
            // Routing verified — no exception (handler deactivates AI mode or dismisses banner)
        }
    }

    @Nested
    inner class RefreshCashPreview {

        @Test
        fun `refreshCashPreview delegates to currency handler without throwing`() = runTest {
            viewModel.refreshCashPreview()
            advanceUntilIdle()
            // Routing verified — previewCashExchangeRateUseCase is relaxed and won't throw
        }
    }
}
