package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.strategy

import es.pedrazamiguez.splittrip.domain.usecase.balance.GetContributionByExpenseIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.AddExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.UpdateExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.ConfigEventHandler

/**
 * Factory class that creates the appropriate ExpenseFlowStrategy.
 *
 * Directs Koin dependencies into the strategy implementations, returning
 * EditExpenseFlowStrategy when an expenseId is present, or AddExpenseFlowStrategy
 * when creating a new expense.
 */
class ExpenseFlowStrategyFactory(
    private val configEventHandler: ConfigEventHandler,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val getContributionByExpenseIdUseCase: GetContributionByExpenseIdUseCase,
    private val addExpenseUiMapper: AddExpenseUiMapper,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val getGroupSubunitsUseCase: GetGroupSubunitsUseCase
) {
    fun create(expenseId: String?): ExpenseFlowStrategy {
        return if (!expenseId.isNullOrBlank()) {
            EditExpenseFlowStrategy(
                expenseId = expenseId,
                configEventHandler = configEventHandler,
                getExpenseByIdUseCase = getExpenseByIdUseCase,
                getContributionByExpenseIdUseCase = getContributionByExpenseIdUseCase,
                updateExpenseUseCase = updateExpenseUseCase,
                addExpenseUiMapper = addExpenseUiMapper,
                getMemberProfilesUseCase = getMemberProfilesUseCase,
                getGroupSubunitsUseCase = getGroupSubunitsUseCase
            )
        } else {
            AddExpenseFlowStrategy(
                configEventHandler = configEventHandler,
                addExpenseUseCase = addExpenseUseCase
            )
        }
    }
}
