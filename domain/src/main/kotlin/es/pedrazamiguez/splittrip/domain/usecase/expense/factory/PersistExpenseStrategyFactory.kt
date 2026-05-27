package es.pedrazamiguez.splittrip.domain.usecase.expense.factory

import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.expense.strategy.AddExpensePersistStrategy
import es.pedrazamiguez.splittrip.domain.usecase.expense.strategy.PersistExpenseStrategy
import es.pedrazamiguez.splittrip.domain.usecase.expense.strategy.UpdateExpensePersistStrategy

class PersistExpenseStrategyFactory(
    private val expenseRepository: ExpenseRepository,
    private val cashWithdrawalRepository: CashWithdrawalRepository,
    private val expenseCalculatorService: ExpenseCalculatorService,
    private val exchangeRateCalculationService: ExchangeRateCalculationService,
    private val groupMembershipService: GroupMembershipService,
    private val contributionRepository: ContributionRepository,
    private val authenticationService: AuthenticationService,
    private val addOnCalculationService: AddOnCalculationService
) {
    fun create(isUpdate: Boolean): PersistExpenseStrategy {
        return if (isUpdate) {
            UpdateExpensePersistStrategy(
                expenseRepository,
                cashWithdrawalRepository,
                expenseCalculatorService,
                exchangeRateCalculationService,
                groupMembershipService,
                contributionRepository,
                authenticationService,
                addOnCalculationService
            )
        } else {
            AddExpensePersistStrategy(
                expenseRepository,
                cashWithdrawalRepository,
                expenseCalculatorService,
                exchangeRateCalculationService,
                groupMembershipService,
                contributionRepository,
                authenticationService,
                addOnCalculationService
            )
        }
    }
}
