package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion

/**
 * Deterministic, offline, zero-latency local rule/keyword classifier for expense titles.
 */
interface ExpenseCategoryClassifierService {
    fun classify(
        title: String,
        groupContext: GroupContext
    ): CategorySuggestion?
}
