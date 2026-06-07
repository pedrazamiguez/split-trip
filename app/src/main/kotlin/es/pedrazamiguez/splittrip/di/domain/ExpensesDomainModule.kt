package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.ExpenseValidationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.addon.AddOnResolverFactory
import es.pedrazamiguez.splittrip.domain.service.impl.AddOnCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExchangeRateCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.RemainderDistributionServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.domain.service.split.SubunitAwareSplitService
import es.pedrazamiguez.splittrip.domain.service.split.impl.SplitPreviewServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.impl.SubunitAwareSplitServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.AddExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.AttachReceiptUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.DeleteExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.DownloadReceiptUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.ExtractReceiptFieldsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetAvailableWithdrawalPoolsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpenseConfigUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.PreviewCashExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.UpdateExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.factory.PersistExpenseStrategyFactory
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.AddExpenseUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.AttachReceiptUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.DeleteExpenseUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.DownloadReceiptUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.ExtractReceiptFieldsUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.GetAvailableWithdrawalPoolsUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.GetExpenseByIdFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.GetExpenseByIdUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.GetGroupExpenseConfigUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.GetGroupExpensesFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.PreviewCashExchangeRateUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.UpdateExpenseUseCaseImpl
import org.koin.dsl.module

val expensesDomainModule = module {
    factory {
        PersistExpenseStrategyFactory(
            expenseRepository = get<ExpenseRepository>(),
            cashWithdrawalRepository = get<CashWithdrawalRepository>(),
            expenseCalculatorService = get<ExpenseCalculatorService>(),
            exchangeRateCalculationService = get<ExchangeRateCalculationService>(),
            groupMembershipService = get<GroupMembershipService>(),
            contributionRepository = get<ContributionRepository>(),
            authenticationService = get<AuthenticationService>(),
            addOnCalculationService = get<AddOnCalculationService>()
        )
    }
    factory<AddExpenseUseCase> {
        AddExpenseUseCaseImpl(strategyFactory = get<PersistExpenseStrategyFactory>())
    }
    factory<UpdateExpenseUseCase> {
        UpdateExpenseUseCaseImpl(strategyFactory = get<PersistExpenseStrategyFactory>())
    }
    factory<DeleteExpenseUseCase> {
        DeleteExpenseUseCaseImpl(
            expenseRepository = get<ExpenseRepository>(),
            cashWithdrawalRepository = get<CashWithdrawalRepository>(),
            groupMembershipService = get<GroupMembershipService>(),
            contributionRepository = get<ContributionRepository>()
        )
    }
    factory<GetGroupExpensesFlowUseCase> {
        GetGroupExpensesFlowUseCaseImpl(expenseRepository = get<ExpenseRepository>())
    }
    factory<GetExpenseByIdUseCase> { GetExpenseByIdUseCaseImpl(expenseRepository = get<ExpenseRepository>()) }
    factory<GetExpenseByIdFlowUseCase> { GetExpenseByIdFlowUseCaseImpl(expenseRepository = get<ExpenseRepository>()) }
    factory<GetAvailableWithdrawalPoolsUseCase> {
        GetAvailableWithdrawalPoolsUseCaseImpl(cashWithdrawalRepository = get<CashWithdrawalRepository>())
    }
    factory<GetGroupExpenseConfigUseCase> {
        GetGroupExpenseConfigUseCaseImpl(
            groupRepository = get<GroupRepository>(),
            currencyRepository = get<CurrencyRepository>(),
            subunitRepository = get<SubunitRepository>()
        )
    }
    factory { AddOnResolverFactory() }
    factory<ExpenseCalculatorService> { ExpenseCalculatorServiceImpl() }
    factory<AddOnCalculationService> { AddOnCalculationServiceImpl(addOnResolverFactory = get<AddOnResolverFactory>()) }
    factory<ExchangeRateCalculationService> { ExchangeRateCalculationServiceImpl() }
    factory<RemainderDistributionService> { RemainderDistributionServiceImpl() }
    factory<PreviewCashExchangeRateUseCase> {
        PreviewCashExchangeRateUseCaseImpl(
            cashWithdrawalRepository = get<CashWithdrawalRepository>(),
            expenseCalculatorService = get<ExpenseCalculatorService>(),
            exchangeRateCalculationService = get<ExchangeRateCalculationService>()
        )
    }
    factory<SplitPreviewService> { SplitPreviewServiceImpl() }
    factory { ExpenseSplitCalculatorFactory(expenseCalculatorService = get<ExpenseCalculatorService>()) }
    factory<ExpenseValidationService> {
        ExpenseValidationServiceImpl(splitCalculatorFactory = get<ExpenseSplitCalculatorFactory>())
    }
    factory<SubunitAwareSplitService> {
        SubunitAwareSplitServiceImpl(splitCalculatorFactory = get<ExpenseSplitCalculatorFactory>())
    }
    factory<AttachReceiptUseCase> {
        AttachReceiptUseCaseImpl(receiptStorageService = get<ReceiptStorageService>())
    }
    factory<DownloadReceiptUseCase> {
        DownloadReceiptUseCaseImpl(
            receiptStorageService = get<ReceiptStorageService>(),
            expenseRepository = get<ExpenseRepository>()
        )
    }
    factory<ExtractReceiptFieldsUseCase> {
        ExtractReceiptFieldsUseCaseImpl(
            ocrService = get<ReceiptOcrService>(),
            extractionService = get<ReceiptExtractionService>()
        )
    }
}
