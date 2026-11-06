package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.constant.DomainConstants
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.service.ExpenseSearchService
import java.math.BigDecimal
import java.text.Normalizer
import java.util.Currency

class ExpenseSearchServiceImpl : ExpenseSearchService {

    override fun search(expenses: List<Expense>, query: String): List<Expense> {
        val normalizedQuery = query.normalizeForSearch()
        if (normalizedQuery.isEmpty()) return expenses

        val queryAmount = parseQueryAmount(query)

        return expenses.filter { expense ->
            matchesText(expense, normalizedQuery) || matchesAmount(expense, queryAmount)
        }
    }

    private fun parseQueryAmount(query: String): BigDecimal? {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return null
        val normalized = CurrencyConverter.normalizeAmountString(cleanQuery).trimEnd('.')
        return normalized.toBigDecimalOrNull()
    }

    private fun matchesText(expense: Expense, normalizedQuery: String): Boolean {
        return expense.title.normalizeForSearch().contains(normalizedQuery) ||
            (expense.vendor?.normalizeForSearch()?.contains(normalizedQuery) == true) ||
            (expense.notes?.normalizeForSearch()?.contains(normalizedQuery) == true)
    }

    private fun matchesAmount(expense: Expense, queryAmount: BigDecimal?): Boolean {
        if (queryAmount == null) return false

        val sourceAmountDecimal = expense.sourceAmount.toAmountDecimal(expense.sourceCurrency)
        val groupAmountDecimal = expense.groupAmount.toAmountDecimal(expense.groupCurrency)

        return queryAmount.compareTo(sourceAmountDecimal) == 0 ||
            queryAmount.compareTo(groupAmountDecimal) == 0
    }

    private fun Long.toAmountDecimal(currencyCode: String): BigDecimal {
        val fractionDigits = getFractionDigits(currencyCode)
        return BigDecimal.valueOf(this, fractionDigits)
    }

    private fun getFractionDigits(currencyCode: String): Int {
        return runCatching {
            val digits = Currency.getInstance(currencyCode).defaultFractionDigits
            if (digits >= 0) digits else DomainConstants.DEFAULT_DECIMAL_PLACES
        }.getOrDefault(DomainConstants.DEFAULT_DECIMAL_PLACES)
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
