package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.service.ExpenseFilterService
import es.pedrazamiguez.splittrip.domain.service.ExpenseSearchService
import java.time.LocalDate

class ExpenseFilterServiceImpl(
    private val expenseSearchService: ExpenseSearchService
) : ExpenseFilterService {

    override fun filter(expenses: List<Expense>, criteria: ExpenseFilterCriteria): List<Expense> {
        if (!criteria.isActive) return expenses

        var candidateExpenses = expenses

        // 1. Text Search Filter
        if (criteria.isSearchFiltered) {
            candidateExpenses = expenseSearchService.search(candidateExpenses, criteria.searchQuery)
        }

        // 2. Category & Subcategory Filter
        if (criteria.isCategoryFiltered) {
            candidateExpenses = candidateExpenses.filter { expense ->
                matchesCategory(expense, criteria)
            }
        }

        // 3. Member / Payer Filter
        if (criteria.isMemberFiltered) {
            candidateExpenses = candidateExpenses.filter { expense ->
                matchesMember(expense, criteria.selectedMemberIds)
            }
        }

        // 4. Date Range Filter
        if (criteria.isDateFiltered) {
            candidateExpenses = candidateExpenses.filter { expense ->
                matchesDateRange(expense, criteria.startDate, criteria.endDate)
            }
        }

        return candidateExpenses
    }

    private fun matchesCategory(expense: Expense, criteria: ExpenseFilterCriteria): Boolean {
        val hasCategories = criteria.selectedCategories.isNotEmpty()
        val hasSubcategories = criteria.selectedSubcategories.isNotEmpty()

        if (!hasCategories && !hasSubcategories) return true

        val expenseCategory = expense.category
        val expenseSubcategory = expense.subcategory

        // 1. Expense matches a selected parent category
        if (expenseCategory in criteria.selectedCategories) {
            val selectedSubcategoriesForThisCategory = criteria.selectedSubcategories
                .filter { it.parentCategory == expenseCategory }

            return if (selectedSubcategoriesForThisCategory.isEmpty()) {
                // No subcategory refinement -> all expenses in this category match
                true
            } else {
                // Refined by subcategories -> expense must match one of the selected subcategories
                expenseSubcategory in selectedSubcategoriesForThisCategory
            }
        }

        // 2. Expense matches an orphan selected subcategory (if parent category was not explicitly selected)
        return expenseSubcategory in criteria.selectedSubcategories
    }

    private fun matchesMember(expense: Expense, selectedMemberIds: Set<String>): Boolean {
        val isPayer = expense.payerId != null && expense.payerId in selectedMemberIds
        val isInvolvedInSplit = expense.splits.any { it.userId in selectedMemberIds && !it.isExcluded }
        val isSubExpensePayer = expense.subExpenses.any { it.payerId != null && it.payerId in selectedMemberIds }
        return isPayer || isInvolvedInSplit || isSubExpensePayer
    }

    private fun matchesDateRange(
        expense: Expense,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Boolean {
        val expenseDates = buildList {
            expense.effectiveDate?.toLocalDate()?.let { add(it) }
            expense.subExpenses.forEach { sub ->
                (sub.operationDate ?: sub.dueDate)?.toLocalDate()?.let { add(it) }
            }
        }
        if (expenseDates.isEmpty()) return false
        return expenseDates.any { date ->
            (startDate == null || !date.isBefore(startDate)) &&
                (endDate == null || !date.isAfter(endDate))
        }
    }
}
