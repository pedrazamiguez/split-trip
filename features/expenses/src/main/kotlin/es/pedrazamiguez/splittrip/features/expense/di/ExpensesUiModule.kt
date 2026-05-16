package es.pedrazamiguez.splittrip.features.expense.di

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.ExpenseValidationService
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.domain.service.split.SubunitAwareSplitService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.AddExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.DeleteExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetAvailableWithdrawalPoolsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpenseConfigUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.PreviewCashExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCategoryUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetGroupLastUsedPaymentMethodUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedCategoryUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedPaymentMethodUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.navigation.impl.ExpensesNavigationProviderImpl
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseAddOnUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseSplitUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpenseDetailUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl.AddExpenseScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl.ExpenseDetailScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl.ExpensesScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.AddExpenseViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.ExpenseDetailViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.ExpensesUseCases
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.ExpensesViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.AddOnCrudDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.AddOnEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.AddOnExchangeRateDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.CashRateDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.ConfigEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.CurrencyEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.EntitySplitFlattenDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.FormEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.IntraSubunitSplitDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SaveLastUsedPreferencesBundle
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SplitEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SplitRowMappingDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SubmitEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SubmitResultDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.SubunitSplitEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.WithdrawalPoolSelectionDelegate
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val expensesUiModule = module {

    single {
        val entitySplitFlattenDelegate = EntitySplitFlattenDelegate(
            splitPreviewService = get<SplitPreviewService>(),
            remainderDistributionService = get<RemainderDistributionService>()
        )

        AddExpenseSplitUiMapper(
            localeProvider = get<LocaleProvider>(),
            formattingHelper = get<FormattingHelper>(),
            splitPreviewService = get<SplitPreviewService>(),
            entitySplitFlattenDelegate = entitySplitFlattenDelegate
        )
    }

    single {
        AddExpenseOptionsUiMapper(
            resourceProvider = get<ResourceProvider>(),
            formattingHelper = get<FormattingHelper>()
        )
    }

    single { AddExpenseAddOnUiMapper() }

    single {
        AddExpenseUiMapper(
            localeProvider = get<LocaleProvider>(),
            resourceProvider = get<ResourceProvider>(),
            splitMapper = get<AddExpenseSplitUiMapper>(),
            addOnMapper = get<AddExpenseAddOnUiMapper>(),
            splitPreviewService = get<SplitPreviewService>()
        )
    }

    single {
        ExpenseUiMapper(
            localeProvider = get<LocaleProvider>(),
            resourceProvider = get<ResourceProvider>()
        )
    }

    viewModel {
        val expensesUseCases = ExpensesUseCases(
            getGroupExpensesFlowUseCase = get<GetGroupExpensesFlowUseCase>(),
            deleteExpenseUseCase = get<DeleteExpenseUseCase>(),
            getGroupByIdUseCase = get<GetGroupByIdUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            getGroupContributionsFlowUseCase = get<GetGroupContributionsFlowUseCase>(),
            getGroupSubunitsFlowUseCase = get<GetGroupSubunitsFlowUseCase>()
        )

        ExpensesViewModel(
            useCases = expensesUseCases,
            expenseUiMapper = get<ExpenseUiMapper>(),
            authenticationService = get<AuthenticationService>()
        )
    }

    // ── AddExpense ViewModel with co-created handlers ───────────────────
    // Handlers are created inside the viewModel block so the SAME instances
    // are shared between the ViewModel and cross-handler references
    // (e.g., ConfigEventHandler calls CurrencyEventHandler.fetchRate()).

    viewModel {
        val addExpenseUiMapper = get<AddExpenseUiMapper>()
        val addExpenseOptionsUiMapper = get<AddExpenseOptionsUiMapper>()
        val addExpenseSplitUiMapper = get<AddExpenseSplitUiMapper>()
        val formattingHelper = get<FormattingHelper>()

        val splitRowMappingDelegate = SplitRowMappingDelegate(
            splitCalculatorFactory = get<ExpenseSplitCalculatorFactory>(),
            splitPreviewService = get<SplitPreviewService>(),
            formattingHelper = formattingHelper
        )

        val splitHandler = SplitEventHandler(
            splitCalculatorFactory = get<ExpenseSplitCalculatorFactory>(),
            splitPreviewService = get<SplitPreviewService>(),
            formattingHelper = formattingHelper,
            splitRowMappingDelegate = splitRowMappingDelegate
        )

        val intraSubunitSplitDelegate = IntraSubunitSplitDelegate(
            splitCalculatorFactory = get<ExpenseSplitCalculatorFactory>(),
            splitPreviewService = get<SplitPreviewService>(),
            subunitAwareSplitService = get<SubunitAwareSplitService>(),
            formattingHelper = formattingHelper
        )

        val subunitSplitHandler = SubunitSplitEventHandler(
            splitPreviewService = get<SplitPreviewService>(),
            addExpenseSplitMapper = addExpenseSplitUiMapper,
            intraSubunitSplitDelegate = intraSubunitSplitDelegate,
            splitRowMappingDelegate = splitRowMappingDelegate
        )

        val withdrawalPoolSelectionDelegate = WithdrawalPoolSelectionDelegate(
            getAvailableWithdrawalPoolsUseCase = get<GetAvailableWithdrawalPoolsUseCase>(),
            addExpenseOptionsMapper = addExpenseOptionsUiMapper
        )

        val cashRateDelegate = CashRateDelegate(
            previewCashExchangeRateUseCase = get<PreviewCashExchangeRateUseCase>(),
            expenseCalculatorService = get<ExpenseCalculatorService>(),
            splitPreviewService = get<SplitPreviewService>(),
            formattingHelper = formattingHelper,
            addExpenseOptionsMapper = addExpenseOptionsUiMapper
        )

        val currencyHandler = CurrencyEventHandler(
            getExchangeRateUseCase = get<GetExchangeRateUseCase>(),
            exchangeRateCalculationService = get<ExchangeRateCalculationService>(),
            formattingHelper = formattingHelper,
            addExpenseOptionsMapper = addExpenseOptionsUiMapper,
            withdrawalPoolSelectionDelegate = withdrawalPoolSelectionDelegate,
            cashRateDelegate = cashRateDelegate
        )

        val configHandler = ConfigEventHandler(
            getGroupExpenseConfigUseCase = get<GetGroupExpenseConfigUseCase>(),
            getGroupLastUsedCurrencyUseCase = get<GetGroupLastUsedCurrencyUseCase>(),
            getGroupLastUsedPaymentMethodUseCase = get<GetGroupLastUsedPaymentMethodUseCase>(),
            getGroupLastUsedCategoryUseCase = get<GetGroupLastUsedCategoryUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            authenticationService = get<AuthenticationService>(),
            addExpenseOptionsMapper = addExpenseOptionsUiMapper,
            addExpenseSplitMapper = addExpenseSplitUiMapper
        )

        val submitResultDelegate = SubmitResultDelegate(
            saveLastUsedPreferences = SaveLastUsedPreferencesBundle(
                setGroupLastUsedCurrencyUseCase = get<SetGroupLastUsedCurrencyUseCase>(),
                setGroupLastUsedPaymentMethodUseCase = get<SetGroupLastUsedPaymentMethodUseCase>(),
                setGroupLastUsedCategoryUseCase = get<SetGroupLastUsedCategoryUseCase>()
            ),
            formattingHelper = formattingHelper
        )

        val submitHandler = SubmitEventHandler(
            addExpenseUseCase = get<AddExpenseUseCase>(),
            expenseValidationService = get<ExpenseValidationService>(),
            addOnCalculationService = get<AddOnCalculationService>(),
            expenseCalculatorService = get<ExpenseCalculatorService>(),
            remainderDistributionService = get<RemainderDistributionService>(),
            addExpenseUiMapper = addExpenseUiMapper,
            submitResultDelegate = submitResultDelegate
        )

        val addOnExchangeRateDelegate = AddOnExchangeRateDelegate(
            exchangeRateCalculationService = get<ExchangeRateCalculationService>(),
            expenseCalculatorService = get<ExpenseCalculatorService>(),
            splitPreviewService = get<SplitPreviewService>(),
            formattingHelper = formattingHelper,
            getExchangeRateUseCase = get<GetExchangeRateUseCase>(),
            previewCashExchangeRateUseCase = get<PreviewCashExchangeRateUseCase>()
        )

        val addOnCrudDelegate = AddOnCrudDelegate(
            addExpenseOptionsMapper = addExpenseOptionsUiMapper,
            exchangeRateDelegate = addOnExchangeRateDelegate
        )

        val addOnHandler = AddOnEventHandler(
            addOnCalculationService = get<AddOnCalculationService>(),
            exchangeRateCalculationService = get<ExchangeRateCalculationService>(),
            expenseCalculatorService = get<ExpenseCalculatorService>(),
            splitPreviewService = get<SplitPreviewService>(),
            formattingHelper = formattingHelper,
            addExpenseOptionsMapper = addExpenseOptionsUiMapper,
            exchangeRateDelegate = addOnExchangeRateDelegate,
            addOnCrudDelegate = addOnCrudDelegate
        )

        val formHandler = FormEventHandler(
            addExpenseUiMapper = addExpenseUiMapper
        )

        AddExpenseViewModel(
            configEventHandler = configHandler,
            currencyEventHandler = currencyHandler,
            splitEventHandler = splitHandler,
            subunitSplitEventHandler = subunitSplitHandler,
            addOnEventHandler = addOnHandler,
            submitEventHandler = submitHandler,
            formEventHandler = formHandler
        )
    }

    factory { ExpensesNavigationProviderImpl() } bind NavigationProvider::class

    single { ExpensesScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { AddExpenseScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { ExpenseDetailScreenUiProviderImpl() } bind ScreenUiProvider::class

    single {
        ExpenseDetailUiMapper(
            formattingHelper = get<FormattingHelper>(),
            resourceProvider = get<ResourceProvider>(),
            expenseCalculatorService = get<ExpenseCalculatorService>(),
            addOnCalculationService = get<AddOnCalculationService>()
        )
    }

    viewModel {
        ExpenseDetailViewModel(
            getExpenseByIdUseCase = get<GetExpenseByIdUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            getCashWithdrawalsFlowUseCase = get<GetCashWithdrawalsFlowUseCase>(),
            getGroupSubunitsUseCase = get<GetGroupSubunitsUseCase>(),
            deleteExpenseUseCase = get<DeleteExpenseUseCase>(),
            authenticationService = get<AuthenticationService>(),
            expenseDetailUiMapper = get<ExpenseDetailUiMapper>()
        )
    }
}
