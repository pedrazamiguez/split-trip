package es.pedrazamiguez.splittrip.domain.di

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
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.domain.service.split.SubunitAwareSplitService
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
import org.koin.dsl.module

val expensesDomainModule = module {
    factory<AddExpenseUseCase> {
        AddExpenseUseCase(
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
    factory<DeleteExpenseUseCase> {
        DeleteExpenseUseCase(
            expenseRepository = get<ExpenseRepository>(),
            cashWithdrawalRepository = get<CashWithdrawalRepository>(),
            groupMembershipService = get<GroupMembershipService>(),
            contributionRepository = get<ContributionRepository>()
        )
    }
    factory<GetGroupExpensesFlowUseCase> { GetGroupExpensesFlowUseCase(expenseRepository = get<ExpenseRepository>()) }
    factory<GetExpenseByIdUseCase> { GetExpenseByIdUseCase(expenseRepository = get<ExpenseRepository>()) }
    factory<GetExpenseByIdFlowUseCase> { GetExpenseByIdFlowUseCase(expenseRepository = get<ExpenseRepository>()) }
    factory<GetAvailableWithdrawalPoolsUseCase> {
        GetAvailableWithdrawalPoolsUseCase(cashWithdrawalRepository = get<CashWithdrawalRepository>())
    }
    factory<GetGroupExpenseConfigUseCase> {
        GetGroupExpenseConfigUseCase(
            groupRepository = get<GroupRepository>(),
            currencyRepository = get<CurrencyRepository>(),
            subunitRepository = get<SubunitRepository>()
        )
    }
    factory { AddOnResolverFactory() }
    factory { ExpenseCalculatorService() }
    factory { AddOnCalculationService(addOnResolverFactory = get<AddOnResolverFactory>()) }
    factory { ExchangeRateCalculationService() }
    factory { RemainderDistributionService() }
    factory {
        PreviewCashExchangeRateUseCase(
            cashWithdrawalRepository = get<CashWithdrawalRepository>(),
            expenseCalculatorService = get<ExpenseCalculatorService>(),
            exchangeRateCalculationService = get<ExchangeRateCalculationService>()
        )
    }
    factory { SplitPreviewService() }
    factory { ExpenseSplitCalculatorFactory(expenseCalculatorService = get<ExpenseCalculatorService>()) }
    factory { ExpenseValidationService(splitCalculatorFactory = get<ExpenseSplitCalculatorFactory>()) }
    factory { SubunitAwareSplitService(splitCalculatorFactory = get<ExpenseSplitCalculatorFactory>()) }
    factory<AttachReceiptUseCase> {
        AttachReceiptUseCase(receiptStorageService = get<ReceiptStorageService>())
    }
    factory {
        DownloadReceiptUseCase(
            receiptStorageService = get<ReceiptStorageService>(),
            expenseRepository = get<ExpenseRepository>()
        )
    }
    factory {
        ExtractReceiptFieldsUseCase(
            ocrService = get<ReceiptOcrService>(),
            extractionService = get<ReceiptExtractionService>()
        )
    }
}
