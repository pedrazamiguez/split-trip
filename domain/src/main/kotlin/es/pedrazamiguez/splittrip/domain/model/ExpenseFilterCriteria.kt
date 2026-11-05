package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import java.io.Serializable
import java.time.LocalDate

data class ExpenseFilterCriteria(
    val searchQuery: String = "",
    val selectedCategories: Set<ExpenseCategory> = emptySet(),
    val selectedSubcategories: Set<ExpenseSubcategory> = emptySet(),
    val selectedMemberIds: Set<String> = emptySet(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val selectedPaymentMethods: Set<PaymentMethod> = emptySet(),
    val selectedPaymentStatuses: Set<PaymentStatus> = emptySet()
) : Serializable {
    val isCategoryFiltered: Boolean
        get() = selectedCategories.isNotEmpty() || selectedSubcategories.isNotEmpty()

    val isMemberFiltered: Boolean
        get() = selectedMemberIds.isNotEmpty()

    val isDateFiltered: Boolean
        get() = startDate != null || endDate != null

    val isSearchFiltered: Boolean
        get() = searchQuery.isNotBlank()

    val isPaymentMethodFiltered: Boolean
        get() = selectedPaymentMethods.isNotEmpty()

    val isPaymentStatusFiltered: Boolean
        get() = selectedPaymentStatuses.isNotEmpty()

    /** Number of active filter dimensions excluding free-text search query. */
    val activeFilterCount: Int
        get() {
            var count = 0
            if (isCategoryFiltered) count++
            if (isMemberFiltered) count++
            if (isDateFiltered) count++
            if (isPaymentMethodFiltered) count++
            if (isPaymentStatusFiltered) count++
            return count
        }

    val isActive: Boolean
        get() = isSearchFiltered || activeFilterCount > 0

    fun clearNonSearchFilters(): ExpenseFilterCriteria = copy(
        selectedCategories = emptySet(),
        selectedSubcategories = emptySet(),
        selectedMemberIds = emptySet(),
        startDate = null,
        endDate = null,
        selectedPaymentMethods = emptySet(),
        selectedPaymentStatuses = emptySet()
    )

    fun clearAll(): ExpenseFilterCriteria = ExpenseFilterCriteria()

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
