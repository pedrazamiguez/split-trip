package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.expense.presentation.model.DateRangePreset
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ExpensesFilterUiMapper")
class ExpensesFilterUiMapperTest {

    private lateinit var formattingHelper: FormattingHelper
    private lateinit var userUiMapper: UserUiMapper
    private lateinit var mapper: ExpensesFilterUiMapper

    @BeforeEach
    fun setUp() {
        formattingHelper = mockk()
        userUiMapper = mockk()
        mapper = ExpensesFilterUiMapper(
            formattingHelper = formattingHelper,
            userUiMapper = userUiMapper
        )
    }

    @Nested
    @DisplayName("extractDateBounds")
    inner class ExtractDateBounds {

        @Test
        fun `returns null bounds when expenses list is empty`() {
            val bounds = mapper.extractDateBounds(emptyList())
            assertNull(bounds.startDate)
            assertNull(bounds.endDate)
        }

        @Test
        fun `returns min and max dates based on effectiveDate`() {
            val expense1 = Expense(
                id = "exp-1",
                createdAt = LocalDateTime.of(2026, 6, 10, 12, 0)
            )
            val expense2 = Expense(
                id = "exp-2",
                operationDate = LocalDateTime.of(2026, 6, 2, 10, 0),
                createdAt = LocalDateTime.of(2026, 6, 15, 10, 0)
            )
            val expense3 = Expense(
                id = "exp-3",
                operationDate = LocalDateTime.of(2026, 6, 20, 18, 0)
            )

            val bounds = mapper.extractDateBounds(listOf(expense1, expense2, expense3))

            assertEquals(LocalDate.of(2026, 6, 2), bounds.startDate)
            assertEquals(LocalDate.of(2026, 6, 20), bounds.endDate)
        }

        @Test
        fun `ignores expenses with null effectiveDate`() {
            val nullDateExpense = Expense(id = "exp-null", operationDate = null, createdAt = null)
            val validExpense = Expense(id = "exp-valid", createdAt = LocalDateTime.of(2026, 6, 5, 8, 0))

            val bounds = mapper.extractDateBounds(listOf(nullDateExpense, validExpense))

            assertEquals(LocalDate.of(2026, 6, 5), bounds.startDate)
            assertEquals(LocalDate.of(2026, 6, 5), bounds.endDate)
        }

        @Test
        fun `extractDateBounds converts UTC dates to target timezone bounds across midnight`() {
            val expense = Expense(
                id = "exp-cst",
                createdAt = LocalDateTime.of(2026, 10, 27, 22, 58, 0)
            )

            val bounds = mapper.extractDateBounds(listOf(expense), zoneId = ZoneId.of("Asia/Shanghai"))

            assertEquals(LocalDate.of(2026, 10, 28), bounds.startDate)
            assertEquals(LocalDate.of(2026, 10, 28), bounds.endDate)
        }
    }

    @Nested
    @DisplayName("formatFilterDate")
    inner class FormatFilterDate {

        @Test
        fun `delegates to formattingHelper`() {
            val date = LocalDate.of(2026, 6, 15)
            every { formattingHelper.formatShortDate(date) } returns "15 Jun"

            val result = mapper.formatFilterDate(date)

            assertEquals("15 Jun", result)
        }

        @Test
        fun `returns empty string when date is null`() {
            every { formattingHelper.formatShortDate(null as LocalDate?) } returns ""

            val result = mapper.formatFilterDate(null)

            assertEquals("", result)
        }
    }

    @Nested
    @DisplayName("mapAvailableMembers")
    inner class MapAvailableMembers {

        @Test
        fun `places current user first and sorts others alphabetically`() {
            val allUserIds = listOf("user-2", "user-1", "user-3")
            val memberProfiles = mapOf(
                "user-1" to User(userId = "user-1", email = "user1@test.com", displayName = "John Doe"),
                "user-2" to User(userId = "user-2", email = "user2@test.com", displayName = "Carlos"),
                "user-3" to User(userId = "user-3", email = "user3@test.com", displayName = "Ana")
            )

            every {
                userUiMapper.mapToDisplayName(
                    user = any(),
                    fallbackUserId = any(),
                    currentUserId = any(),
                    youLabel = any(),
                    selfIdentificationContext = any(),
                    gender = any()
                )
            } answers {
                val user = firstArg<User?>()
                val fallback = secondArg<String>()
                val current = thirdArg<String?>()
                if (fallback == current) "You" else user?.displayName ?: fallback
            }

            val result = mapper.mapAvailableMembers(
                allUserIds = allUserIds,
                memberProfiles = memberProfiles,
                currentUserId = "user-1"
            )

            assertEquals(3, result.size)
            assertEquals("user-1", result[0].userId)
            assertEquals("You", result[0].displayName)
            assertTrue(result[0].isCurrentUser)

            assertEquals("user-3", result[1].userId)
            assertEquals("Ana", result[1].displayName)

            assertEquals("user-2", result[2].userId)
            assertEquals("Carlos", result[2].displayName)
        }
    }

