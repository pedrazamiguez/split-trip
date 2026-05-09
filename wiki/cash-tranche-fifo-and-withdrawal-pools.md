# Cash Tranche FIFO & Withdrawal Pool System

> **Related articles:** [Multi-Currency Logic & Snapshot Model](multi-currency-logic-and-snapshot-model.md) · [Subunits & Group Composition](sub-units-and-group-composition.md) · [Offline-First Architecture](offline-first-architecture.md) · [Core Services Catalog](core-services-catalog.md)

When a cash expense is recorded in a multi-currency group, SplitTrip does not ask the user to enter an exchange rate manually. Instead, it automatically links the expense to the ATM withdrawal(s) that funded it, using a **First-In-First-Out (FIFO)** algorithm. This article explains the complete lifecycle — from withdrawal recording through expense creation to balance calculation.

---

## 1. The Problem: Cash Has a Fixed, Known Rate

Unlike card payments (where the bank charges an unpredictable dynamic rate), cash exchanges happen at a specific ATM withdrawal moment. The rate is fixed at that point in time and should be used for every subsequent cash expense paid from that withdrawal.

**Example:** Andrés withdraws 5,000 THB at an airport ATM at a rate of 0.027 EUR/THB (including ATM fee). Every time the group pays for something in cash, the correct EUR cost must be calculated using the rate(s) from the actual ATM receipt(s) — not a live market rate.

This is fundamentally different from a Revolut/card payment, where the user knows the exact EUR amount and enters it directly.

---

## 2. The `CashWithdrawal` Model

A `CashWithdrawal` records a physical ATM withdrawal. It is the **source of truth** for the available cash pool.

```kotlin
data class CashWithdrawal(
    val id: String = "",
    val groupId: String = "",
    val withdrawnBy: String = "",              // Who physically took the cash
    val withdrawalScope: PayerType = PayerType.GROUP, // Who the cash is for
    val subunitId: String? = null,             // Only when scope = SUBUNIT
    val title: String? = null,                 // Optional label ("Airport ATM", "7-11")
    val amountWithdrawn: Long = 0,             // Total amount taken (in source currency cents)
    val remainingAmount: Long = 0,             // Unspent balance (decreases as expenses consume it)
    val currency: String = "EUR",              // Source currency (THB, USD, etc.)
    val deductedBaseAmount: Long = 0,          // Group-currency equivalent of the withdrawal
    val exchangeRate: BigDecimal = BigDecimal.ONE, // Rate at withdrawal time (BigDecimal, NOT Double)
    val createdAt: LocalDateTime? = null,
    val lastUpdatedAt: LocalDateTime? = null
)
```

### Withdrawal Scopes

| Scope (`withdrawalScope`) | `subunitId` | Meaning |
|---|---|---|
| `GROUP` | `null` | Cash for the entire group's shared expenses |
| `SUBUNIT` | `"subunit-xyz"` | Cash earmarked for a specific subunit (couple, family) |
| `USER` | `null` | Personal cash for the withdrawing individual |

> **Important:** `remainingAmount` is updated in real time by the FIFO algorithm each time an expense consumes from this withdrawal. It reflects exactly how much cash is still unspent.

---

## 3. The FIFO Algorithm

The FIFO algorithm is implemented in `ExpenseCalculatorService.calculateFifoCashAmount()`. It:

1. Takes a target amount to cover (the expense source amount in cents).
2. Takes an ordered list of available `CashWithdrawal`s (oldest first, by `createdAt ASC`).
3. Consumes from the oldest withdrawal first, then the next, until the amount is covered.
4. Returns a `FifoResult` containing:
   - The list of **tranches** consumed (which withdrawal IDs and how many cents from each).
   - The **blended exchange rate** (weighted average across all consumed tranches).

### Scope-Aware Pool Selection (Added in #1008)

Before running FIFO, the repository queries the **correct withdrawal pool** based on the expense's `payerType`. The priority chain for each expense type:

