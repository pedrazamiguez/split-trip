package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.strategy

import es.pedrazamiguez.splittrip.domain.usecase.balance.GetContributionByExpenseIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.AddExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.UpdateExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.ConfigEventHandler
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExpenseFlowStrategyFactoryTest {

    private lateinit var factory: ExpenseFlowStrategyFactory

    @BeforeEach
    fun setUp() {
        factory = ExpenseFlowStrategyFactory(
            configEventHandler = mockk<ConfigEventHandler>(relaxed = true),
            addExpenseUseCase = mockk<AddExpenseUseCase>(relaxed = true),
            updateExpenseUseCase = mockk<UpdateExpenseUseCase>(relaxed = true),
            getExpenseByIdUseCase = mockk<GetExpenseByIdUseCase>(relaxed = true),
            getContributionByExpenseIdUseCase = mockk<GetContributionByExpenseIdUseCase>(relaxed = true),
            addExpenseUiMapper = mockk<AddExpenseUiMapper>(relaxed = true),
            getMemberProfilesUseCase = mockk<GetMemberProfilesUseCase>(relaxed = true),
            getGroupSubunitsUseCase = mockk<GetGroupSubunitsUseCase>(relaxed = true)
        )
    }

    @Test
    fun `create with non-blank expenseId returns EditExpenseFlowStrategy`() {
        val strategy = factory.create(expenseId = "expense-1")
        assertTrue(strategy is EditExpenseFlowStrategy)
        assertTrue(strategy.isEditMode)
    }

    @Test
    fun `create with null expenseId returns AddExpenseFlowStrategy`() {
        val strategy = factory.create(expenseId = null)
        assertTrue(strategy is AddExpenseFlowStrategy)
        assertTrue(!strategy.isEditMode)
    }

    @Test
    fun `create with empty expenseId returns AddExpenseFlowStrategy`() {
        val strategy = factory.create(expenseId = "")
        assertTrue(strategy is AddExpenseFlowStrategy)
    }

    @Test
    fun `create with blank expenseId returns AddExpenseFlowStrategy`() {
        val strategy = factory.create(expenseId = "   ")
        assertTrue(strategy is AddExpenseFlowStrategy)
    }
}
