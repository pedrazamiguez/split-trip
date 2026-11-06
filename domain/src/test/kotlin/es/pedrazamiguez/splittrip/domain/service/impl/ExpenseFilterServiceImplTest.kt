package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.service.ExpenseFilterService
import es.pedrazamiguez.splittrip.domain.service.ExpenseSearchService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpenseFilterServiceImplTest {

    private lateinit var searchService: ExpenseSearchService
    private lateinit var filterService: ExpenseFilterService

    private val baseDate = LocalDate.of(2024, 6, 15)

    private val expense1 = Expense(
        id = "exp-1",
        title = "Dinner at Italian Place",
        category = ExpenseCategory.FOOD,
        subcategory = ExpenseSubcategory.RESTAURANT,
        payerId = "user-1",
        splits = listOf(
            ExpenseSplit(userId = "user-1", amountCents = 2500L),
            ExpenseSplit(userId = "user-2", amountCents = 2500L)
        ),
        createdAt = baseDate.atTime(20, 0)
    )

    private val expense2 = Expense(
        id = "exp-2",
        title = "Supermarket Groceries",
        category = ExpenseCategory.FOOD,
        subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET,
        payerId = "user-2",
        splits = listOf(
            ExpenseSplit(userId = "user-1", amountCents = 1500L),
            ExpenseSplit(userId = "user-2", amountCents = 1500L)
        ),
        createdAt = baseDate.plusDays(2).atTime(11, 0)
    )

    private val expense3 = Expense(
        id = "exp-3",
        title = "Museum Tickets",
        category = ExpenseCategory.ACTIVITIES,
        subcategory = ExpenseSubcategory.MUSEUM_CULTURE,
        payerId = "user-3",
        splits = listOf(
            ExpenseSplit(userId = "user-3", amountCents = 3000L),
            ExpenseSplit(userId = "user-1", amountCents = 0L, isExcluded = true)
        ),
        createdAt = baseDate.plusDays(5).atTime(15, 0)
    )

    private val expense4 = Expense(
        id = "exp-4",
        title = "Flight to Rome",
        category = ExpenseCategory.TRANSPORT,
        subcategory = ExpenseSubcategory.DOMESTIC_FLIGHT,
        payerId = "user-1",
        splits = listOf(
            ExpenseSplit(userId = "user-1", amountCents = 10000L)
        ),
        createdAt = baseDate.minusDays(10).atTime(8, 0)
    )

    private val allExpenses = listOf(expense1, expense2, expense3, expense4)

    @BeforeEach
    fun setUp() {
        searchService = ExpenseSearchServiceImpl()
        filterService = ExpenseFilterServiceImpl(
            expenseSearchService = searchService,
            zoneIdProvider = { ZoneOffset.UTC }
        )
    }

    @Nested
    @DisplayName("Inactive / Empty Filter Criteria")
    inner class InactiveCriteria {

        @Test
        fun `empty criteria returns all expenses unmodified`() {
            val criteria = ExpenseFilterCriteria()

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(allExpenses, result)
            assertFalse(criteria.isActive)
            assertEquals(0, criteria.activeFilterCount)
        }

        @Test
        fun `criteria with empty collections and null dates returns all expenses`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "   ",
                selectedCategories = emptySet(),
                selectedSubcategories = emptySet(),
                selectedMemberIds = emptySet(),
                startDate = null,
                endDate = null
            )

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(allExpenses, result)
            assertFalse(criteria.isActive)
        }
    }

    @Nested
    @DisplayName("Search Query Filtering")
    inner class SearchQueryFiltering {

        @Test
        fun `filters expenses matching search query`() {
            val criteria = ExpenseFilterCriteria(searchQuery = "groceries")

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense2), result)
            assertTrue(criteria.isSearchFiltered)
            assertTrue(criteria.isActive)
            assertEquals(0, criteria.activeFilterCount)
        }
    }

    @Nested
    @DisplayName("Category & Subcategory Filtering")
    inner class CategoryFiltering {

        @Test
        fun `filters by parent category matching all subcategories when no subcategories are selected`() {
            val criteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD)
            )

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense1, expense2), result)
            assertTrue(criteria.isCategoryFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `refines parent category to only selected subcategories`() {
            val criteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedSubcategories = setOf(ExpenseSubcategory.RESTAURANT)
            )

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense1), result)
            assertTrue(criteria.isCategoryFiltered)
        }

        @Test
        fun `multiple categories with partial subcategory refinement (OR union)`() {
            val criteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD, ExpenseCategory.ACTIVITIES),
                selectedSubcategories = setOf(ExpenseSubcategory.RESTAURANT)
            )

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense1, expense3), result)
        }

        @Test
        fun `deselecting parent category clears matching for that category`() {
            val initialCriteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD, ExpenseCategory.ACTIVITIES),
                selectedSubcategories = setOf(ExpenseSubcategory.RESTAURANT)
            )
            val updatedCriteria = initialCriteria.copy(
                selectedCategories = initialCriteria.selectedCategories - ExpenseCategory.FOOD,
                selectedSubcategories = initialCriteria.selectedSubcategories.filterNot {
                    it.parentCategory == ExpenseCategory.FOOD
                }.toSet()
            )

            val result = filterService.filter(allExpenses, updatedCriteria)

            assertEquals(listOf(expense3), result)
        }

        @Test
        fun `orphan subcategory matches even if parent category is not in selectedCategories`() {
            val criteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.ACTIVITIES),
                selectedSubcategories = setOf(ExpenseSubcategory.DOMESTIC_FLIGHT)
            )

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense3, expense4), result)
        }

        @Test
        fun `expense with unspecified subcategory matches parent category when no subcategories selected`() {
            val unspecifiedSubcategoryExpense = expense1.copy(
                id = "exp-unspecified-sub",
                subcategory = ExpenseSubcategory.UNSPECIFIED
            )
            val criteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD)
            )

            val result = filterService.filter(listOf(unspecifiedSubcategoryExpense), criteria)

            assertEquals(listOf(unspecifiedSubcategoryExpense), result)
        }

        @Test
        fun `expense with unspecified subcategory does not match when specific subcategories are selected`() {
            val unspecifiedSubcategoryExpense = expense1.copy(
                id = "exp-unspecified-sub",
                subcategory = ExpenseSubcategory.UNSPECIFIED
            )
            val criteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedSubcategories = setOf(ExpenseSubcategory.RESTAURANT)
            )

            val result = filterService.filter(listOf(unspecifiedSubcategoryExpense), criteria)

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Member / Payer Filtering")
    inner class MemberFiltering {

        @Test
        fun `matches when member is payer`() {
            val criteria = ExpenseFilterCriteria(
                selectedMemberIds = setOf("user-3")
            )

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense3), result)
            assertTrue(criteria.isMemberFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `matches when member is participant in split`() {
            val criteria = ExpenseFilterCriteria(
                selectedMemberIds = setOf("user-2")
            )

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense1, expense2), result)
        }

        @Test
        fun `does not match when member is excluded from split and not payer`() {
            val criteria = ExpenseFilterCriteria(
                selectedMemberIds = setOf("user-1")
            )

            // user-1 is payer for exp-1, exp-4, and participant in exp-2, but excluded in exp-3
            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense1, expense2, expense4), result)
        }
    }

    @Nested
    @DisplayName("Date Range Filtering")
    inner class DateRangeFiltering {

        @Test
        fun `filters by start date only inclusive`() {
            val criteria = ExpenseFilterCriteria(
                startDate = baseDate
            )

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense1, expense2, expense3), result)
            assertTrue(criteria.isDateFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `filters by end date only inclusive`() {
            val criteria = ExpenseFilterCriteria(
                endDate = baseDate.plusDays(2)
            )

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense1, expense2, expense4), result)
        }

        @Test
        fun `filters by both start and end date range`() {
            val criteria = ExpenseFilterCriteria(
                startDate = baseDate,
                endDate = baseDate.plusDays(3)
            )

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense1, expense2), result)
        }

        @Test
        fun `excludes expense with null effectiveDate when date range active`() {
            val nullDateExpense = expense1.copy(id = "exp-null-date", operationDate = null, createdAt = null)
            val criteria = ExpenseFilterCriteria(startDate = baseDate)

            val result = filterService.filter(listOf(nullDateExpense), criteria)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `uses operationDate over createdAt when operationDate is present`() {
            val expenseWithBoth = expense1.copy(
                id = "exp-op-date",
                operationDate = baseDate.plusDays(10).atTime(12, 0),
                createdAt = baseDate.minusDays(5).atTime(12, 0)
            )
            val criteriaMatchingOpDate = ExpenseFilterCriteria(startDate = baseDate.plusDays(9))
            val criteriaMatchingCreatedAt =
                ExpenseFilterCriteria(startDate = baseDate.minusDays(6), endDate = baseDate.minusDays(1))

            val resultOp = filterService.filter(listOf(expenseWithBoth), criteriaMatchingOpDate)
            val resultCreated = filterService.filter(listOf(expenseWithBoth), criteriaMatchingCreatedAt)

            assertEquals(listOf(expenseWithBoth), resultOp)
            assertTrue(resultCreated.isEmpty())
        }

        @Test
        fun `falls back to createdAt when operationDate is null`() {
            val expenseWithCreatedAtOnly = expense1.copy(
                id = "exp-created-only",
                operationDate = null,
                createdAt = baseDate.atTime(12, 0)
            )
            val criteria = ExpenseFilterCriteria(startDate = baseDate, endDate = baseDate)

            val result = filterService.filter(listOf(expenseWithCreatedAtOnly), criteria)

            assertEquals(listOf(expenseWithCreatedAtOnly), result)
        }

        @Test
        fun `matchesDateRange matches expense on local date across midnight in positive offset zone`() {
            val shanghaiFilterService = ExpenseFilterServiceImpl(
                expenseSearchService = searchService,
                zoneIdProvider = { ZoneId.of("Asia/Shanghai") }
            )
            val expense = expense1.copy(
                id = "exp-cst",
                operationDate = null,
                createdAt = LocalDateTime.of(2026, 10, 27, 22, 58, 0)
            )

            val matchingCriteria = ExpenseFilterCriteria(
                startDate = LocalDate.of(2026, 10, 28),
                endDate = LocalDate.of(2026, 10, 28)
            )
            val resultMatching = shanghaiFilterService.filter(listOf(expense), matchingCriteria)
            assertEquals(listOf(expense), resultMatching)

            val previousDayCriteria = ExpenseFilterCriteria(
                startDate = LocalDate.of(2026, 10, 27),
                endDate = LocalDate.of(2026, 10, 27)
            )
            val resultPreviousDay = shanghaiFilterService.filter(listOf(expense), previousDayCriteria)
            assertTrue(resultPreviousDay.isEmpty())
        }

        @Test
        fun `matchesDateRange matches expense on local date across midnight in negative offset zone`() {
            val newYorkFilterService = ExpenseFilterServiceImpl(
                expenseSearchService = searchService,
                zoneIdProvider = { ZoneId.of("America/New_York") }
            )
            val expense = expense1.copy(
                id = "exp-edt",
                operationDate = null,
                createdAt = LocalDateTime.of(2026, 10, 28, 2, 0, 0)
            )

            val previousDayCriteria = ExpenseFilterCriteria(
                startDate = LocalDate.of(2026, 10, 27),
                endDate = LocalDate.of(2026, 10, 27)
            )
            val resultPreviousDay = newYorkFilterService.filter(listOf(expense), previousDayCriteria)
            assertEquals(listOf(expense), resultPreviousDay)

            val nextDayCriteria = ExpenseFilterCriteria(
                startDate = LocalDate.of(2026, 10, 28),
                endDate = LocalDate.of(2026, 10, 28)
            )
            val resultNextDay = newYorkFilterService.filter(listOf(expense), nextDayCriteria)
            assertTrue(resultNextDay.isEmpty())
        }
    }

    @Nested
    @DisplayName("Payment Method Filtering")
    inner class PaymentMethodFiltering {

        private val cashExpense = expense1.copy(
            id = "exp-cash",
            paymentMethod = PaymentMethod.CASH
        )
        private val cardExpense = expense2.copy(
            id = "exp-card",
            paymentMethod = PaymentMethod.CREDIT_CARD
        )
        private val alipayExpense = expense3.copy(
            id = "exp-alipay",
            paymentMethod = PaymentMethod.ALIPAY
        )
        private val paymentMethodExpenses = listOf(
            cashExpense,
            cardExpense,
            alipayExpense
        )

        @Test
        fun `filters expenses matching single selected payment method`() {
            val criteria = ExpenseFilterCriteria(selectedPaymentMethods = setOf(PaymentMethod.CASH))
            val result = filterService.filter(paymentMethodExpenses, criteria)
            assertEquals(listOf(cashExpense), result)
        }

        @Test
        fun `filters expenses matching multiple selected payment methods using OR logic`() {
            val criteria = ExpenseFilterCriteria(
                selectedPaymentMethods = setOf(PaymentMethod.CASH, PaymentMethod.ALIPAY)
            )
            val result = filterService.filter(paymentMethodExpenses, criteria)
            assertEquals(listOf(cashExpense, alipayExpense), result)
        }

        @Test
        fun `returns empty list when no expenses match selected payment method`() {
            val criteria = ExpenseFilterCriteria(selectedPaymentMethods = setOf(PaymentMethod.BIZUM))
            val result = filterService.filter(paymentMethodExpenses, criteria)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Payment Status Filtering")
    inner class PaymentStatusFiltering {

        private val finishedExpense = expense1.copy(
            id = "exp-finished",
            paymentStatus = PaymentStatus.FINISHED
        )
        private val scheduledExpense = expense2.copy(
            id = "exp-scheduled",
            paymentStatus = PaymentStatus.SCHEDULED
        )
        private val refundableExpense = expense3.copy(
            id = "exp-refundable",
            paymentStatus = PaymentStatus.REFUNDABLE
        )
        private val cancelledExpense = expense4.copy(
            id = "exp-cancelled",
            paymentStatus = PaymentStatus.CANCELLED
        )
        private val paymentStatusExpenses = listOf(
            finishedExpense,
            scheduledExpense,
            refundableExpense,
            cancelledExpense
        )

        @Test
        fun `filters expenses matching single selected payment status`() {
            val criteria = ExpenseFilterCriteria(selectedPaymentStatuses = setOf(PaymentStatus.SCHEDULED))
            val result = filterService.filter(paymentStatusExpenses, criteria)
            assertEquals(listOf(scheduledExpense), result)
        }

        @Test
        fun `filters expenses matching multiple selected payment statuses using OR logic`() {
            val criteria = ExpenseFilterCriteria(
                selectedPaymentStatuses = setOf(PaymentStatus.SCHEDULED, PaymentStatus.REFUNDABLE)
            )
            val result = filterService.filter(paymentStatusExpenses, criteria)
            assertEquals(listOf(scheduledExpense, refundableExpense), result)
        }

        @Test
        fun `returns empty list when no expenses match selected payment status`() {
            val criteria = ExpenseFilterCriteria(selectedPaymentStatuses = setOf(PaymentStatus.PENDING))
            val result = filterService.filter(paymentStatusExpenses, criteria)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Combined Filter Criteria (Logical AND)")
    inner class CombinedFiltering {

        @Test
        fun `combines category, payment method, payment status, and member filters using AND logic`() {
            val matchingExpense = expense1.copy(
                id = "exp-match",
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.RESTAURANT,
                payerId = "user-1",
                paymentMethod = PaymentMethod.CREDIT_CARD,
                paymentStatus = PaymentStatus.FINISHED
            )
            val wrongCategory = matchingExpense.copy(
                id = "exp-wrong-cat",
                category = ExpenseCategory.ACTIVITIES,
                subcategory = ExpenseSubcategory.MUSEUM_CULTURE
            )
            val wrongMethod = matchingExpense.copy(
                id = "exp-wrong-method",
                paymentMethod = PaymentMethod.CASH
            )
            val wrongStatus = matchingExpense.copy(
                id = "exp-wrong-status",
                paymentStatus = PaymentStatus.CANCELLED
            )
            val wrongMember = matchingExpense.copy(
                id = "exp-wrong-member",
                payerId = "user-3",
                splits = listOf(ExpenseSplit(userId = "user-3", amountCents = 2500L))
            )

            val expenses = listOf(matchingExpense, wrongCategory, wrongMethod, wrongStatus, wrongMember)
            val criteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedMemberIds = setOf("user-1"),
                selectedPaymentMethods = setOf(PaymentMethod.CREDIT_CARD),
                selectedPaymentStatuses = setOf(PaymentStatus.FINISHED)
            )

            val result = filterService.filter(expenses, criteria)

            assertEquals(listOf(matchingExpense), result)
        }

        @Test
        fun `combines search query, category, member, and date range`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "Italian",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedMemberIds = setOf("user-1"),
                startDate = baseDate.minusDays(1),
                endDate = baseDate.plusDays(1)
            )

            val result = filterService.filter(allExpenses, criteria)

            assertEquals(listOf(expense1), result)
            assertEquals(3, criteria.activeFilterCount)
            assertTrue(criteria.isActive)
        }

        @Test
        fun `returns empty list when combination has no matches`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "Flight",
                selectedCategories = setOf(ExpenseCategory.FOOD)
            )

            val result = filterService.filter(allExpenses, criteria)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `combines amount search query with category and member filters`() {
            val expenseWithAmount = expense1.copy(
                sourceAmount = 5000L,
                sourceCurrency = "EUR",
                groupAmount = 5000L,
                groupCurrency = "EUR"
            )
            val expenses = listOf(expenseWithAmount, expense2, expense3)
            val criteria = ExpenseFilterCriteria(
                searchQuery = "50,00",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedMemberIds = setOf("user-1")
            )

            val result = filterService.filter(expenses, criteria)

            assertEquals(listOf(expenseWithAmount), result)
        }

        @Test
        fun `clearNonSearchFilters preserves search query and resets other dimensions`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "Dinner",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedMemberIds = setOf("user-1"),
                startDate = baseDate,
                endDate = baseDate.plusDays(5)
            )

            val cleared = criteria.clearNonSearchFilters()

            assertEquals("Dinner", cleared.searchQuery)
            assertTrue(cleared.selectedCategories.isEmpty())
            assertTrue(cleared.selectedSubcategories.isEmpty())
            assertTrue(cleared.selectedMemberIds.isEmpty())
            assertEquals(null, cleared.startDate)
            assertEquals(null, cleared.endDate)
            assertEquals(0, cleared.activeFilterCount)
            assertTrue(cleared.isActive)
        }

        @Test
        fun `clearAll resets all criteria to defaults`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "Dinner",
                selectedCategories = setOf(ExpenseCategory.FOOD)
            )

            val cleared = criteria.clearAll()

            assertEquals(ExpenseFilterCriteria(), cleared)
            assertFalse(cleared.isActive)
        }
    }
}