| Expense `payerType` | Pool Priority |
|---|---|
| `GROUP` | GROUP-scoped withdrawals (primary). When a `payerId` is provided, also probes the user's personal (USER-scoped) pool as a supplement — see §5 for the pool-selector UI. |
| `USER` | USER-scoped withdrawals for `expense.payerId` → fallback to GROUP-scoped if insufficient |
| `SUBUNIT` | SUBUNIT-scoped withdrawals for the expense's `subunitId` → fallback to GROUP-scoped if insufficient |

The combined list (scoped pool first, then GROUP) is passed to `calculateFifoCashAmount()` as a **single ordered FIFO sequence** — the algorithm does not know about scopes; the repository handles scope ordering before calling it.

```kotlin
// CashWithdrawalRepository interface
suspend fun getAvailableWithdrawals(
    groupId: String,
    currency: String,
    payerType: PayerType,
    payerId: String? = null  // userId for USER scope, subunitId for SUBUNIT scope
): List<CashWithdrawal>
```

**Impl logic:**
- `GROUP` → fetch GROUP-scoped withdrawals first. If a `payerId` (userId) is provided, also probe the user's personal (USER-scoped) pool. If the GROUP pool alone is insufficient, surfacing personal cash via the pool-selector UI allows the user to supplement from their own ATM withdrawal.
- `USER` → fetch USER-scoped (for `payerId`) + append GROUP-scoped.
- `SUBUNIT` → fetch SUBUNIT-scoped (for `payerId`) + append GROUP-scoped.

If the combined pool is still insufficient, `processCashExpense()` throws `InsufficientCashException`. **A cash expense is never saved without at least one linked tranche.**

