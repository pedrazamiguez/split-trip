package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.CurrencyAmount
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import java.math.BigDecimal
import java.time.LocalDateTime

const val PREVIEW_GROUP_NAME = "Thai 2.0"

// ── Member Profiles (for resolveDisplayName / resolveCreatedByDisplayName) ──

val PREVIEW_MEMBER_PROFILES: Map<String, User> = mapOf(
    "user-1" to User(userId = "user-1", email = "antonio@example.com", displayName = "Antonio"),
    "user-2" to User(userId = "user-2", email = "pedro@example.com", displayName = "Pedro"),
    "Antonio" to User(userId = "Antonio", email = "antonio@example.com", displayName = "Antonio"),
    "Maria" to User(userId = "Maria", email = "maria@example.com", displayName = "Maria"),
    "Pedro" to User(userId = "Pedro", email = "pedro@example.com", displayName = "Pedro"),
    "Laura" to User(userId = "Laura", email = "laura@example.com", displayName = "Laura")
)

val PREVIEW_POCKET_BALANCE = GroupPocketBalance(
    totalContributions = 120000L,
    totalExpenses = 16545L,
    virtualBalance = 103455L,
    currency = "EUR",
    cashBalances = mapOf("THB" to 1000000L),
    cashEquivalents = mapOf("THB" to 27000L),
    totalExtras = 1206L
)

val PREVIEW_POCKET_BALANCE_EMPTY = GroupPocketBalance(
    totalContributions = 0L,
    totalExpenses = 0L,
    virtualBalance = 0L,
    currency = "EUR"
)

// ── Cash Withdrawals ────────────────────────────────────────────────────────

val PREVIEW_CASH_WITHDRAWAL_GROUP = CashWithdrawal(
    id = "cw1",
    groupId = "group-1",
    withdrawnBy = "user-1",
    withdrawalScope = PayerType.GROUP,
    amountWithdrawn = 1000000L,
    remainingAmount = 770000L,
    currency = "THB",
    deductedBaseAmount = 27000L,
    exchangeRate = BigDecimal("37.037"),
    createdAt = LocalDateTime.of(2026, 1, 16, 14, 0)
)

val PREVIEW_CASH_WITHDRAWAL_SUBUNIT = CashWithdrawal(
    id = "cw2",
    groupId = "group-1",
    withdrawnBy = "user-1",
    withdrawalScope = PayerType.SUBUNIT,
    subunitId = "subunit-1",
    amountWithdrawn = 500000L,
    remainingAmount = 500000L,
    currency = "THB",
    deductedBaseAmount = 13500L,
    exchangeRate = BigDecimal("37.037"),
    createdAt = LocalDateTime.of(2026, 1, 17, 10, 0)
)

val PREVIEW_CASH_WITHDRAWAL_PERSONAL = CashWithdrawal(
    id = "cw3",
    groupId = "group-1",
    withdrawnBy = "user-1",
    withdrawalScope = PayerType.USER,
    amountWithdrawn = 200000L,
    remainingAmount = 200000L,
    currency = "THB",
    deductedBaseAmount = 5400L,
    exchangeRate = BigDecimal("37.037"),
    createdAt = LocalDateTime.of(2026, 1, 18, 9, 0)
)

val PREVIEW_CASH_WITHDRAWAL_IMPERSONATED = CashWithdrawal(
    id = "cw4",
    groupId = "group-1",
    withdrawnBy = "user-1",
    createdBy = "user-2",
    withdrawalScope = PayerType.GROUP,
    amountWithdrawn = 300000L,
    remainingAmount = 300000L,
    currency = "THB",
    deductedBaseAmount = 8100L,
    exchangeRate = BigDecimal("37.037"),
    createdAt = LocalDateTime.of(2026, 1, 19, 11, 0)
)

// ── Subunits ─────────────────────────────────────────────────────────────────

/**
 * Subunit fixture that matches [PREVIEW_CASH_WITHDRAWAL_SUBUNIT]'s subunitId = "subunit-1".
 * Member shares are equal halves between "user-1" and "user-2".
 */
val PREVIEW_SUBUNIT_FOR_BREAKDOWN = Subunit(
    id = "subunit-1",
    name = "Couple Hotel Room",
    memberShares = mapOf("user-1" to BigDecimal("0.5"), "user-2" to BigDecimal("0.5"))
)

/**
 * Member balance fixture for cash-breakdown previews.
 * userId = "user-1" matches [PREVIEW_CASH_WITHDRAWAL_GROUP], [PREVIEW_CASH_WITHDRAWAL_SUBUNIT],
 * and [PREVIEW_CASH_WITHDRAWAL_PERSONAL] so all three scopes populate the breakdown sheet.
 */
