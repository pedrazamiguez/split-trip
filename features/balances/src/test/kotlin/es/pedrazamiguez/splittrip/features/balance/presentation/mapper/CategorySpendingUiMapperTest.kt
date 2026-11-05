package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReceiptEuro
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.features.balance.R
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CategorySpendingUiMapperTest {

    private val localeProvider = mockk<LocaleProvider>()
    private val resourceProvider = mockk<ResourceProvider>()
    private lateinit var mapper: CategorySpendingUiMapper

    @BeforeEach
    fun setup() {
        every { localeProvider.getCurrentLocale() } returns Locale.US
        every { resourceProvider.getString(any()) } answers { "Translated Category" }
        mapper = CategorySpendingUiMapper(localeProvider, resourceProvider)
    }

    @Test
    fun `mapExpenses maps positive expenses and sorts them by category amount descending`() {
        val expenses = listOf(
            Expense(
                groupAmount = 500L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.CAFE_BREAKFAST
            ),
            Expense(
                groupAmount = 1500L,
                category = ExpenseCategory.LODGING,
                subcategory = ExpenseSubcategory.HOTEL
            ),
            Expense(
                groupAmount = -200L, // Negative, should be filtered out
                category = ExpenseCategory.TRANSPORT,
                subcategory = ExpenseSubcategory.TRAIN
            ),
            Expense(
                groupAmount = 0L, // Zero, should be filtered out
                category = ExpenseCategory.TRANSPORT,
                subcategory = ExpenseSubcategory.BUS
            ),
            Expense(
                groupAmount = 1000L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.RESTAURANT
            )
        )

        // Total positive = 500 + 1500 + 1000 = 3000
        // FOOD = 1500, LODGING = 1500

        val result = mapper.mapExpenses(expenses, "USD")

        assertEquals(2, result.size)

        // FOOD category: RESTAURANT (1000) + CAFE_BREAKFAST (500)
        val foodCategory = result.first { it.categoryIcon == ExpenseCategory.FOOD.toIconVector() }
        assertEquals("Translated Category", foodCategory.categoryName)
        assertEquals("$15.00", foodCategory.formattedAmount)
        assertEquals(0.5f, foodCategory.progress)
        assertEquals(2, foodCategory.subcategories.size)

        val foodSub0 = foodCategory.subcategories[0]
        assertEquals("Translated Category", foodSub0.subcategoryName)
        assertEquals(ExpenseSubcategory.RESTAURANT.toIconVector(), foodSub0.subcategoryIcon)
        assertEquals("$10.00", foodSub0.formattedAmount)
        assertEquals(67, foodSub0.percentageOfCategory)
        assertEquals(1000L, foodSub0.rawAmountCents)

        val foodSub1 = foodCategory.subcategories[1]
        assertEquals("Translated Category", foodSub1.subcategoryName)
        assertEquals(ExpenseSubcategory.CAFE_BREAKFAST.toIconVector(), foodSub1.subcategoryIcon)
        assertEquals("$5.00", foodSub1.formattedAmount)
        assertEquals(33, foodSub1.percentageOfCategory)
        assertEquals(500L, foodSub1.rawAmountCents)

        // LODGING category: HOTEL (1500)
        val lodgingCategory = result.first { it.categoryIcon == ExpenseCategory.LODGING.toIconVector() }
        assertEquals("Translated Category", lodgingCategory.categoryName)
        assertEquals("$15.00", lodgingCategory.formattedAmount)
        assertEquals(0.5f, lodgingCategory.progress)
        assertEquals(1, lodgingCategory.subcategories.size)

        val lodgingSub = lodgingCategory.subcategories[0]
        assertEquals(ExpenseSubcategory.HOTEL.toIconVector(), lodgingSub.subcategoryIcon)
        assertEquals("$15.00", lodgingSub.formattedAmount)
        assertEquals(100, lodgingSub.percentageOfCategory)
        assertEquals(1500L, lodgingSub.rawAmountCents)
    }

    @Test
    fun `mapExpenses handles multiple subcategories with UNSPECIFIED correctly`() {
        val expenses = listOf(
            Expense(
                groupAmount = 800L,
                category = ExpenseCategory.TRANSPORT,
                subcategory = ExpenseSubcategory.TRAIN
            ),
            Expense(
                groupAmount = 200L,
                category = ExpenseCategory.TRANSPORT,
                subcategory = ExpenseSubcategory.UNSPECIFIED
            )
        )

        val result = mapper.mapExpenses(expenses, "EUR")

        assertEquals(1, result.size)
        val transportCategory = result[0]
        assertEquals(2, transportCategory.subcategories.size)

        assertEquals(ExpenseSubcategory.TRAIN.toIconVector(), transportCategory.subcategories[0].subcategoryIcon)
        assertEquals(80, transportCategory.subcategories[0].percentageOfCategory)
        assertEquals(800L, transportCategory.subcategories[0].rawAmountCents)

        assertEquals(ExpenseSubcategory.UNSPECIFIED.toIconVector(), transportCategory.subcategories[1].subcategoryIcon)
        assertEquals(20, transportCategory.subcategories[1].percentageOfCategory)
        assertEquals(200L, transportCategory.subcategories[1].rawAmountCents)
    }

    @Test
    fun `mapExpenses returns empty list when no positive expenses`() {
        val expenses = listOf(
            Expense(
                groupAmount = -200L,
                category = ExpenseCategory.TRANSPORT,
                subcategory = ExpenseSubcategory.TRAIN
            ),
            Expense(
                groupAmount = 0L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.RESTAURANT
            )
        )

        val result = mapper.mapExpenses(expenses, "USD")

        assertTrue(result.isEmpty())
    }

    private fun mockExtrasResourceStrings() {
        every { resourceProvider.getString(R.string.balances_total_extras) } returns "Extras"
        every { resourceProvider.getString(R.string.balances_extras_add_on_fee_plural) } returns "Fees"
        every { resourceProvider.getString(R.string.balances_extras_add_on_surcharge_plural) } returns "Surcharges"
        every { resourceProvider.getString(R.string.balances_extras_add_on_tip_plural) } returns "Tips"
    }

    private fun createSampleExpensesWithAddOns(): List<Expense> = listOf(
        Expense(
            groupAmount = 1000L,
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.RESTAURANT,
            addOns = listOf(
                AddOn(type = AddOnType.TIP, groupAmountCents = 200L)
            )
        ),
        Expense(
            groupAmount = 2000L,
            category = ExpenseCategory.LODGING,
            subcategory = ExpenseSubcategory.HOTEL,
            addOns = listOf(
                AddOn(type = AddOnType.FEE, groupAmountCents = 300L)
            )
        )
    )

    private fun createSampleWithdrawalsWithAddOns(): List<CashWithdrawal> = listOf(
        CashWithdrawal(
            amountWithdrawn = 5000L,
            addOns = listOf(
                AddOn(type = AddOnType.FEE, groupAmountCents = 150L),
                AddOn(type = AddOnType.SURCHARGE, groupAmountCents = 50L)
            )
        )
    )

    @Test
    fun `mapExpenses includes positive non-discount add-ons from expenses and withdrawals in Extras category`() {
        mockExtrasResourceStrings()

        val expenses = createSampleExpensesWithAddOns()
        val withdrawals = createSampleWithdrawalsWithAddOns()

        // Base expenses: Lodging = 2000L, Food = 1000L
        // Extras: Fee (300 + 150 = 450L), Tip (200L), Surcharge (50L) -> total extras = 700L
        // Global total = 2000 + 1000 + 700 = 3700L

        val result = mapper.mapExpenses(expenses, "USD", withdrawals)

        assertEquals(3, result.size)

        // Slices sorted descending: Lodging (2000), Food (1000), Extras (700)
        val lodging = result[0]
        assertEquals("Translated Category", lodging.categoryName)
        assertEquals(ExpenseCategory.LODGING.toIconVector(), lodging.categoryIcon)
        assertEquals("$20.00", lodging.formattedAmount)
        assertEquals(2000f / 3700f, lodging.progress)

        val food = result[1]
        assertEquals("Translated Category", food.categoryName)
        assertEquals(ExpenseCategory.FOOD.toIconVector(), food.categoryIcon)
        assertEquals("$10.00", food.formattedAmount)
        assertEquals(1000f / 3700f, food.progress)

        val extras = result[2]
        assertEquals("Extras", extras.categoryName)
        assertEquals(TablerIcons.Outline.ReceiptEuro, extras.categoryIcon)
        assertEquals("$7.00", extras.formattedAmount)
        assertEquals(700f / 3700f, extras.progress)
        assertEquals(3, extras.subcategories.size)

        // Extras subcategories sorted descending: Fee (450), Tip (200), Surcharge (50)
        val feeSub = extras.subcategories[0]
        assertEquals("Fees", feeSub.subcategoryName)
        assertEquals(AddOnType.FEE.toIconVector(), feeSub.subcategoryIcon)
        assertEquals("$4.50", feeSub.formattedAmount)
        assertEquals(64, feeSub.percentageOfCategory) // 450 / 700 * 100 = 64.28 -> 64
        assertEquals(450L, feeSub.rawAmountCents)

        val tipSub = extras.subcategories[1]
        assertEquals("Tips", tipSub.subcategoryName)
        assertEquals(AddOnType.TIP.toIconVector(), tipSub.subcategoryIcon)
        assertEquals("$2.00", tipSub.formattedAmount)
        assertEquals(29, tipSub.percentageOfCategory) // 200 / 700 * 100 = 28.57 -> 29
        assertEquals(200L, tipSub.rawAmountCents)

        val surchargeSub = extras.subcategories[2]
        assertEquals("Surcharges", surchargeSub.subcategoryName)
        assertEquals(AddOnType.SURCHARGE.toIconVector(), surchargeSub.subcategoryIcon)
        assertEquals("$0.50", surchargeSub.formattedAmount)
        assertEquals(7, surchargeSub.percentageOfCategory) // 50 / 700 * 100 = 7.14 -> 7
        assertEquals(50L, surchargeSub.rawAmountCents)
    }

    @Test
    fun `mapExpenses excludes DISCOUNT add-ons from Extras category`() {
        every { resourceProvider.getString(R.string.balances_total_extras) } returns "Extras"
        every { resourceProvider.getString(R.string.balances_extras_add_on_tip_plural) } returns "Tips"

        val expenses = listOf(
            Expense(
                groupAmount = 1000L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.RESTAURANT,
                addOns = listOf(
                    AddOn(type = AddOnType.TIP, groupAmountCents = 200L),
                    AddOn(type = AddOnType.DISCOUNT, groupAmountCents = 300L)
                )
            )
        )

        val result = mapper.mapExpenses(expenses, "USD")

        assertEquals(2, result.size)
        val extras = result.first { it.categoryName == "Extras" }
        assertEquals("$2.00", extras.formattedAmount)
        assertEquals(1, extras.subcategories.size)
        assertEquals("Tips", extras.subcategories[0].subcategoryName)
        assertEquals(200L, extras.subcategories[0].rawAmountCents)
    }

    @Test
    fun `mapExpenses excludes CANCELLED expenses and future SCHEDULED expenses`() {
        val today = LocalDate.of(2026, 11, 5)
        val expenses = listOf(
            Expense(
                groupAmount = 1000L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.RESTAURANT
            ),
            Expense(
                groupAmount = 5000L,
                category = ExpenseCategory.TRANSPORT,
                subcategory = ExpenseSubcategory.TRAIN,
                paymentStatus = PaymentStatus.CANCELLED,
                addOns = listOf(AddOn(type = AddOnType.TIP, groupAmountCents = 500L))
            ),
            Expense(
                groupAmount = 3000L,
                category = ExpenseCategory.LODGING,
                subcategory = ExpenseSubcategory.HOTEL,
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = today.plusDays(1).atStartOfDay()
            )
        )

        val result = mapper.mapExpenses(expenses = expenses, groupCurrency = "USD", today = today)

        assertEquals(1, result.size)
        assertEquals(1000L, result[0].subcategories[0].rawAmountCents)
        assertEquals(1.0f, result[0].progress)
    }

    @Test
    fun `mapExpenses omits Extras category when no positive non-discount extras exist`() {
        val expenses = listOf(
            Expense(
                groupAmount = 1000L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.RESTAURANT,
                addOns = emptyList()
            )
        )
        val withdrawals = listOf(
            CashWithdrawal(
                amountWithdrawn = 2000L,
                addOns = emptyList()
            )
        )

        val result = mapper.mapExpenses(expenses, "USD", withdrawals)

        assertEquals(1, result.size)
        assertEquals(ExpenseCategory.FOOD.toIconVector(), result[0].categoryIcon)
    }

    @Test
    fun `calculateTotalCents computes grand total of active base expenses and non-discount extras`() {
        val today = LocalDate.of(2026, 11, 5)
        val expenses = listOf(
            Expense(
                groupAmount = 1000L,
                addOns = listOf(
                    AddOn(type = AddOnType.TIP, groupAmountCents = 200L),
                    AddOn(type = AddOnType.DISCOUNT, groupAmountCents = 300L) // Excluded
                )
            ),
            Expense(
                groupAmount = 2000L,
                addOns = listOf(AddOn(type = AddOnType.FEE, groupAmountCents = 300L))
            ),
            Expense(
                groupAmount = 5000L,
                paymentStatus = PaymentStatus.CANCELLED, // Excluded
                addOns = listOf(AddOn(type = AddOnType.FEE, groupAmountCents = 500L)) // Excluded
            ),
            Expense(
                groupAmount = 4000L,
                paymentStatus = PaymentStatus.SCHEDULED, // Excluded
                dueDate = today.plusDays(2).atStartOfDay(),
                addOns = listOf(AddOn(type = AddOnType.FEE, groupAmountCents = 400L)) // Excluded
            )
        )
        val withdrawals = listOf(
            CashWithdrawal(
                amountWithdrawn = 5000L,
                addOns = listOf(
                    AddOn(type = AddOnType.FEE, groupAmountCents = 150L),
                    AddOn(type = AddOnType.SURCHARGE, groupAmountCents = 50L)
                )
            )
        )

        // Base = 1000 + 2000 = 3000L
        // Extras = 200 (tip) + 300 (fee) + 150 (fee) + 50 (surcharge) = 700L
        // Grand total = 3700L
        val total = mapper.calculateTotalCents(expenses, withdrawals, today)

        assertEquals(3700L, total)
    }

    @Test
    fun `formatTotalAmount formats correctly when total is positive`() {
        val result = mapper.formatTotalAmount(2500L, "USD")
        assertEquals("$25.00", result)
    }

    @Test
    fun `formatTotalAmount returns empty string when total is zero or negative`() {
        assertEquals("", mapper.formatTotalAmount(0L, "USD"))
        assertEquals("", mapper.formatTotalAmount(-100L, "USD"))
    }
}
