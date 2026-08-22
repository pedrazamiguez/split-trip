package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.service.ExpenseSearchService
import java.text.Normalizer

class ExpenseSearchServiceImpl : ExpenseSearchService {

    override fun search(expenses: List<Expense>, query: String): List<Expense> {
        val normalizedQuery = query.normalizeForSearch()
        if (normalizedQuery.isEmpty()) return expenses

        return expenses.filter { expense ->
            expense.title.normalizeForSearch().contains(normalizedQuery) ||
                (expense.vendor?.normalizeForSearch()?.contains(normalizedQuery) == true) ||
                (expense.notes?.normalizeForSearch()?.contains(normalizedQuery) == true) ||
                expense.subExpenses.any { sub ->
                    sub.title.normalizeForSearch().contains(normalizedQuery) ||
                        (sub.notes?.normalizeForSearch()?.contains(normalizedQuery) == true)
                }
        }
    }

    private fun String.normalizeForSearch(): String {
        if (isEmpty()) return this
        val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
        val withoutDiacritics = DIACRITICS_REGEX.replace(decomposed, "")
        val withoutPunctuation = NON_ALPHANUMERIC_REGEX.replace(withoutDiacritics, " ")
        return MULTIPLE_WHITESPACE_REGEX.replace(withoutPunctuation, " ").trim().lowercase()
    }

    private companion object {
        private val DIACRITICS_REGEX = Regex("\\p{M}+")
        private val NON_ALPHANUMERIC_REGEX = Regex("[^\\p{L}\\p{N}\\s]+")
        private val MULTIPLE_WHITESPACE_REGEX = Regex("\\s+")
    }
}