    @Nested
    @DisplayName("calculatePresetRange")
    inner class CalculatePresetRanges {

        private val anchor = LocalDate.of(2026, 8, 18) // Tuesday

        @Test
        fun `calculates TODAY range`() {
            val range = mapper.calculatePresetRange(DateRangePreset.TODAY, anchor)
            assertEquals(anchor, range.startDate)
            assertEquals(anchor, range.endDate)
        }

        @Test
        fun `calculates YESTERDAY range`() {
            val range = mapper.calculatePresetRange(DateRangePreset.YESTERDAY, anchor)
            assertEquals(LocalDate.of(2026, 8, 17), range.startDate)
            assertEquals(LocalDate.of(2026, 8, 17), range.endDate)
        }

        @Test
        fun `calculates THIS_WEEK range from Monday`() {
            val range = mapper.calculatePresetRange(DateRangePreset.THIS_WEEK, anchor)
            assertEquals(LocalDate.of(2026, 8, 17), range.startDate) // Monday
            assertEquals(anchor, range.endDate)
        }

        @Test
        fun `calculates LAST_15_DAYS range`() {
            val range = mapper.calculatePresetRange(DateRangePreset.LAST_15_DAYS, anchor)
            assertEquals(LocalDate.of(2026, 8, 4), range.startDate)
            assertEquals(anchor, range.endDate)
        }

        @Test
        fun `calculates THIS_MONTH range`() {
            val range = mapper.calculatePresetRange(DateRangePreset.THIS_MONTH, anchor)
            assertEquals(LocalDate.of(2026, 8, 1), range.startDate)
            assertEquals(anchor, range.endDate)
        }
    }

    @Nested
    @DisplayName("findMatchingPreset")
    inner class FindMatchingPreset {

        private val anchor = LocalDate.of(2026, 8, 18)

        @Test
        fun `returns null when start or end is null`() {
            assertNull(mapper.findMatchingPreset(null, anchor, anchor))
            assertNull(mapper.findMatchingPreset(anchor, null, anchor))
            assertNull(mapper.findMatchingPreset(null, null, anchor))
        }

        @Test
        fun `identifies matching presets correctly`() {
            assertEquals(
                DateRangePreset.TODAY,
                mapper.findMatchingPreset(anchor, anchor, anchor)
            )
            assertEquals(
                DateRangePreset.YESTERDAY,
                mapper.findMatchingPreset(LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 17), anchor)
            )
            assertEquals(
                DateRangePreset.THIS_WEEK,
                mapper.findMatchingPreset(LocalDate.of(2026, 8, 17), anchor, anchor)
            )
            assertEquals(
                DateRangePreset.LAST_15_DAYS,
                mapper.findMatchingPreset(LocalDate.of(2026, 8, 4), anchor, anchor)
            )
            assertEquals(
                DateRangePreset.THIS_MONTH,
                mapper.findMatchingPreset(LocalDate.of(2026, 8, 1), anchor, anchor)
            )
        }

        @Test
        fun `returns null when dates do not match any preset`() {
            val customStart = LocalDate.of(2026, 7, 1)
            val customEnd = LocalDate.of(2026, 7, 10)
            assertNull(mapper.findMatchingPreset(customStart, customEnd, anchor))
        }
    }

    @Nested
    @DisplayName("extractAvailableCategories")
    inner class ExtractAvailableCategories {

        @Test
        fun `returns empty list when expenses list is empty`() {
            val result = mapper.extractAvailableCategories(emptyList())
            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns distinct used categories in canonical enum order`() {
            val expenses = listOf(
                Expense(id = "exp-1", category = ExpenseCategory.SHOPPING),
                Expense(id = "exp-2", category = ExpenseCategory.FOOD),
                Expense(id = "exp-3", category = ExpenseCategory.TRANSPORT),
                Expense(id = "exp-4", category = ExpenseCategory.FOOD)
            )

            val result = mapper.extractAvailableCategories(expenses)

            assertEquals(
                listOf(ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD, ExpenseCategory.SHOPPING),
                result
            )
        }

        @Test
        fun `includes everyday-only categories whenever expenses use them`() {
            val expenses = listOf(
                Expense(id = "exp-1", category = ExpenseCategory.HOME),
                Expense(id = "exp-2", category = ExpenseCategory.HEALTH_PERSONAL_CARE)
            )

            val result = mapper.extractAvailableCategories(expenses)

            assertEquals(
                listOf(ExpenseCategory.HOME, ExpenseCategory.HEALTH_PERSONAL_CARE),
                result
            )
        }
    }

    @Nested
    @DisplayName("extractAvailableSubcategories")
    inner class ExtractAvailableSubcategories {

        @Test
        fun `returns empty list when expenses list is empty`() {
            val result = mapper.extractAvailableSubcategories(emptyList())
            assertTrue(result.isEmpty())
        }

        @Test
        fun `excludes UNSPECIFIED subcategory when expenses have no specific subcategory`() {
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    category = ExpenseCategory.OTHER,
                    subcategory = ExpenseSubcategory.UNSPECIFIED
                )
            )

            val result = mapper.extractAvailableSubcategories(expenses)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns distinct used subcategories in canonical enum order excluding UNSPECIFIED`() {
            val expenses = listOf(
                Expense(id = "exp-1", subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET),
                Expense(id = "exp-2", subcategory = ExpenseSubcategory.TAXI_RIDESHARE),
                Expense(id = "exp-3", subcategory = ExpenseSubcategory.UNSPECIFIED),
                Expense(id = "exp-4", subcategory = ExpenseSubcategory.RESTAURANT),
                Expense(id = "exp-5", subcategory = ExpenseSubcategory.TAXI_RIDESHARE)
            )

            val result = mapper.extractAvailableSubcategories(expenses)

            assertEquals(
                listOf(
                    ExpenseSubcategory.TAXI_RIDESHARE,
                    ExpenseSubcategory.RESTAURANT,
                    ExpenseSubcategory.GROCERIES_SUPERMARKET
                ),
                result
            )
        }
    }
}
