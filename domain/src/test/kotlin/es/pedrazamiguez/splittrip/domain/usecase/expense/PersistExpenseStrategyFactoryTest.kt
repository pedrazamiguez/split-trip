package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.expense.factory.PersistExpenseStrategyFactory
import es.pedrazamiguez.splittrip.domain.usecase.expense.strategy.AddExpensePersistStrategy
import es.pedrazamiguez.splittrip.domain.usecase.expense.strategy.UpdateExpensePersistStrategy
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PersistExpenseStrategyFactoryTest {

    private lateinit var factory: PersistExpenseStrategyFactory

    @BeforeEach
    fun setUp() {
        factory = PersistExpenseStrategyFactory(
            expenseRepository = mockk<ExpenseRepository>(relaxed = true),
            cashWithdrawalRepository = mockk<CashWithdrawalRepository>(relaxed = true),
            expenseCalculatorService = mockk<ExpenseCalculatorService>(relaxed = true),
            exchangeRateCalculationService = mockk<ExchangeRateCalculationService>(relaxed = true),
            groupMembershipService = mockk<GroupMembershipService>(relaxed = true),
            contributionRepository = mockk<ContributionRepository>(relaxed = true),
            authenticationService = mockk<AuthenticationService>(relaxed = true),
            addOnCalculationService = mockk<AddOnCalculationService>(relaxed = true)
        )
    }

    @Test
    fun `create with isUpdate=true returns UpdateExpensePersistStrategy`() {
        val strategy = factory.create(isUpdate = true)

        assertTrue(
            strategy is UpdateExpensePersistStrategy,
            "Expected UpdateExpensePersistStrategy but got ${strategy::class.simpleName}"
        )
    }

    @Test
    fun `create with isUpdate=false returns AddExpensePersistStrategy`() {
        val strategy = factory.create(isUpdate = false)

        assertTrue(
            strategy is AddExpensePersistStrategy,
            "Expected AddExpensePersistStrategy but got ${strategy::class.simpleName}"
        )
    }

    @Test
    fun `create returns a new instance on each invocation`() {
        val firstUpdate = factory.create(isUpdate = true)
        val secondUpdate = factory.create(isUpdate = true)
        val firstAdd = factory.create(isUpdate = false)
        val secondAdd = factory.create(isUpdate = false)

        assertNotSame(firstUpdate, secondUpdate)
        assertNotSame(firstAdd, secondAdd)
    }
}
