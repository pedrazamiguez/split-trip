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
import es.pedrazamiguez.splittrip.domain.service.impl.RemainderDistributionServiceImpl
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
        mapper = CategorySpendingUiMapper(
            localeProvider = localeProvider,
            resourceProvider = resourceProvider,
            remainderDistributionService = RemainderDistributionServiceImpl()
        )
    }

    @Test
    fun `mapExpenses maps positive expenses and sorts them by category amount descending`() {
        val expenses = listOf(
            createExpense(500L, ExpenseCategory.FOOD, ExpenseSubcategory.CAFE_BREAKFAST),
            createExpense(1500L, ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL),
            createExpense(-200L, ExpenseCategory.TRANSPORT, ExpenseSubcategory.TRAIN),
            createExpense(0L, ExpenseCategory.TRANSPORT, ExpenseSubcategory.BUS),
            createExpense(1000L, ExpenseCategory.FOOD, ExpenseSubcategory.RESTAURANT)
        )

        // Total positive = 500 + 1500 + 1000 = 3000
        // FOOD = 1500, LODGING = 1500

        val result = mapper.mapExpenses(expenses, "USD")

        assertEquals(2, result.size)

        // FOOD category: RESTAURANT (1000) + CAFE_BREAKFAST (500)
        val foodCategory = result.first { it.categoryIcon == ExpenseCategory.FOOD.toIconVector() }
        assertEquals("Translated Category", foodCategory.categoryName)
        assertEquals("$15.00", foodCategory.formattedAmount)
        assertEquals("50%", foodCategory.formattedPercentage)
        assertEquals(0.5f, foodCategory.progress)
        assertEquals(2, foodCategory.subcategories.size)

        val foodSub0 = foodCategory.subcategories[0]
        assertEquals("Translated Category", foodSub0.subcategoryName)
        assertEquals(ExpenseSubcategory.RESTAURANT.toIconVector(), foodSub0.subcategoryIcon)
        assertEquals("$10.00", foodSub0.formattedAmount)
        assertEquals("67%", foodSub0.formattedPercentage)
        assertEquals(1000L, foodSub0.rawAmountCents)

        val foodSub1 = foodCategory.subcategories[1]
        assertEquals("Translated Category", foodSub1.subcategoryName)
        assertEquals(ExpenseSubcategory.CAFE_BREAKFAST.toIconVector(), foodSub1.subcategoryIcon)
        assertEquals("$5.00", foodSub1.formattedAmount)
        assertEquals("33%", foodSub1.formattedPercentage)
        assertEquals(500L, foodSub1.rawAmountCents)

        // LODGING category: HOTEL (1500)
        val lodgingCategory = result.first { it.categoryIcon == ExpenseCategory.LODGING.toIconVector() }
        assertEquals("Translated Category", lodgingCategory.categoryName)
        assertEquals("$15.00", lodgingCategory.formattedAmount)
        assertEquals("50%", lodgingCategory.formattedPercentage)
        assertEquals(0.5f, lodgingCategory.progress)
        assertEquals(1, lodgingCategory.subcategories.size)

        val lodgingSub = lodgingCategory.subcategories[0]
        assertEquals(ExpenseSubcategory.HOTEL.toIconVector(), lodgingSub.subcategoryIcon)
        assertEquals("$15.00", lodgingSub.formattedAmount)
        assertEquals("100%", lodgingSub.formattedPercentage)
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
        assertEquals("100%", transportCategory.formattedPercentage)
        assertEquals(2, transportCategory.subcategories.size)

        assertEquals(ExpenseSubcategory.TRAIN.toIconVector(), transportCategory.subcategories[0].subcategoryIcon)
        assertEquals("80%", transportCategory.subcategories[0].formattedPercentage)
        assertEquals(800L, transportCategory.subcategories[0].rawAmountCents)

        assertEquals(ExpenseSubcategory.UNSPECIFIED.toIconVector(), transportCategory.subcategories[1].subcategoryIcon)
        assertEquals("20%", transportCategory.subcategories[1].formattedPercentage)
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

    private fun createExpense(
        amount: Long,
        category: ExpenseCategory,
        subcategory: ExpenseSubcategory
    ): Expense = Expense(groupAmount = amount, category = category, subcategory = subcategory)

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
        assertEquals("54%", lodging.formattedPercentage)
        assertEquals(2000f / 3700f, lodging.progress)

        val food = result[1]
        assertEquals("Translated Category", food.categoryName)
        assertEquals(ExpenseCategory.FOOD.toIconVector(), food.categoryIcon)
        assertEquals("$10.00", food.formattedAmount)
        assertEquals("27%", food.formattedPercentage)
        assertEquals(1000f / 3700f, food.progress)

        val extras = result[2]
        assertEquals("Extras", extras.categoryName)
        assertEquals(TablerIcons.Outline.ReceiptEuro, extras.categoryIcon)
        assertEquals("$7.00", extras.formattedAmount)
        assertEquals("19%", extras.formattedPercentage)
        assertEquals(700f / 3700f, extras.progress)
        assertEquals(3, extras.subcategories.size)

        // Extras subcategories sorted descending: Fee (450), Tip (200), Surcharge (50)
        val feeSub = extras.subcategories[0]
        assertEquals("Fees", feeSub.subcategoryName)
        assertEquals(AddOnType.FEE.toIconVector(), feeSub.subcategoryIcon)
        assertEquals("$4.50", feeSub.formattedAmount)
        assertEquals("64%", feeSub.formattedPercentage) // 450 / 700 * 100 = 64.28 -> 64
        assertEquals(450L, feeSub.rawAmountCents)

        val tipSub = extras.subcategories[1]
        assertEquals("Tips", tipSub.subcategoryName)
        assertEquals(AddOnType.TIP.toIconVector(), tipSub.subcategoryIcon)
        assertEquals("$2.00", tipSub.formattedAmount)
        assertEquals("29%", tipSub.formattedPercentage) // 200 / 700 * 100 = 28.57 -> 29
        assertEquals(200L, tipSub.rawAmountCents)

        val surchargeSub = extras.subcategories[2]
        assertEquals("Surcharges", surchargeSub.subcategoryName)
        assertEquals(AddOnType.SURCHARGE.toIconVector(), surchargeSub.subcategoryIcon)
        assertEquals("$0.50", surchargeSub.formattedAmount)
        assertEquals("7%", surchargeSub.formattedPercentage) // 50 / 700 * 100 = 7.14 -> 7
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

    @Test
    fun `mapExpenses formats category with amount less than 0_5 percent of global total as less than 1 percent`() {
        val expenses = listOf(
            Expense(
                groupAmount = 99900L,
                category = ExpenseCategory.LODGING,
                subcategory = ExpenseSubcategory.HOTEL
            ),
            Expense(
                groupAmount = 100L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.RESTAURANT
            )
        )

        val result = mapper.mapExpenses(expenses, "USD")

        assertEquals(2, result.size)
        val lodging = result[0]
        assertEquals(ExpenseCategory.LODGING.toIconVector(), lodging.categoryIcon)
        assertEquals("100%", lodging.formattedPercentage)

        val food = result[1]
        assertEquals(ExpenseCategory.FOOD.toIconVector(), food.categoryIcon)
        assertEquals("<1%", food.formattedPercentage)
    }

    @Test
    fun `mapExpenses formats subcategory with amount less than 0_5 percent of category total as less than 1 percent`() {
        val expenses = listOf(
            Expense(
                groupAmount = 100000L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.RESTAURANT
            ),
            Expense(
                groupAmount = 400L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.CAFE_BREAKFAST
            )
        )

        val result = mapper.mapExpenses(expenses, "USD")

        assertEquals(1, result.size)
        val foodCategory = result[0]
        assertEquals(2, foodCategory.subcategories.size)

        val sub0 = foodCategory.subcategories[0]
        assertEquals(ExpenseSubcategory.RESTAURANT.toIconVector(), sub0.subcategoryIcon)
        assertEquals("100%", sub0.formattedPercentage)

        val sub1 = foodCategory.subcategories[1]
        assertEquals(ExpenseSubcategory.CAFE_BREAKFAST.toIconVector(), sub1.subcategoryIcon)
        assertEquals("<1%", sub1.formattedPercentage)
    }

    @Test
    fun `mapExpenses formats subcategory below 1 percent as less than 1 percent and larger as whole percentage`() {
        val expenses = listOf(
            Expense(
                groupAmount = 98500L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.RESTAURANT
            ),
            Expense(
                groupAmount = 1000L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET
            ),
            Expense(
                groupAmount = 500L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.CAFE_BREAKFAST
            )
        )

        val result = mapper.mapExpenses(expenses, "USD")

        assertEquals(1, result.size)
        val foodCategory = result[0]
        assertEquals(3, foodCategory.subcategories.size)

        val sub0 = foodCategory.subcategories[0]
        assertEquals(ExpenseSubcategory.RESTAURANT.toIconVector(), sub0.subcategoryIcon)
        assertEquals("99%", sub0.formattedPercentage)

        val sub1 = foodCategory.subcategories[1]
        assertEquals(ExpenseSubcategory.GROCERIES_SUPERMARKET.toIconVector(), sub1.subcategoryIcon)
        assertEquals("1%", sub1.formattedPercentage)

        val sub2 = foodCategory.subcategories[2]
        assertEquals(ExpenseSubcategory.CAFE_BREAKFAST.toIconVector(), sub2.subcategoryIcon)
        assertEquals("<1%", sub2.formattedPercentage)
    }

    @Test
    fun `mapExpenses correctly solves reported Compras scenario with integers summing to 100`() {
        val expenses = listOf(
            Expense(
                groupAmount = 16855L,
                category = ExpenseCategory.SHOPPING,
                subcategory = ExpenseSubcategory.SOUVENIRS_GIFTS
            ),
            Expense(
                groupAmount = 5532L,
                category = ExpenseCategory.SHOPPING,
                subcategory = ExpenseSubcategory.CLOTHING
            ),
            Expense(
                groupAmount = 5460L,
                category = ExpenseCategory.SHOPPING,
                subcategory = ExpenseSubcategory.PHARMACY_HEALTH
            ),
            Expense(
                groupAmount = 1343L,
                category = ExpenseCategory.SHOPPING,
                subcategory = ExpenseSubcategory.ELECTRONICS
            ),
            Expense(
                groupAmount = 246L,
                category = ExpenseCategory.SHOPPING,
                subcategory = ExpenseSubcategory.CONVENIENCE_STORE
            )
        )

        val result = mapper.mapExpenses(expenses, "EUR")

        assertEquals(1, result.size)
        val shopping = result[0]
        assertEquals(5, shopping.subcategories.size)
        assertEquals("57%", shopping.subcategories[0].formattedPercentage)
        assertEquals("19%", shopping.subcategories[1].formattedPercentage)
        assertEquals("19%", shopping.subcategories[2].formattedPercentage)
        assertEquals("5%", shopping.subcategories[3].formattedPercentage)
        assertEquals("<1%", shopping.subcategories[4].formattedPercentage)
    }

    @Test
    fun `mapExpenses mitigates cumulative rounding drift across categories`() {
        val expenses101 = listOf(
            createExpense(336L, ExpenseCategory.FOOD, ExpenseSubcategory.RESTAURANT),
            createExpense(336L, ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL),
            createExpense(328L, ExpenseCategory.TRANSPORT, ExpenseSubcategory.TRAIN)
        )
        val result101 = mapper.mapExpenses(expenses101, "USD")
        assertEquals(3, result101.size)
        assertEquals("34%", result101[0].formattedPercentage)
        assertEquals("33%", result101[1].formattedPercentage)
        assertEquals("33%", result101[2].formattedPercentage)

        val expenses99 = listOf(
            createExpense(3334L, ExpenseCategory.FOOD, ExpenseSubcategory.RESTAURANT),
            createExpense(3333L, ExpenseCategory.LODGING, ExpenseSubcategory.HOTEL),
            createExpense(3333L, ExpenseCategory.TRANSPORT, ExpenseSubcategory.TRAIN)
        )
        val result99 = mapper.mapExpenses(expenses99, "USD")
        assertEquals(3, result99.size)
        assertEquals("34%", result99[0].formattedPercentage)
        assertEquals("33%", result99[1].formattedPercentage)
        assertEquals("33%", result99[2].formattedPercentage)
    }

    @Test
    fun `mapExpenses formats extras category and subcategory with less than 0_5 percent as less than 1 percent`() {
        mockExtrasResourceStrings()

        val expenses = listOf(
            Expense(
                groupAmount = 100000L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.RESTAURANT,
                addOns = listOf(
                    AddOn(type = AddOnType.FEE, groupAmountCents = 400L),
                    AddOn(type = AddOnType.TIP, groupAmountCents = 1L)
                )
            )
        )

        val result = mapper.mapExpenses(expenses, "USD")

        assertEquals(2, result.size)
        val extras = result[1]
        assertEquals("Extras", extras.categoryName)
        assertEquals("<1%", extras.formattedPercentage)
        assertEquals(2, extras.subcategories.size)

        val feeSub = extras.subcategories[0]
        assertEquals(AddOnType.FEE.toIconVector(), feeSub.subcategoryIcon)
        assertEquals("100%", feeSub.formattedPercentage)

        val tipSub = extras.subcategories[1]
        assertEquals(AddOnType.TIP.toIconVector(), tipSub.subcategoryIcon)
        assertEquals("<1%", tipSub.formattedPercentage)
    }
}
