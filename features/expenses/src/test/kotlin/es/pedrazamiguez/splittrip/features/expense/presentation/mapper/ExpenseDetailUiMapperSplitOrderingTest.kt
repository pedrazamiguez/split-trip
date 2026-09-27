package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.impl.AddOnCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitBreakdownItemUiModel
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpenseDetailUiMapperSplitOrderingTest {

    private lateinit var mapper: ExpenseDetailUiMapper
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var localeProvider: LocaleProvider

    private val currentUserId = "user-me"
    private val userAliceId = "user-alice"
    private val userBobId = "user-bob"
    private val userZackId = "user-zack"

    private val memberProfiles = mapOf(
        currentUserId to User(userId = currentUserId, displayName = "Me", email = "me@example.com"),
        userAliceId to User(userId = userAliceId, displayName = "Alice", email = "alice@example.com"),
        userBobId to User(userId = userBobId, displayName = "Bob", email = "bob@example.com"),
        userZackId to User(userId = userZackId, displayName = "Zack", email = "zack@example.com")
    )

    private val subunitNameLookup = mapOf(
        "sub-alpha" to "Alpha Subunit",
        "sub-beta" to "Beta Subunit",
        "sub-gamma" to "Gamma Subunit"
    )

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        resourceProvider = mockk(relaxed = true)
        every { localeProvider.getCurrentLocale() } returns Locale.US
        every { resourceProvider.getString(any()) } returns "label"
        every { resourceProvider.getString(any(), any()) } returns "label_with_arg"
        every { resourceProvider.getString(R.string.you_label) } returns "You"

        val formattingHelper = FormattingHelper(localeProvider)
        val userUiMapper = UserUiMapper(resourceProvider, localeProvider)
        val paymentStatusBadgeUiMapper = PaymentStatusBadgeUiMapper(formattingHelper, resourceProvider)

        mapper = ExpenseDetailUiMapper(
            formattingHelper = formattingHelper,
            resourceProvider = resourceProvider,
            expenseCalculatorService = ExpenseCalculatorServiceImpl(),
            addOnCalculationService = AddOnCalculationServiceImpl(),
            paymentStatusBadgeUiMapper = paymentStatusBadgeUiMapper,
            userUiMapper = userUiMapper,
            localeProvider = localeProvider
        )
    }

    private fun createExpense(splits: List<ExpenseSplit>): Expense {
        return Expense(
            id = "exp-test",
            groupId = "group-1",
            title = "Test Expense",
            sourceAmount = 10000L,
            sourceCurrency = "EUR",
            groupAmount = 10000L,
            groupCurrency = "EUR",
            exchangeRate = BigDecimal.ONE,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            paymentStatus = PaymentStatus.FINISHED,
            splitType = SplitType.EQUAL,
            category = ExpenseCategory.OTHER,
            createdBy = userAliceId,
            splits = splits,
            createdAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        )
    }

    @Nested
    inner class PriorityHelperTests {

        @Test
        fun `when isUserEntity is true priority is always 0`() {
            assertEquals(
                0,
                getExpenseDetailEntityPriority(isUserEntity = true, isUserInSubunit = true, isSubunit = true)
            )
            assertEquals(
                0,
                getExpenseDetailEntityPriority(isUserEntity = true, isUserInSubunit = false, isSubunit = false)
            )
            assertEquals(
                0,
                getExpenseDetailEntityPriority(isUserEntity = true, isUserInSubunit = false, isSubunit = true)
            )
        }

        @Test
        fun `when user in subunit priority 1 is other subunits and priority 2 is solo`() {
            assertEquals(
                1,
                getExpenseDetailEntityPriority(isUserEntity = false, isUserInSubunit = true, isSubunit = true)
            )
            assertEquals(
                2,
                getExpenseDetailEntityPriority(isUserEntity = false, isUserInSubunit = true, isSubunit = false)
            )
        }

        @Test
        fun `when user not in subunit priority 1 is solo and priority 2 is subunit`() {
            assertEquals(
                1,
                getExpenseDetailEntityPriority(isUserEntity = false, isUserInSubunit = false, isSubunit = false)
            )
            assertEquals(
                2,
                getExpenseDetailEntityPriority(isUserEntity = false, isUserInSubunit = false, isSubunit = true)
            )
        }
    }

    @Nested
    inner class SplitBreakdownOrderingTests {

        @Test
        fun `when current user is solo they appear first followed by other solo then subunits`() {
            val splits = listOf(
                ExpenseSplit(userId = userZackId, amountCents = 2000L),
                ExpenseSplit(userId = userAliceId, amountCents = 2000L),
                ExpenseSplit(userId = currentUserId, amountCents = 2000L),
                ExpenseSplit(userId = userBobId, amountCents = 2000L, subunitId = "sub-beta"),
                ExpenseSplit(userId = userAliceId, amountCents = 2000L, subunitId = "sub-alpha")
            )
            val expense = createExpense(splits)

            val result = mapper.map(
                expense = expense,
                memberProfiles = memberProfiles,
                currentUserId = currentUserId,
                subunitNameLookup = subunitNameLookup
            )

            val breakdown = result.splitBreakdownItems
            assertEquals(5, breakdown.size)

            // Index 0: Current user (Solo "You")
            assertTrue(breakdown[0] is SplitBreakdownItemUiModel.Solo)
            assertEquals("You", (breakdown[0] as SplitBreakdownItemUiModel.Solo).split.displayName)
            assertTrue((breakdown[0] as SplitBreakdownItemUiModel.Solo).split.isCurrentUser)

            // Index 1 & 2: Other solo members alphabetically (Alice, Zack)
            assertTrue(breakdown[1] is SplitBreakdownItemUiModel.Solo)
            assertEquals("Alice", (breakdown[1] as SplitBreakdownItemUiModel.Solo).split.displayName)
            assertTrue(breakdown[2] is SplitBreakdownItemUiModel.Solo)
            assertEquals("Zack", (breakdown[2] as SplitBreakdownItemUiModel.Solo).split.displayName)

            // Index 3 & 4: Subunits alphabetically ("Alpha Subunit", "Beta Subunit")
            assertTrue(breakdown[3] is SplitBreakdownItemUiModel.Subunit)
            assertEquals("Alpha Subunit", (breakdown[3] as SplitBreakdownItemUiModel.Subunit).group.subunitLabel)
            assertTrue(breakdown[4] is SplitBreakdownItemUiModel.Subunit)
            assertEquals("Beta Subunit", (breakdown[4] as SplitBreakdownItemUiModel.Subunit).group.subunitLabel)
        }

        @Test
        fun `when current user is in a subunit that subunit appears first followed by other subunits then solo`() {
            val splits = listOf(
                ExpenseSplit(userId = userAliceId, amountCents = 2000L),
                ExpenseSplit(userId = userZackId, amountCents = 2000L),
                ExpenseSplit(userId = userBobId, amountCents = 2000L, subunitId = "sub-alpha"),
                ExpenseSplit(userId = currentUserId, amountCents = 2000L, subunitId = "sub-beta"),
                ExpenseSplit(userId = userAliceId, amountCents = 2000L, subunitId = "sub-beta")
            )
            val expense = createExpense(splits)

            val result = mapper.map(
                expense = expense,
                memberProfiles = memberProfiles,
                currentUserId = currentUserId,
                subunitNameLookup = subunitNameLookup
            )

            val breakdown = result.splitBreakdownItems
            assertEquals(4, breakdown.size)

            // Index 0: Beta Subunit (contains current user)
            assertTrue(breakdown[0] is SplitBreakdownItemUiModel.Subunit)
            assertEquals("Beta Subunit", (breakdown[0] as SplitBreakdownItemUiModel.Subunit).group.subunitLabel)

            // Index 1: Other subunits (Alpha Subunit)
            assertTrue(breakdown[1] is SplitBreakdownItemUiModel.Subunit)
            assertEquals("Alpha Subunit", (breakdown[1] as SplitBreakdownItemUiModel.Subunit).group.subunitLabel)

            // Index 2 & 3: Solo members alphabetically (Alice, Zack)
            assertTrue(breakdown[2] is SplitBreakdownItemUiModel.Solo)
            assertEquals("Alice", (breakdown[2] as SplitBreakdownItemUiModel.Solo).split.displayName)
            assertTrue(breakdown[3] is SplitBreakdownItemUiModel.Solo)
            assertEquals("Zack", (breakdown[3] as SplitBreakdownItemUiModel.Solo).split.displayName)
        }

        @Test
        fun `within subunit card current user is pinned to index 0 followed alphabetically`() {
            val splits = listOf(
                ExpenseSplit(userId = userZackId, amountCents = 2000L, subunitId = "sub-alpha"),
                ExpenseSplit(userId = currentUserId, amountCents = 2000L, subunitId = "sub-alpha"),
                ExpenseSplit(userId = userAliceId, amountCents = 2000L, subunitId = "sub-alpha")
            )
            val expense = createExpense(splits)

            val result = mapper.map(
                expense = expense,
                memberProfiles = memberProfiles,
                currentUserId = currentUserId,
                subunitNameLookup = subunitNameLookup
            )

            val breakdown = result.splitBreakdownItems
            assertEquals(1, breakdown.size)
            assertTrue(breakdown[0] is SplitBreakdownItemUiModel.Subunit)

            val subunitMembers = (breakdown[0] as SplitBreakdownItemUiModel.Subunit).group.members
            assertEquals(3, subunitMembers.size)
            assertEquals("You", subunitMembers[0].displayName)
            assertTrue(subunitMembers[0].isCurrentUser)
            assertEquals("Alice", subunitMembers[1].displayName)
            assertEquals("Zack", subunitMembers[2].displayName)
        }

        @Test
        fun `flat split mode puts current user at index 0 followed alphabetically`() {
            val splits = listOf(
                ExpenseSplit(userId = userZackId, amountCents = 2500L),
                ExpenseSplit(userId = userAliceId, amountCents = 2500L),
                ExpenseSplit(userId = currentUserId, amountCents = 2500L),
                ExpenseSplit(userId = userBobId, amountCents = 2500L)
            )
            val expense = createExpense(splits)

            val result = mapper.map(
                expense = expense,
                memberProfiles = memberProfiles,
                currentUserId = currentUserId
            )

            val breakdown = result.splitBreakdownItems
            assertEquals(4, breakdown.size)
            assertEquals("You", (breakdown[0] as SplitBreakdownItemUiModel.Solo).split.displayName)
            assertEquals("Alice", (breakdown[1] as SplitBreakdownItemUiModel.Solo).split.displayName)
            assertEquals("Bob", (breakdown[2] as SplitBreakdownItemUiModel.Solo).split.displayName)
            assertEquals("Zack", (breakdown[3] as SplitBreakdownItemUiModel.Solo).split.displayName)

            // Also verify backward compatibility splits property
            assertEquals(4, result.splits.size)
            assertEquals("You", result.splits[0].displayName)
            assertEquals("Alice", result.splits[1].displayName)
            assertEquals("Bob", result.splits[2].displayName)
            assertEquals("Zack", result.splits[3].displayName)
        }

        @Test
        fun `when current user is not in expense solo members appear alphabetically then subunits alphabetically`() {
            val splits = listOf(
                ExpenseSplit(userId = userZackId, amountCents = 2500L),
                ExpenseSplit(userId = userAliceId, amountCents = 2500L),
                ExpenseSplit(userId = userBobId, amountCents = 2500L, subunitId = "sub-beta"),
                ExpenseSplit(userId = userAliceId, amountCents = 2500L, subunitId = "sub-alpha")
            )
            val expense = createExpense(splits)

            val result = mapper.map(
                expense = expense,
                memberProfiles = memberProfiles,
                currentUserId = currentUserId,
                subunitNameLookup = subunitNameLookup
            )

            val breakdown = result.splitBreakdownItems
            assertEquals(4, breakdown.size)

            // Solo first, alphabetically
            assertTrue(breakdown[0] is SplitBreakdownItemUiModel.Solo)
            assertEquals("Alice", (breakdown[0] as SplitBreakdownItemUiModel.Solo).split.displayName)
            assertTrue(breakdown[1] is SplitBreakdownItemUiModel.Solo)
            assertEquals("Zack", (breakdown[1] as SplitBreakdownItemUiModel.Solo).split.displayName)

            // Subunits second, alphabetically
            assertTrue(breakdown[2] is SplitBreakdownItemUiModel.Subunit)
            assertEquals("Alpha Subunit", (breakdown[2] as SplitBreakdownItemUiModel.Subunit).group.subunitLabel)
            assertTrue(breakdown[3] is SplitBreakdownItemUiModel.Subunit)
            assertEquals("Beta Subunit", (breakdown[3] as SplitBreakdownItemUiModel.Subunit).group.subunitLabel)
        }

        @Test
        fun `when current user id is null solo members appear alphabetically then subunits alphabetically`() {
            val splits = listOf(
                ExpenseSplit(userId = userZackId, amountCents = 2500L),
                ExpenseSplit(userId = userAliceId, amountCents = 2500L),
                ExpenseSplit(userId = userBobId, amountCents = 2500L, subunitId = "sub-alpha")
            )
            val expense = createExpense(splits)

            val result = mapper.map(
                expense = expense,
                memberProfiles = memberProfiles,
                currentUserId = null,
                subunitNameLookup = subunitNameLookup
            )

            val breakdown = result.splitBreakdownItems
            assertEquals(3, breakdown.size)
            assertEquals("Alice", (breakdown[0] as SplitBreakdownItemUiModel.Solo).split.displayName)
            assertEquals("Zack", (breakdown[1] as SplitBreakdownItemUiModel.Solo).split.displayName)
            assertEquals("Alpha Subunit", (breakdown[2] as SplitBreakdownItemUiModel.Subunit).group.subunitLabel)
        }
    }
}
