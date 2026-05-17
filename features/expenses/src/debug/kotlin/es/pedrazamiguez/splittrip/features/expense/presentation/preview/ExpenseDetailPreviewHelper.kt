package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.preview.MappedPreview
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpenseDetailUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ScheduledBadgeUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel
import java.math.BigDecimal
import java.time.LocalDateTime

// ── Detail-specific domain fixtures (Scenarios A–D from issue #1077) ────────

/** Scenario A — Vanilla: same currency, no add-ons, flat split. */
val PREVIEW_EXPENSE_DETAIL_VANILLA = Expense(
    id = "exp-detail-vanilla",
    groupId = "group-1",
    title = "Coffee at the airport",
    sourceAmount = 1500L,
    sourceCurrency = "EUR",
    groupAmount = 1500L,
    groupCurrency = "EUR",
    category = ExpenseCategory.FOOD,
    paymentMethod = PaymentMethod.CREDIT_CARD,
    paymentStatus = PaymentStatus.FINISHED,
    splitType = SplitType.EQUAL,
    createdBy = "user-antonio",
    splits = listOf(
        ExpenseSplit(userId = "user-antonio", amountCents = 750L),
        ExpenseSplit(userId = "user-maria", amountCents = 750L)
    ),
    createdAt = LocalDateTime.of(2026, 4, 1, 9, 15)
)

/** Scenario B — INCLUDED tip + subunit split (two-level accordion). */
val PREVIEW_EXPENSE_DETAIL_INCLUDED_TIP = Expense(
    id = "exp-detail-included",
    groupId = "group-1",
    title = "Dinner with friends",
    sourceAmount = 5000L,
    sourceCurrency = "EUR",
    // groupAmount is the decomposed base — 5500 entered − 500 INCLUDED tip = 5000.
    groupAmount = 5000L,
    groupCurrency = "EUR",
    category = ExpenseCategory.FOOD,
    paymentMethod = PaymentMethod.CREDIT_CARD,
    paymentStatus = PaymentStatus.FINISHED,
    splitType = SplitType.PERCENT,
    createdBy = "user-antonio",
    addOns = listOf(
        AddOn(
            type = AddOnType.TIP,
            mode = AddOnMode.INCLUDED,
            valueType = AddOnValueType.EXACT,
            amountCents = 500L,
            currency = "EUR",
            groupAmountCents = 500L
        )
    ),
    splits = listOf(
        ExpenseSplit(
            userId = "user-antonio",
            amountCents = 1500L,
            percentage = BigDecimal("30"),
            subunitId = "subunit-cantalobos"
        ),
        ExpenseSplit(
            userId = "user-andres",
            amountCents = 1500L,
            percentage = BigDecimal("30"),
            subunitId = "subunit-cantalobos"
        ),
        ExpenseSplit(userId = "user-maria", amountCents = 2000L, percentage = BigDecimal("40"))
    ),
    createdAt = LocalDateTime.of(2026, 4, 2, 21, 30)
)

/** Scenario C — Cash FIFO across two ATM tranches with locked rates. */
val PREVIEW_EXPENSE_DETAIL_CASH_FIFO = Expense(
    id = "exp-detail-cash",
    groupId = "group-1",
    title = "Boat trip in Phuket",
    sourceAmount = 80000L,
    sourceCurrency = "THB",
    groupAmount = 2160L,
    groupCurrency = "EUR",
    exchangeRate = BigDecimal("37.037"),
    category = ExpenseCategory.ACTIVITIES,
    paymentMethod = PaymentMethod.CASH,
    paymentStatus = PaymentStatus.FINISHED,
    splitType = SplitType.EQUAL,
    createdBy = "user-antonio",
    cashTranches = listOf(
        CashTranche(withdrawalId = "wd-bangkok", amountConsumed = 50000L),
        CashTranche(withdrawalId = "wd-phuket", amountConsumed = 30000L)
    ),
    splits = listOf(
        ExpenseSplit(userId = "user-antonio", amountCents = 40000L),
        ExpenseSplit(userId = "user-maria", amountCents = 40000L)
    ),
    createdAt = LocalDateTime.of(2026, 4, 5, 14, 0)
)

/** Scenario D — Cross-currency ON_TOP fee and DISCOUNT add-ons. */
val PREVIEW_EXPENSE_DETAIL_FOREIGN_ADDONS = Expense(
    id = "exp-detail-foreign-addons",
    groupId = "group-1",
    title = "Eurostar tickets",
    sourceAmount = 12000L,
    sourceCurrency = "EUR",
    groupAmount = 12000L,
    groupCurrency = "EUR",
    category = ExpenseCategory.TRANSPORT,
    paymentMethod = PaymentMethod.CREDIT_CARD,
    paymentStatus = PaymentStatus.FINISHED,
    splitType = SplitType.EQUAL,
    createdBy = "user-antonio",
    addOns = listOf(
        AddOn(
            type = AddOnType.FEE,
            mode = AddOnMode.ON_TOP,
            valueType = AddOnValueType.EXACT,
            amountCents = 500L,
            currency = "GBP",
            exchangeRate = BigDecimal("0.83"),
            groupAmountCents = 600L,
            description = "Bank fee"
        ),
        AddOn(
            type = AddOnType.DISCOUNT,
            mode = AddOnMode.ON_TOP,
            valueType = AddOnValueType.EXACT,
            amountCents = 300L,
            currency = "EUR",
            groupAmountCents = 300L,
            description = "Promo code"
        )
    ),
    splits = listOf(
        ExpenseSplit(userId = "user-antonio", amountCents = 6000L),
        ExpenseSplit(userId = "user-maria", amountCents = 6000L),
        ExpenseSplit(userId = "user-andres", amountCents = 0L, isExcluded = true)
    ),
    createdAt = LocalDateTime.of(2026, 4, 6, 11, 20)
)