val PREVIEW_MEMBER_BALANCE_FOR_BREAKDOWN = MemberBalance(
    userId = "user-1",
    contributed = 50000L,
    withdrawn = 1700000L,
    cashSpent = 800000L,
    nonCashSpent = 10000L,
    totalSpent = 810000L,
    pocketBalance = 15000L,
    cashInHand = 900000L,
    cashInHandByCurrency = listOf(CurrencyAmount("THB", 900000L, 24300L))
)

// ── Contributions ───────────────────────────────────────────────────────────

val PREVIEW_CONTRIBUTION_GROUP = Contribution(
    id = "c1",
    groupId = "group-1",
    userId = "Antonio",
    contributionScope = PayerType.GROUP,
    amount = 30000L,
    currency = "EUR",
    createdAt = LocalDateTime.of(2026, 1, 15, 10, 30)
)

val PREVIEW_CONTRIBUTION_SUBUNIT = Contribution(
    id = "c2",
    groupId = "group-1",
    userId = "Maria",
    contributionScope = PayerType.SUBUNIT,
    subunitId = "subunit-1",
    amount = 30000L,
    currency = "EUR",
    createdAt = LocalDateTime.of(2026, 1, 15, 11, 0)
)

val PREVIEW_CONTRIBUTION_PERSONAL = Contribution(
    id = "c3",
    groupId = "group-1",
    userId = "Pedro",
    contributionScope = PayerType.USER,
    amount = 30000L,
    currency = "EUR",
    createdAt = LocalDateTime.of(2026, 1, 14, 9, 15)
)

val PREVIEW_CONTRIBUTION_4 = Contribution(
    id = "c4",
    groupId = "group-1",
    userId = "Laura",
    contributionScope = PayerType.GROUP,
    amount = 30000L,
    currency = "EUR",
    createdAt = LocalDateTime.of(2026, 1, 14, 14, 45)
)

val PREVIEW_CONTRIBUTION_IMPERSONATED = Contribution(
    id = "c5",
    groupId = "group-1",
    userId = "Maria",
    createdBy = "Pedro",
    contributionScope = PayerType.GROUP,
    amount = 15000L,
    currency = "EUR",
    createdAt = LocalDateTime.of(2026, 1, 13, 16, 0)
)

val PREVIEW_CONTRIBUTION_LINKED = Contribution(
    id = "c6",
    groupId = "group-1",
    userId = "Antonio",
    contributionScope = PayerType.USER,
    amount = 16500L,
    currency = "EUR",
    linkedExpenseId = "exp-dinner-1",
    createdAt = LocalDateTime.of(2026, 1, 16, 20, 30)
)

val PREVIEW_CONTRIBUTIONS = listOf(
    PREVIEW_CONTRIBUTION_GROUP,
    PREVIEW_CONTRIBUTION_SUBUNIT,
    PREVIEW_CONTRIBUTION_PERSONAL,
    PREVIEW_CONTRIBUTION_4,
    PREVIEW_CONTRIBUTION_IMPERSONATED,
    PREVIEW_CONTRIBUTION_LINKED
)

// ── Member Balances ─────────────────────────────────────────────────────────

val PREVIEW_MEMBER_BALANCE_POSITIVE = MemberBalance(
    userId = "Antonio",
    contributed = 30000L,
    withdrawn = 1000000L,
    cashSpent = 250000L,
    nonCashSpent = 5250L,
    totalSpent = 255250L,
    pocketBalance = 18000L,
    cashInHand = 750000L,
    cashInHandByCurrency = listOf(
        CurrencyAmount("THB", 750000L, 20250L)
    ),
    cashSpentByCurrency = listOf(
        CurrencyAmount("THB", 250000L, 6750L)
    )
)

val PREVIEW_MEMBER_BALANCE_NEGATIVE = MemberBalance(
    userId = "Maria",
    contributed = 10000L,
    withdrawn = 0L,
    cashSpent = 0L,
    nonCashSpent = 18000L,
    totalSpent = 18000L,
    pocketBalance = -8000L,
    cashInHand = 0L
)

/**
 * Member with negative cashInHand due to cross-scope FIFO consumption:
 * the member's share of cash expenses exceeds their attributed withdrawals.
 * The UI should display "—" instead of a negative currency amount and
 * show an explanatory hint in the expanded detail.
 */
val PREVIEW_MEMBER_BALANCE_NEGATIVE_CASH = MemberBalance(
    userId = "Pedro",
    contributed = 30000L,
    withdrawn = 0L,
    cashSpent = 7500L,
    nonCashSpent = 5000L,
    totalSpent = 12500L,
    pocketBalance = 25000L,
    cashInHand = -7500L
)
