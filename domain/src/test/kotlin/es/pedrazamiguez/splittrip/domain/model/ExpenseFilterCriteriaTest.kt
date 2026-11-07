package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.ExpenseAttribute
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ExpenseFilterCriteria")
class ExpenseFilterCriteriaTest {

    @Nested
    @DisplayName("Filter status properties")
    inner class FilterStatusProperties {

        @Test
        fun `default criteria is not active and has zero active count`() {
            val criteria = ExpenseFilterCriteria()
            assertFalse(criteria.isActive)
            assertFalse(criteria.isCategoryFiltered)
            assertFalse(criteria.isMemberFiltered)
            assertFalse(criteria.isDateFiltered)
            assertFalse(criteria.isSearchFiltered)
            assertEquals(0, criteria.activeFilterCount)
        }

        @Test
        fun `category filter flags active status`() {
            val criteria = ExpenseFilterCriteria(selectedCategories = setOf(ExpenseCategory.FOOD))
            assertTrue(criteria.isActive)
            assertTrue(criteria.isCategoryFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `subcategory filter flags active status`() {
            val criteria = ExpenseFilterCriteria(selectedSubcategories = setOf(ExpenseSubcategory.RESTAURANT))
            assertTrue(criteria.isActive)
            assertTrue(criteria.isCategoryFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `member filter flags active status`() {
            val criteria = ExpenseFilterCriteria(selectedMemberIds = setOf("user_1"))
            assertTrue(criteria.isActive)
            assertTrue(criteria.isMemberFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `date filter flags active status`() {
            val criteria = ExpenseFilterCriteria(startDate = LocalDate.now())
            assertTrue(criteria.isActive)
            assertTrue(criteria.isDateFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `search query flags active status without incrementing activeFilterCount`() {
            val criteria = ExpenseFilterCriteria(searchQuery = "dinner")
            assertTrue(criteria.isActive)
            assertTrue(criteria.isSearchFiltered)
            assertEquals(0, criteria.activeFilterCount)
        }

        @Test
        fun `payment method filter flags active status and increments activeFilterCount`() {
            val criteria = ExpenseFilterCriteria(selectedPaymentMethods = setOf(PaymentMethod.ALIPAY))
            assertTrue(criteria.isActive)
            assertTrue(criteria.isPaymentMethodFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `payment status filter flags active status and increments activeFilterCount`() {
            val criteria = ExpenseFilterCriteria(selectedPaymentStatuses = setOf(PaymentStatus.SCHEDULED))
            assertTrue(criteria.isActive)
            assertTrue(criteria.isPaymentStatusFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `attribute filter flags active status and increments activeFilterCount`() {
            val criteria = ExpenseFilterCriteria(selectedAttributes = setOf(ExpenseAttribute.HAS_RECEIPT))
            assertTrue(criteria.isActive)
            assertTrue(criteria.isAttributeFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `minAmount filter flags active status and increments activeFilterCount`() {
            val criteria = ExpenseFilterCriteria(minAmount = 1000L)
            assertTrue(criteria.isActive)
            assertTrue(criteria.isAmountFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `maxAmount filter flags active status and increments activeFilterCount`() {
            val criteria = ExpenseFilterCriteria(maxAmount = 5000L)
            assertTrue(criteria.isActive)
            assertTrue(criteria.isAmountFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `filterCurrency filter flags active status and increments activeFilterCount`() {
            val criteria = ExpenseFilterCriteria(filterCurrency = "USD")
            assertTrue(criteria.isActive)
            assertTrue(criteria.isAmountFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `combined amount and currency filter increments activeFilterCount by exactly 1`() {
            val criteria = ExpenseFilterCriteria(filterCurrency = "USD", minAmount = 1000L, maxAmount = 5000L)
            assertTrue(criteria.isActive)
            assertTrue(criteria.isAmountFiltered)
            assertEquals(1, criteria.activeFilterCount)
        }

        @Test
        fun `multi-criteria computes correct activeFilterCount`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "dinner",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedMemberIds = setOf("user_1"),
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(1)
            )
            assertTrue(criteria.isActive)
            assertEquals(3, criteria.activeFilterCount)
        }

        @Test
        fun `multi-criteria computes correct activeFilterCount including payment method and status`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "dinner",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedMemberIds = setOf("user_1"),
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(1),
                selectedPaymentMethods = setOf(PaymentMethod.ALIPAY),
                selectedPaymentStatuses = setOf(PaymentStatus.SCHEDULED)
            )
            assertTrue(criteria.isActive)
            assertEquals(5, criteria.activeFilterCount)
        }

        @Test
        fun `multi-criteria computes correct activeFilterCount including attributes`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "dinner",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedMemberIds = setOf("user_1"),
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(1),
                selectedPaymentMethods = setOf(PaymentMethod.ALIPAY),
                selectedPaymentStatuses = setOf(PaymentStatus.SCHEDULED),
                selectedAttributes = setOf(ExpenseAttribute.HAS_RECEIPT)
            )
            assertTrue(criteria.isActive)
            assertEquals(6, criteria.activeFilterCount)
        }

        @Test
        fun `multi-criteria computes correct activeFilterCount including amount filter`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "dinner",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedMemberIds = setOf("user_1"),
                startDate = LocalDate.now(),
                selectedPaymentMethods = setOf(PaymentMethod.ALIPAY),
                selectedPaymentStatuses = setOf(PaymentStatus.SCHEDULED),
                filterCurrency = "USD",
                minAmount = 1000L
            )
            assertTrue(criteria.isActive)
            assertEquals(6, criteria.activeFilterCount)
        }
    }

    @Nested
    @DisplayName("Reset methods")
    inner class ResetMethods {

        @Test
        fun `clearNonSearchFilters preserves search query and resets all dimensions`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "dinner",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedSubcategories = setOf(ExpenseSubcategory.RESTAURANT),
                selectedMemberIds = setOf("user_1"),
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(2)
            )
            val cleared = criteria.clearNonSearchFilters()
            assertEquals("dinner", cleared.searchQuery)
            assertFalse(cleared.isCategoryFiltered)
            assertFalse(cleared.isMemberFiltered)
            assertFalse(cleared.isDateFiltered)
            assertEquals(0, cleared.activeFilterCount)
        }

        @Test
        fun `clearNonSearchFilters clears payment method and payment status filters`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "dinner",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedSubcategories = setOf(ExpenseSubcategory.RESTAURANT),
                selectedMemberIds = setOf("user_1"),
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(2),
                selectedPaymentMethods = setOf(PaymentMethod.ALIPAY),
                selectedPaymentStatuses = setOf(PaymentStatus.SCHEDULED)
            )
            val cleared = criteria.clearNonSearchFilters()
            assertEquals("dinner", cleared.searchQuery)
            assertFalse(cleared.isCategoryFiltered)
            assertFalse(cleared.isMemberFiltered)
            assertFalse(cleared.isDateFiltered)
            assertFalse(cleared.isPaymentMethodFiltered)
            assertFalse(cleared.isPaymentStatusFiltered)
            assertTrue(cleared.selectedPaymentMethods.isEmpty())
            assertTrue(cleared.selectedPaymentStatuses.isEmpty())
            assertEquals(0, cleared.activeFilterCount)
        }

        @Test
        fun `clearNonSearchFilters clears attribute filters`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "dinner",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedAttributes = setOf(ExpenseAttribute.HAS_RECEIPT, ExpenseAttribute.HAS_NOTE)
            )
            val cleared = criteria.clearNonSearchFilters()
            assertEquals("dinner", cleared.searchQuery)
            assertFalse(cleared.isCategoryFiltered)
            assertFalse(cleared.isAttributeFiltered)
            assertTrue(cleared.selectedAttributes.isEmpty())
            assertEquals(0, cleared.activeFilterCount)
        }

        @Test
        fun `clearNonSearchFilters clears amount range and currency filters`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "dinner",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                filterCurrency = "USD",
                minAmount = 1000L,
                maxAmount = 5000L
            )
            val cleared = criteria.clearNonSearchFilters()
            assertEquals("dinner", cleared.searchQuery)
            assertFalse(cleared.isCategoryFiltered)
            assertFalse(cleared.isAmountFiltered)
            assertNull(cleared.filterCurrency)
            assertNull(cleared.minAmount)
            assertNull(cleared.maxAmount)
            assertEquals(0, cleared.activeFilterCount)
        }

        @Test
        fun `clearAll returns clean default criteria`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "dinner",
                selectedCategories = setOf(ExpenseCategory.FOOD)
            )
            val cleared = criteria.clearAll()
            assertEquals(ExpenseFilterCriteria(), cleared)
        }
    }

    @Nested
    @DisplayName("Serialization")
    inner class Serialization {

        @Test
        fun `serializes and deserializes correctly via Java Serializable`() {
            val original = ExpenseFilterCriteria(
                searchQuery = "flight ticket",
                selectedCategories = setOf(ExpenseCategory.TRANSPORT),
                selectedSubcategories = setOf(ExpenseSubcategory.INTERNATIONAL_FLIGHT),
                selectedMemberIds = setOf("user_1", "user_2"),
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 8, 15)
            )

            val byteArrayOutputStream = ByteArrayOutputStream()
            ObjectOutputStream(byteArrayOutputStream).use { it.writeObject(original) }

            val byteArrayInputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
            val deserialized = ObjectInputStream(byteArrayInputStream).use {
                it.readObject() as ExpenseFilterCriteria
            }

            assertEquals(original, deserialized)
        }

        @Test
        fun `serializes and deserializes criteria with payment methods and statuses`() {
            val original = ExpenseFilterCriteria(
                searchQuery = "flight ticket",
                selectedCategories = setOf(ExpenseCategory.TRANSPORT),
                selectedSubcategories = setOf(ExpenseSubcategory.INTERNATIONAL_FLIGHT),
                selectedMemberIds = setOf("user_1", "user_2"),
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 8, 15),
                selectedPaymentMethods = setOf(PaymentMethod.CASH, PaymentMethod.WECHAT_PAY),
                selectedPaymentStatuses = setOf(PaymentStatus.FINISHED, PaymentStatus.CANCELLED)
            )

            val byteArrayOutputStream = ByteArrayOutputStream()
            ObjectOutputStream(byteArrayOutputStream).use { it.writeObject(original) }

            val byteArrayInputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
            val deserialized = ObjectInputStream(byteArrayInputStream).use {
                it.readObject() as ExpenseFilterCriteria
            }

            assertEquals(original, deserialized)
        }

        @Test
        fun `serializes and deserializes criteria with attributes`() {
            val original = ExpenseFilterCriteria(
                searchQuery = "hotel booking",
                selectedCategories = setOf(ExpenseCategory.LODGING),
                selectedAttributes = setOf(ExpenseAttribute.HAS_RECEIPT, ExpenseAttribute.HAS_EXTRAS)
            )

            val byteArrayOutputStream = ByteArrayOutputStream()
            ObjectOutputStream(byteArrayOutputStream).use { it.writeObject(original) }

            val byteArrayInputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
            val deserialized = ObjectInputStream(byteArrayInputStream).use {
                it.readObject() as ExpenseFilterCriteria
            }

            assertEquals(original, deserialized)
        }

        @Test
        fun `serializes and deserializes criteria with amount range and currency`() {
            val original = ExpenseFilterCriteria(
                filterCurrency = "USD",
                minAmount = 1000L,
                maxAmount = 5000L
            )

            val byteArrayOutputStream = ByteArrayOutputStream()
            ObjectOutputStream(byteArrayOutputStream).use { it.writeObject(original) }

            val byteArrayInputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
            val deserialized = ObjectInputStream(byteArrayInputStream).use {
                it.readObject() as ExpenseFilterCriteria
            }

            assertEquals(original, deserialized)
        }
    }
}
