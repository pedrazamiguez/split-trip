package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.service.ExpenseFilterService
import es.pedrazamiguez.splittrip.domain.service.ExpenseSearchService
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class ExpenseFilterServiceImpl(
    private val expenseSearchService: ExpenseSearchService,
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() }
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

        // 5. Payment Method Filter
        if (criteria.isPaymentMethodFiltered) {
            candidateExpenses = candidateExpenses.filter { expense ->
                expense.paymentMethod in criteria.selectedPaymentMethods
            }
        }

        // 6. Payment Status Filter
        if (criteria.isPaymentStatusFiltered) {
            candidateExpenses = candidateExpenses.filter { expense ->
                expense.paymentStatus in criteria.selectedPaymentStatuses
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
        return isPayer || isInvolvedInSplit
    }

    private fun matchesDateRange(
        expense: Expense,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Boolean {
        val expenseDate = expense.effectiveDate
            ?.atZone(ZoneOffset.UTC)
            ?.withZoneSameInstant(zoneIdProvider())
            ?.toLocalDate() ?: return false
        if (startDate != null && expenseDate.isBefore(startDate)) return false
        if (endDate != null && expenseDate.isAfter(endDate)) return false
        return true
    }
}
