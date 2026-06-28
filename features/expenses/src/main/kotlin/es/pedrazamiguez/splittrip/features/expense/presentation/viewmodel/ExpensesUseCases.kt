package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.DeleteExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.UpdateExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase

/**
 * Bundles all use cases required by [ExpensesViewModel] to reduce
 * constructor parameter count below the detekt `LongParameterList` threshold.
 */
data class ExpensesUseCases(
    val getGroupExpensesFlowUseCase: GetGroupExpensesFlowUseCase,
    val deleteExpenseUseCase: DeleteExpenseUseCase,
    val getGroupByIdUseCase: GetGroupByIdUseCase,
    val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    val getGroupContributionsFlowUseCase: GetGroupContributionsFlowUseCase,
    val getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase,
    val getExpenseByIdFlowUseCase: GetExpenseByIdFlowUseCase,
    val updateExpenseUseCase: UpdateExpenseUseCase
)