/**
 * Scenario E — Two-level mixed strategy:
 * Level 1 = EQUAL between entities; Level 2 = PERCENT within one subunit.
 *
 * Demonstrates per-subunit [splitTypeText] chip in the expense detail split card.
 */
val PREVIEW_EXPENSE_DETAIL_MIXED_LEVELS = Expense(
    id = "exp-detail-mixed",
    groupId = "group-1",
    title = "Beach hotel split",
    sourceAmount = 100000L, // 1000.00 CNY
    sourceCurrency = "CNY",
    groupAmount = 12630L, // 126.30 EUR (@ 0.126297)
    groupCurrency = "EUR",
    exchangeRate = BigDecimal("0.126297"),
    category = ExpenseCategory.LODGING,
    paymentMethod = PaymentMethod.CREDIT_CARD,
    paymentStatus = PaymentStatus.FINISHED,
    splitType = SplitType.EQUAL,
    createdBy = "user-antonio",
    splits = listOf(
        // Subunit "Cantalobos" — Level 2 PERCENT (85 % Antonio / 15 % Andrés)
        // Total for subunit: 53000 CNY (669.77 EUR) split 85/15
        ExpenseSplit(
            userId = "user-antonio",
            amountCents = 45050L, // 450.50 CNY (85% of 530 CNY subunit share)
            percentage = BigDecimal("85"),
            subunitId = "subunit-cantalobos",
            splitType = SplitType.PERCENT
        ),
        ExpenseSplit(
            userId = "user-andres",
            amountCents = 7950L, // 79.50 CNY (15% of 530 CNY subunit share)
            percentage = BigDecimal("15"),
            subunitId = "subunit-cantalobos",
            splitType = SplitType.PERCENT
        ),
        // Solo member — Level 1 EQUAL (47000 CNY = 593.60 EUR)
        ExpenseSplit(userId = "user-maria", amountCents = 47000L)
    ),
    createdAt = LocalDateTime.of(2026, 4, 8, 15, 0)
)

val PREVIEW_DETAIL_MEMBER_PROFILES = mapOf(
    "user-antonio" to User(userId = "user-antonio", email = "antonio@test.com", displayName = "Antonio"),
    "user-maria" to User(userId = "user-maria", email = "maria@test.com", displayName = "Mara"),
    "user-andres" to User(userId = "user-andres", email = "andres@test.com", displayName = "Andrés")
)

val PREVIEW_DETAIL_SUBUNIT_NAMES = mapOf("subunit-cantalobos" to "Cantalobos")

val PREVIEW_DETAIL_WITHDRAWAL_LOOKUP = mapOf(
    "wd-bangkok" to CashWithdrawal(
        id = "wd-bangkok",
        groupId = "group-1",
        title = "ATM Bangkok",
        amountWithdrawn = 100000L,
        remainingAmount = 50000L,
        currency = "THB",
        deductedBaseAmount = 2700L,
        exchangeRate = BigDecimal("37.037"),
        withdrawalScope = PayerType.GROUP,
        createdAt = LocalDateTime.of(2026, 4, 4, 10, 0)
    ),
    "wd-phuket" to CashWithdrawal(
        id = "wd-phuket",
        groupId = "group-1",
        title = "ATM Phuket",
        amountWithdrawn = 60000L,
        remainingAmount = 30000L,
        currency = "THB",
        deductedBaseAmount = 1620L,
        exchangeRate = BigDecimal("37.037"),
        withdrawalScope = PayerType.GROUP,
        createdAt = LocalDateTime.of(2026, 4, 5, 9, 0)
    )
)

@Composable
fun ExpenseDetailPreviewHelper(
    domainExpense: Expense = PREVIEW_EXPENSE_DETAIL_VANILLA,
    memberProfiles: Map<String, User> = PREVIEW_DETAIL_MEMBER_PROFILES,
    currentUserId: String? = "user-antonio",
    withdrawalLookup: Map<String, CashWithdrawal> = emptyMap(),
    subunitNameLookup: Map<String, String> = emptyMap(),
    content: @Composable (ExpenseDetailUiModel) -> Unit
) {
    MappedPreview(
        domain = domainExpense,
        mapper = { localeProvider, resourceProvider ->
            // Mapper is stateless — both domain services have empty constructors and
            // safe defaults, so we can instantiate them inline for previews.
            val formattingHelper = FormattingHelper(localeProvider)
            ExpenseDetailUiMapper(
                formattingHelper = formattingHelper,
                resourceProvider = resourceProvider,
                expenseCalculatorService = ExpenseCalculatorService(),
                addOnCalculationService = AddOnCalculationService(),
                scheduledBadgeUiMapper = ScheduledBadgeUiMapper(
                    formattingHelper = formattingHelper,
                    resourceProvider = resourceProvider
                )
            )
        },
        transform = { mapper, domain ->
            mapper.map(
                expense = domain,
                memberProfiles = memberProfiles,
                currentUserId = currentUserId,
                withdrawalLookup = withdrawalLookup,
                subunitNameLookup = subunitNameLookup
            )
        },
        content = content
    )
}
