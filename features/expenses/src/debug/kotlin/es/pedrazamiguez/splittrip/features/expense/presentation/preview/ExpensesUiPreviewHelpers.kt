package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.MappedPreview
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpensesFilterUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.PaymentStatusBadgeUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDateGroupUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesFilterUiState
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private fun buildExpenseUiMapper(localeProvider: LocaleProvider, resourceProvider: ResourceProvider): ExpenseUiMapper =
    ExpenseUiMapper(
        localeProvider = localeProvider,
        resourceProvider = resourceProvider,
        paymentStatusBadgeUiMapper = PaymentStatusBadgeUiMapper(
            formattingHelper = FormattingHelper(localeProvider),
            resourceProvider = resourceProvider
        ),
        userUiMapper = UserUiMapper(resourceProvider)
    )

private fun buildExpensesFilterUiMapper(
    localeProvider: LocaleProvider,
    resourceProvider: ResourceProvider
): ExpensesFilterUiMapper = ExpensesFilterUiMapper(
    formattingHelper = FormattingHelper(localeProvider),
    userUiMapper = UserUiMapper(resourceProvider)
)

@Composable
fun ExpenseItemPreviewHelper(
    domainExpense: Expense = PREVIEW_EXPENSE_BASIC,
    memberProfiles: Map<String, User> = emptyMap(),
    currentUserId: String? = null,
    pairedContributions: Map<String, Contribution> = emptyMap(),
    subunits: Map<String, Subunit> = emptyMap(),
    content: @Composable (ExpenseUiModel) -> Unit
) {
    MappedPreview(
        domain = domainExpense,
        mapper = { localeProvider, resourceProvider ->
            buildExpenseUiMapper(localeProvider, resourceProvider)
        },
        transform = { mapper, domain ->
            mapper.map(domain, memberProfiles, currentUserId, pairedContributions, subunits)
        },
        content = content
    )
}

@Composable
fun ExpenseListPreviewHelper(
    domainExpenses: List<Expense> = PREVIEW_EXPENSES,
    memberProfiles: Map<String, User> = emptyMap(),
    currentUserId: String? = null,
    pairedContributions: Map<String, Contribution> = emptyMap(),
    subunits: Map<String, Subunit> = emptyMap(),
    content: @Composable (ImmutableList<ExpenseDateGroupUiModel>) -> Unit
) {
    MappedPreview(
        domain = domainExpenses,
        mapper = { localeProvider, resourceProvider ->
            buildExpenseUiMapper(localeProvider, resourceProvider)
        },
        transform = { mapper, domain ->
            mapper.mapGroupedByDate(domain, memberProfiles, currentUserId, pairedContributions, subunits)
        },
        content = content
    )
}

@Composable
fun ExpensesFilterPreviewHelper(
    draftCriteria: ExpenseFilterCriteria = ExpenseFilterCriteria(),
    matchingExpensesCount: Int = 10,
    totalExpensesCount: Int = 10,
    allUserIds: List<String> = listOf("user-1", "user-2", "user-3"),
    memberProfiles: Map<String, User> = emptyMap(),
    currentUserId: String? = "user-1",
    oldestExpenseDate: LocalDate? = null,
    newestExpenseDate: LocalDate? = null,
    today: LocalDate = LocalDate.now(),
    groupCurrency: String = "EUR",
    groupCurrencies: ImmutableList<String> = persistentListOf(),
    content: @Composable (ExpensesFilterUiState) -> Unit
) {
    MappedPreview(
        domain = draftCriteria,
        mapper = { localeProvider, resourceProvider ->
            buildExpensesFilterUiMapper(localeProvider, resourceProvider)
        },
        transform = { mapper, domain ->
            val availableMembers = mapper.mapAvailableMembers(
                allUserIds = allUserIds,
                memberProfiles = memberProfiles,
                currentUserId = currentUserId
            )
            val formattedStartDate = mapper.formatFilterDate(domain.startDate)
            val formattedEndDate = mapper.formatFilterDate(domain.endDate)
            val activePreset = mapper.findMatchingPreset(domain.startDate, domain.endDate, today)

            ExpensesFilterUiState(
                draftCriteria = domain,
                availableMembers = availableMembers,
                matchingExpensesCount = matchingExpensesCount,
                totalExpensesCount = totalExpensesCount,
                isLoading = false,
                groupId = "group-1",
                oldestExpenseDate = oldestExpenseDate,
                newestExpenseDate = newestExpenseDate,
                formattedStartDate = formattedStartDate,
                formattedEndDate = formattedEndDate,
                activePreset = activePreset,
                groupCurrency = groupCurrency,
                groupCurrencies = groupCurrencies
            )
        },
        content = content
    )
}
