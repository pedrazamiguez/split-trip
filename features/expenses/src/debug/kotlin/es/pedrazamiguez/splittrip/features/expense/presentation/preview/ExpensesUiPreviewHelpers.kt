package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.preview.MappedPreview
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ScheduledBadgeUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDateGroupUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseUiModel
import kotlinx.collections.immutable.ImmutableList

private fun buildExpenseUiMapper(localeProvider: LocaleProvider, resourceProvider: ResourceProvider): ExpenseUiMapper =
    ExpenseUiMapper(
        localeProvider = localeProvider,
        resourceProvider = resourceProvider,
        scheduledBadgeUiMapper = ScheduledBadgeUiMapper(
            formattingHelper = FormattingHelper(localeProvider),
            resourceProvider = resourceProvider
        )
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
