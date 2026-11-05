package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.Expense

/**
 * Service for filtering and searching expenses by matching queries against
 * expense text properties (title, vendor, notes) or financial amounts (source or group amount).
 */
interface ExpenseSearchService {

    /**
     * Filters the given list of [Expense] items matching the provided search [query].
     *
     * If [query] is blank (or consists only of punctuation/whitespace), returns the original [expenses] list unchanged.
     * Otherwise, matches an expense if:
     * 1. The query matches against the expense title, vendor, or notes (accent-insensitive,
     *    punctuation-tolerant, and whitespace-normalized), OR
     * 2. The query represents a numeric amount (regardless of decimal separator `.` or `,`, omitted `.00`,
     *    or trailing zeros) that numerically matches either [Expense.sourceAmount] or [Expense.groupAmount],
     *    taking each currency's fraction digits into account.
     *
     * @param expenses The list of domain expenses to search within.
     * @param query The search query string.
     * @return Filtered list of expenses matching the query.
     */
    fun search(expenses: List<Expense>, query: String): List<Expense>
}