> **Critical (pre-#1008 gap):** Previously, `processCashExpense()` only ran when `payerType == GROUP`. USER and SUBUNIT cash expenses bypassed FIFO entirely, leading to negative `cashInHand` values and no rate blending. This is now fixed — FIFO runs for all payer types.

---

## 4. `CashTranchePreview` — Real-Time Preview Before Save

### Domain Model

When the user enters a cash expense amount in the Add Expense wizard, `PreviewCashExchangeRateUseCase` simulates the FIFO run against the current pool. The result is surfaced to the UI as a list of `CashTranchePreview` items:

```kotlin
data class CashTranchePreview(
    val withdrawalId: String,
    val withdrawalTitle: String?,       // From CashWithdrawal.title (may be null/blank)
    val withdrawalDate: LocalDateTime?,
    val amountConsumedCents: Long,      // How much of this withdrawal will be consumed
    val remainingAfterCents: Long,      // Remaining after this expense
    val withdrawalRate: BigDecimal      // Exchange rate of this specific tranche
)
```

### Extended `CashRatePreview`

```kotlin
data class CashRatePreview(
    val displayRate: BigDecimal,
    val groupAmountCents: Long = 0,
    val tranches: List<CashTranchePreview> = emptyList()  // Empty when no amount entered
)
```

### UI Display: "Funded from" Section

The `ExchangeRateStep` composable renders a `FlatCard` "Funded from" section when `cashTranchePreviews` in `AddExpenseUiState` is non-empty:

```
┌─────────────────────────────────────────────────────┐
│ Funded from                                         │
│                                                     │
│  Airport ATM (Jan 10)   →  ฿ 5,000  @ 0.027 EUR    │
│  7-11 next to hotel (Jan 12) →  ฿ 1,000  @ 0.028 EUR│
│                                                     │
│  ⚠ Indicative — final tranches confirmed at save time│
└─────────────────────────────────────────────────────┘
```

- **Single tranche:** compact single-line row.
- **Multiple tranches:** expandable list with count label (e.g., "2 withdrawals") collapsed by default.
- **No amount entered:** `cashTranchePreviews = persistentListOf()` (no section shown).
- **Disclaimer:** always shown below the tranche list when tranches are visible.

### Mapper

`AddExpenseOptionsUiMapper.mapCashTranchePreviews()` converts `List<CashTranchePreview>` → `ImmutableList<CashTranchePreviewUiModel>`. Label fallback: if `withdrawalTitle` is null/blank, uses `"ATM — <formattedDate>"` (using `FormattingHelper.formatShortDate()`).

```kotlin
data class CashTranchePreviewUiModel(
    val withdrawalLabel: String,         // Title or "ATM — Jan 10"
    val formattedAmountConsumed: String, // e.g., "฿ 5,000"
    val formattedRemainingAfter: String, // e.g., "฿ 0 remaining"
    val formattedRate: String            // e.g., "0.027 EUR"
)
```

---

## 5. Manual Withdrawal Pool Selection (#1010)

When multiple pools have available funds (e.g., the user has both personal cash AND group cash), the app shows a pool selector UI. The FIFO algorithm otherwise auto-selects the default priority pool.

### `WithdrawalPoolOption` Domain Model

```kotlin
data class WithdrawalPoolOption(
    val scope: PayerType,          // GROUP, USER, or SUBUNIT
    val ownerId: String? = null    // userId for USER scope, subunitId for SUBUNIT scope
)
```

### `GetAvailableWithdrawalPoolsUseCase`

Queries each candidate scope **independently** (single-scope queries, no GROUP fallback at this level) and returns only pools with `remainingAmount > 0`.

**Priority order returned:**
- **GROUP payerType:** GROUP pool first (primary), then the user's personal (USER-scoped) pool if `payerId` is provided and has funds. This allows the pool-selector UI to surface personal cash as a supplement when GROUP funds alone are insufficient.
- **USER / SUBUNIT payerType:** personal/subunit pool first, GROUP pool second.

```kotlin
// Usage in CurrencyEventHandler (via WithdrawalPoolSelectionDelegate)
val pools: List<WithdrawalPoolOption> = getAvailableWithdrawalPoolsUseCase(
    groupId = groupId,
    currency = sourceCurrency,
    payerType = currentPayerType,
    payerId = currentPayerId
)
```

### `WithdrawalPoolSelectionDelegate`

A **plain class** (NOT an `AddExpenseEventHandler`, NOT a ViewModel) that manages pool selection logic:

- `fetchAvailablePools(...)` → calls `GetAvailableWithdrawalPoolsUseCase`, maps to `WithdrawalPoolOptionUiModel`s, updates `availableWithdrawalPools` in state. Auto-selects the only pool if exactly one is available (no UI shown).
- `handlePoolSelected(scope, subunitId)` → updates `selectedWithdrawalPool`, triggers `onPoolChangedCallback` so `CurrencyEventHandler` re-fetches the cash rate preview for the new pool.

> **Why a Delegate, not a Handler?** `CurrencyEventHandler` was already at the 600-line Konsist hard limit. Per the Handler → Delegate sub-pattern (see `copilot-instructions.md` §1), cohesive logic sections can be extracted into `*Delegate` classes that do NOT implement `AddExpenseEventHandler` and do NOT participate in `bind()`. `WithdrawalPoolSelectionDelegate` follows this pattern.

### UI: `WithdrawalPoolSelectorSection`

A `FlowRow` of `PassportChip`s (consistent with the payment step pattern), shown in the **AmountStep** (same-currency CASH) or **ExchangeRateStep** (foreign-currency CASH). Visible only when `availableWithdrawalPools.size > 1`.

The **first pool in the list** is pre-selected by `WithdrawalPoolSelectionDelegate` (GROUP for GROUP expenses, personal/subunit for USER/SUBUNIT expenses), matching the documented FIFO priority order and saving the user a tap in the common case.

When the user taps a different chip, `AddExpenseViewModel` routes `WithdrawalPoolSelected` to both `CurrencyEventHandler` (re-fetch cash rate preview for the new pool) and `SplitEventHandler` (`applyPersonalPoolSplitDefault` to pre-fill split exclusions based on the pool scope).

```
┌─────────────────────────────────────────────────────┐
│ Choose cash source:                                 │
│  ● My personal cash  (฿ 800 available)              │
│  ○ Group cash        (฿ 5,000 available)            │
└─────────────────────────────────────────────────────┘
```

### UiState Extensions

```kotlin
// AddExpenseUiState additions
val availableWithdrawalPools: ImmutableList<WithdrawalPoolOptionUiModel> = persistentListOf()
val selectedWithdrawalPool: WithdrawalPoolOptionUiModel? = null
```

### Submission

`SubmitEventHandler.submitExpense()` reads `currentState.selectedWithdrawalPool` and passes `preferredWithdrawalScope`/`preferredWithdrawalOwnerId` to `AddExpenseUseCase`. `AddExpenseUseCase` uses the explicit scope override when present, skipping the default priority chain.

---

## 6. Conflict Detection & Resolution

In a multi-user group, two members can simultaneously open the Add Expense wizard. The tranche preview shown to both members is based on the same Room DB snapshot. If both save an expense that draws from the same withdrawal pool, the second save may hit `InsufficientCashException` after the first has already consumed the available cash.

### Phase 1 — Graceful Error (Shipped in #1011)

When `processCashExpense()` throws `InsufficientCashException` at save time:

1. `SubmitResultDelegate` emits `AddExpenseUiAction.ShowCashConflictError(message)` (NOT `ShowError`).
2. `AddExpenseFeature` receives the action:
   - Shows a top pill notification: *"Someone else just used this cash. The available amount has changed — please review and retry."*
   - Calls `addExpenseViewModel.refreshCashPreview()` → delegates to `currencyEventHandler.fetchCashRate()`.
3. The `ExchangeRateStep` auto-refreshes with the updated available pool.
4. The form stays open with all user input intact.

```kotlin
// AddExpenseUiAction
data class ShowCashConflictError(val message: UiText) : AddExpenseUiAction
```

### Phase 2 — Optimistic Locking at Firestore (Follow-up: #1018)

Closing the race window requires transaction-based conflict detection at the Firestore layer. Not yet implemented.

### Phase 3 — Resolution UX (Follow-up: #1019)

A dedicated conflict resolution flow (adjust amount / switch payment method / wait). Not yet implemented.

---

## 7. Balance Engine: `cashInHand` Formula Change

### Before (#1008)

```
cashInHand[user] = rawWithdrawn[user] − cashSpent[user]
```

This was a **workaround**: `rawWithdrawn` aggregated `deductedBaseAmount` of withdrawals attributed to the user, and `cashSpent` summed the group-currency amount of all cash-paid expenses per user. The problem: USER and SUBUNIT cash expenses bypassed FIFO (no `remainingAmount` update), so `cashSpent > 0` with `rawWithdrawn = 0` → negative `cashInHand`.

### After (#1008 fix)

```
cashInHand[user] = sum(withdrawal.remainingAmount_in_group_currency × userShare)
```

Where `userShare` depends on `withdrawalScope`:
- `GROUP` → `remainingAmount × groupCurrencyRate ÷ memberCount` (equal share)
- `SUBUNIT` → `remainingAmount × groupCurrencyRate × memberShare` (per subunit share weights)
- `USER` → `remainingAmount × groupCurrencyRate` (100% to `withdrawnBy`)

This is **always accurate** because `remainingAmount` is updated on every FIFO run regardless of the expense's scope. The old formula was an approximation; the new formula is direct.

> The `—` display introduced in PR #844 (showing an em-dash instead of a negative currency amount) can remain as a defensive safety net but should never trigger for any cash expense saved after #1008.

---

## 8. Performance Optimizations (#1012)

### DB Indexes (Migration 25→26)

New composite indexes added to support scoped FIFO queries and balance computation:

| Table | Index columns | Purpose |
|---|---|---|
| `cash_withdrawals` | `(groupId, currency, withdrawalScope, remainingAmount)` | Covers all 4 scoped FIFO queries |
| `cash_withdrawals` | `(groupId, syncStatus)` | Pending sync checks |
| `expenses` | `(groupId, syncStatus)` | Pending sync checks |
| `contributions` | `(groupId, syncStatus)` | Pending sync checks |

### Balance Computation Dispatcher

`GetMemberBalancesFlowUseCase` is CPU-bound (no I/O). The combined balance flow uses:

```kotlin
.debounce(AppConstants.BALANCE_COMPUTATION_DEBOUNCE_MS)  // 300ms — absorbs rapid write bursts
.flowOn(Dispatchers.Default)                               // CPU-bound computation off main thread
```

`BalancesViewModel` accepts `computationDispatcher: CoroutineDispatcher = Dispatchers.Default` as an injectable constructor parameter for testability.

### `@DatabaseView` Decision (Deferred)

`computeMemberBalances()` requires per-split rows for FIFO cash attribution and add-on handling — a `@DatabaseView` partial aggregate would fragment the domain pipeline without eliminating the `BigDecimal` computation. A `// TODO(#1012): revisit @DatabaseView` comment marks the decision point in `BalancesViewModel`.

---

## 9. Architecture Compliance Notes

| Concern | How It's Handled |
|---|---|
| FIFO math in domain service, not ViewModel | `ExpenseCalculatorService.calculateFifoCashAmount()` owns all arithmetic |
| `BigDecimal` for all rates | `CashWithdrawal.exchangeRate: BigDecimal`, `CashTranchePreview.withdrawalRate: BigDecimal` |
| Formatting in Mappers only | `AddExpenseOptionsUiMapper.mapCashTranchePreviews()` uses `FormattingHelper`; no formatting in use cases or handlers |
| `WithdrawalPoolSelectionDelegate` is NOT a ViewModel | Plain class, lambda-based state access, co-created in `viewModel { }` Koin block |
| Conflict action is a `UiAction`, not `UiState` mutation | `ShowCashConflictError` is emitted via `SharedFlow<AddExpenseUiAction>`; the form state is NOT reset |
| Offline-First for `remainingAmount` updates | `updateRemainingAmounts()` saves to Room first, cloud sync in background |
| `InsufficientCashException` never causes data loss | Exception is thrown before any Room write; the expense is not saved |

---

## 10. Quick Reference: Related Domain Types

| Type | Module | Purpose |
|---|---|---|
| `CashWithdrawal` | `:domain` | ATM withdrawal record with `remainingAmount` and `withdrawalScope` |
| `CashTranchePreview` | `:domain` | Read model: simulated FIFO consumption for one withdrawal |
| `CashRatePreview` | `:domain` | Preview result from `PreviewCashExchangeRateUseCase` (includes `tranches`) |
| `WithdrawalPoolOption` | `:domain` | Available pool descriptor (`scope` + `ownerId`) |
| `InsufficientCashException` | `:domain` | Thrown when no pool has enough funds to cover the expense |
| `CashTranchePreviewUiModel` | `:features:expenses` | Formatted tranche row for "Funded from" section |
| `WithdrawalPoolOptionUiModel` | `:features:expenses` | Formatted pool option for `WithdrawalPoolSelectorSection` |
| `WithdrawalPoolSelectionDelegate` | `:features:expenses` | Delegate (not handler) managing pool fetch + selection state |
| `GetAvailableWithdrawalPoolsUseCase` | `:domain` | Returns pools with available funds (single-scope queries, no fallback) |
| `PreviewCashExchangeRateUseCase` | `:domain` | Simulates FIFO and returns `CashRatePreview` with tranches |

