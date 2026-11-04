# Add-Ons Architecture

## Overview

**Add-ons** are structured metadata attached to an **Expense** or **Cash Withdrawal** that represent extra charges (fees, tips, surcharges) or reductions (discounts) beyond the base item price.

Unlike a flat "total includes everything" model, add-ons decompose the financial event into **base cost + extras**, enabling:

- **Analytics:** *"We spent 4% of our budget on ATM fees."*
- **Transparency:** *"The €80 dinner was actually €66.67 food + €13.33 included tip."*
- **Flexibility:** Each add-on can have its own currency, exchange rate, and payment method — a bank fee in EUR on a MXN boat trip, or a cash tip on a card-paid dinner.

---

## 1. Domain Model

### 1.1 The `AddOn` Data Class

```kotlin
// domain/model/AddOn.kt
data class AddOn(
    val id: String = "",               // Client-side UUID
    val type: AddOnType = AddOnType.FEE,
    val mode: AddOnMode = AddOnMode.ON_TOP,
    val valueType: AddOnValueType = AddOnValueType.EXACT,
    val amountCents: Long = 0,         // Resolved absolute amount in add-on's OWN currency
    val currency: String = "EUR",      // Can differ from parent expense
    val exchangeRate: BigDecimal = BigDecimal.ONE, // Add-on currency → group currency
    val groupAmountCents: Long = 0,    // Converted amount in group currency (used in balances)
    val paymentMethod: PaymentMethod = PaymentMethod.OTHER,
    val description: String? = null    // Optional label ("ATM fee", "Service charge")
)
```

**Key design decisions:**

| Field | Rationale |
|---|---|
| `id` | Generated client-side via `UUID.randomUUID()` — never from Firestore `.add()`. Follows the offline-first write protocol. |
| `amountCents` | Always the **resolved absolute** amount regardless of whether the user entered "10%" or "2.50 EUR". Stored in the add-on's own currency minor units. |
| `exchangeRate` | Stored as `BigDecimal` for precision. Serialized as `String` at the Firestore boundary via `toPlainString()`. |
| `groupAmountCents` | The value actually used in balance calculations. Pre-converted so downstream consumers don't need the rate. |

### 1.2 Enums

#### `AddOnType` — Semantic Category

| Value | Description | Example |
|---|---|---|
| `TIP` | Voluntary gratuity | Restaurant tip, bellhop tip |
| `FEE` | Involuntary charge | ATM fee, bank transfer fee, booking fee |
| `SURCHARGE` | Extra charge | Credit card surcharge, tourist tax |
| `DISCOUNT` | Price reduction | Early-bird discount, coupon |

#### `AddOnMode` — Relationship to Base Amount

This is the most nuanced enum. It determines **how** the add-on interacts with the expense's stored amounts.

| Mode | Meaning | Storage Effect | Example |
|---|---|---|---|
| `ON_TOP` | Added **on top of** the base amount | `groupAmount` stores the base; add-on grows the effective total | "100 EUR dinner + 10 EUR tip on top → effective 110 EUR" |
| `INCLUDED` | Extracted **from within** the user-entered total | `groupAmount` stores the **derived base cost** (total minus included portions); add-on captures the extracted portion | "80 EUR total already includes 20% tip → base 66.67 EUR + tip 13.33 EUR" |

> **Both modes decompose the payment into base + add-on.** The difference is the input flow:
> - `ON_TOP`: User enters the base, add-on is extra.
> - `INCLUDED`: User enters the total, add-on is extracted to derive the base.

#### `AddOnValueType` — Input Method

| Value | Meaning | Resolution |
|---|---|---|
| `EXACT` | User entered an absolute amount | `"5.50"` → 550 cents (via `input × 10^decimalDigits`) |
| `PERCENTAGE` | User entered a percentage of the base | `"10"` on 10000 cents → 1000 cents (via `sourceAmountCents × input / 100`) |

> `AddOnValueType` is **metadata for display/edit purposes only**. After resolution, both types produce the same `amountCents`. The enum tells the UI which input widget to show and how to re-populate the field on edit.

---

## 2. Where Add-Ons Live

Add-ons are embedded **inside** their parent entity — they are not standalone documents.

| Parent Entity | Field | Typical Use Cases |
|---|---|---|
| `Expense` | `val addOns: List<AddOn>` | Tips, service fees, surcharges, discounts on expenses |
| `CashWithdrawal` | `val addOns: List<AddOn>` | ATM fees on cash withdrawals |

### 2.1 Expense Context

An expense can have **multiple** add-ons of different types and modes. Example:

```
Dinner: 100 EUR base
  + 10 EUR tip (ON_TOP, TIP)           → effective 110 EUR
  + 2.50 EUR bank fee (ON_TOP, FEE)    → effective 112.50 EUR
  − 5 EUR discount (ON_TOP, DISCOUNT)  → effective 107.50 EUR
```

### 2.2 Cash Withdrawal Context

A cash withdrawal typically has **zero or one** add-on: the ATM fee.

```
ATM Withdrawal: 10,000 THB
  + 260 THB ATM fee (ON_TOP, FEE)  → fee converted to 7.06 EUR at withdrawal rate
```

The ATM fee add-on is always `ON_TOP` and `FEE`. It can have its own currency (the fee might be charged in EUR while the withdrawal is in THB) and its own exchange rate.

---

## 3. Calculation Services

### 3.1 `AddOnCalculationService`

**Location:** `domain/service/AddOnCalculationService.kt`

The central service for all add-on math. It uses `AddOnResolverFactory` internally for amount resolution.

| Method | Purpose | When Called |
|---|---|---|
| `resolveAddOnAmountCents(...)` | Converts user input → absolute cents (EXACT or PERCENTAGE) | Real-time as user types in add-on amount field |
| `calculateTotalOnTopAddOns(addOns)` | Sums ON_TOP non-discount add-ons (group currency) | Display: "Extra costs beyond the base" |
| `calculateTotalAddOnExtras(addOns)` | Sums ALL non-discount add-ons (ON_TOP + INCLUDED) | Balance screen "Extras" line item |
| `calculateEffectiveGroupAmount(base, addOns)` | Computes the real group debt: `base + ON_TOP + INCLUDED − ON_TOP_DISCOUNT` | Balance calculation, paired contribution amount |
| `calculateIncludedBaseCost(...)` | Extracts base cost from a total that includes embedded add-ons | Submission: deriving the stored `groupAmount` |
| `calculateEffectiveDeductedAmount(base, addOns)` | Effective withdrawal deduction including ATM fees | Cash withdrawal balance calculation |
| `convertGroupToSourceCents(...)` | Reverse-converts group cents → add-on currency cents | Adjusting percentage add-on amounts after base cost extraction |
| `sumPercentagesFromInputs(inputs)` | Sums raw percentage input strings | Base cost calculation for INCLUDED percentage add-ons |

### 3.2 `AddOnResolverFactory` (Strategy Pattern)

**Location:** `domain/service/addon/`

Mirrors the `ExpenseSplitCalculatorFactory` pattern.

```
AddOnResolverFactory
  ├── ExactAddOnResolver      → input × 10^decimalDigits
  └── PercentageAddOnResolver → sourceAmountCents × input / 100
```

Both implement the `AddOnAmountResolver` interface:

```kotlin
interface AddOnAmountResolver {
    fun resolve(normalizedInput: BigDecimal, decimalDigits: Int, sourceAmountCents: Long): Long
}
```

### 3.3 Validation (`ExpenseValidationService`)

Add-on validation lives alongside other expense validation:

| Rule | Implementation |
|---|---|
| Amount must be > 0 | `addOn.amountCents <= 0` → Invalid |
| Currency must not be blank | `addOn.currency.isBlank()` → Invalid |
| INCLUDED amount must be < source amount | `addOn.amountCents >= sourceAmountCents` → Invalid |

The `validateAddOns(addOns, sourceAmountCents)` method iterates all add-ons and returns the first error (fail-fast).

---

## 4. The Effective Amount Formula

The effective group amount determines the **real debt** for balance calculations:

```
effectiveGroupAmount = baseGroupAmount
    + Σ(ON_TOP non-discount add-ons)
    + Σ(INCLUDED non-discount add-ons)
    − Σ(ON_TOP DISCOUNT add-ons)
```

> **INCLUDED DISCOUNT** add-ons are **informational only** — the user already entered the discounted price, so they don't affect the effective amount.

**Examples:**

| Scenario | Base | Add-Ons | Effective |
|---|---|---|---|
| No add-ons | 10,000 | — | 10,000 |
| +250 fee ON_TOP | 10,000 | +250 | 10,250 |
| −500 discount ON_TOP | 10,000 | −500 | 9,500 |
| +1,000 tip INCLUDED | 9,000 (derived base) | +1,000 | 10,000 (reconstructs original total) |
| Mixed: +1,000 tip ON_TOP, +250 fee ON_TOP, +800 tip INCLUDED, −500 discount | 10,000 | +1,000 +250 +800 −500 | 11,550 |

---

## 5. ON_TOP vs. INCLUDED: Deep Dive

### 5.1 ON_TOP Flow

The simplest case. The user enters the **base cost**, then adds extras on top.

```
User enters:  100.00 EUR (base)
User adds:     10.00 EUR tip ON_TOP

Stored:
  expense.groupAmount  = 10000  (base)
  addOn.groupAmountCents = 1000 (tip)

Effective total = 10000 + 1000 = 11000 (110.00 EUR)
```

The `groupAmount` stores the base cost directly. No adjustment needed at submission.

### 5.2 INCLUDED Flow

The user enters the **total** (which already contains the tip/fee), and the system extracts the base cost.

```
User enters:   80.00 EUR (total, already includes 20% tip)
User marks:    20% tip as INCLUDED

At submission, the system computes:
  baseCost = 80.00 / (1 + 0.20) = 66.67 EUR
  tipAmount = 80.00 − 66.67 = 13.33 EUR

Stored:
  expense.groupAmount  = 6667  (derived base cost)
  addOn.groupAmountCents = 1333 (extracted tip)

Effective total = 6667 + 1333 = 8000 (80.00 EUR — matches user input)
```

**Base cost extraction** uses `calculateIncludedBaseCost`:

1. **EXACT INCLUDED** amounts are subtracted directly: `afterExact = total − sumOfExactIncludedCents`
2. **PERCENTAGE INCLUDED** amounts are extracted via division: `baseCost = afterExact / (1 + sumOfPercentages / 100)`

> For mixed cases (both EXACT and PERCENTAGE INCLUDED add-ons), exact amounts are subtracted first, then percentages are extracted from the remainder.

### 5.3 ON_TOP DISCOUNT Flow

Discounts reduce the stored base amount. Unlike tips/fees that are stored as separate add-ons and reconstructed, **ON_TOP discounts are "baked" into `groupAmount`** at submission:

```
User enters:  100.00 EUR (base)
User adds:      5.00 EUR discount ON_TOP

At submission (adjustForOnTopDiscounts):
  expense.groupAmount  = 9500  (100.00 − 5.00 = 95.00 EUR)
  addOn.groupAmountCents = 0   (zeroed to prevent double-subtraction)

Effective total = 9500 + 0 = 9500 (95.00 EUR)
```

The `groupAmountCents` is zeroed after baking so that `calculateEffectiveGroupAmount` doesn't subtract the discount again. The original amount is still recoverable from `addOn.amountCents` and `addOn.exchangeRate`.

### 5.4 INCLUDED DISCOUNT Flow

Informational only. The user already entered the discounted price:

```
User enters:  95.00 EUR (already discounted)
User marks:    5.00 EUR discount as INCLUDED (for tracking: "we got a 5 EUR coupon")

Stored:
  expense.groupAmount  = 9500  (no adjustment — user already entered discounted price)
  addOn.groupAmountCents = 500 (recorded for analytics, not used in calculation)
```

---

## 6. Multi-Currency Add-Ons

Each add-on can have its **own currency** independent of the parent expense. This enables scenarios like:

- A bank fee in EUR on a THB boat trip
- A cash tip in local currency on a card-paid dinner

### 6.1 Exchange Rate per Add-On

When the add-on currency differs from the group currency, the add-on carries its own `exchangeRate` (add-on currency → group currency) and `groupAmountCents` (pre-converted for balance calculations).

```
Expense: 4000 MXN boat trip (group currency: EUR, rate: 0.05 → 200.00 EUR)
Add-on:  2.50 EUR bank fee (same as group currency → rate: 1.0)

Stored:
  expense.groupAmount = 20000  (200.00 EUR base)
  addOn.amountCents = 250      (2.50 EUR in add-on currency)
  addOn.currency = "EUR"
  addOn.exchangeRate = 1.0
  addOn.groupAmountCents = 250 (2.50 EUR in group currency)

Effective = 20000 + 250 = 20250 (202.50 EUR)
```

### 6.2 CASH Payment Method & FIFO Rates

When an add-on's payment method is `CASH` and the currency is foreign:

- The exchange rate is **locked** (not user-editable)
- The rate is derived from **ATM withdrawal FIFO** blended rates
- The `AddOnExchangeRateDelegate` fetches the rate via `PreviewCashExchangeRateUseCase`
- If insufficient cash is available, a warning hint is displayed

When switching **from CASH to non-CASH**, the previously saved exchange rate is restored (or an API rate is fetched if none was saved).

---

## 7. Submission Pipeline

The submission flow for add-ons involves several adjustments before the expense is saved:

```
UI State ──► mapToDomain() ──► adjustForIncludedAddOns() ──► adjustForOnTopDiscounts() ──► AddExpenseUseCase
```

### 7.1 `mapToDomain()` (AddExpenseUiMapper + AddExpenseAddOnUiMapper)

Converts `AddOnUiModel` → domain `AddOn`. Only includes add-ons with `resolvedAmountCents > 0`.

### 7.2 `adjustForIncludedAddOns()` (SubmitEventHandler)

When INCLUDED non-discount add-ons are present:

1. **Computes base costs** in both group and source currencies via `calculateIncludedBaseCost` + `computeProportionalAmount`
2. **Adjusts PERCENTAGE INCLUDED add-ons** using a residual approach:
   - `residual = originalGroupAmount − includedExactCents − baseCostGroup`
   - Distributes residual proportionally across percentage add-ons (floor rounding + remainder redistribution)
   - Guarantees `base + includedExact + sum(includedPct) == originalGroupAmount` exactly
3. **Rescales splits** proportionally to the new base source amount

### 7.3 `adjustForOnTopDiscounts()` (SubmitEventHandler)

Bakes ON_TOP discount amounts into the expense:

1. Subtracts discount cents from `groupAmount` and `sourceAmount`
2. Zeroes each ON_TOP discount add-on's `groupAmountCents` to prevent double-subtraction
3. Rescales splits to the new amounts

### 7.4 `AddExpenseUseCase`

The use case computes the effective amount for the **paired contribution** (when the expense is paid by a specific user):

```kotlin
val effectiveAmount = addOnCalculationService.calculateEffectiveGroupAmount(
    expense.groupAmount, expense.addOns
)
```

This ensures the contribution offsets the full effective cost, not just the base.

---

## 8. Balance Integration

Add-ons affect balance calculations in two places:

### 8.1 Expense Balances

`GetMemberBalancesFlowUseCase` uses `calculateEffectiveGroupAmount` to determine the real group debt. Splits are distributed against the **base** `groupAmount`, while the effective amount (including add-ons) determines total cost attribution.

### 8.2 Cash Withdrawal Balances

`calculateEffectiveDeductedAmount` adds ON_TOP add-on amounts (ATM fees) to the base deducted amount:

```kotlin
fun calculateEffectiveDeductedAmount(baseDeductedAmount: Long, addOns: List<AddOn>): Long {
    val addOnTotal = addOns.filter { it.mode == AddOnMode.ON_TOP }.sumOf { it.groupAmountCents }
    return baseDeductedAmount + addOnTotal
}
```

> INCLUDED add-ons are ignored for cash withdrawals — they don't apply to the ATM context.

---

## 9. Persistence

### 9.1 Firestore (Cloud)

Add-ons are stored as a nested array within the parent document (`ExpenseDocument.addOns`, `CashWithdrawalDocument.addOns`).

```kotlin
// AddOnDocument — Firestore representation
data class AddOnDocument(
    val id: String = "",
    val type: String = "FEE",           // Enum names as strings
    val mode: String = "ON_TOP",
    val valueType: String = "EXACT",
    val amountCents: Long = 0L,
    val currency: String = "EUR",
    val exchangeRate: String? = null,   // BigDecimal → String (no IEEE 754 loss)
    val groupAmountCents: Long = 0L,
    val paymentMethod: String = "OTHER",
    val description: String? = null
)
```

**Critical:** `exchangeRate` is serialized as `String` via `toPlainString()` and deserialized with `toBigDecimalOrNull() ?: BigDecimal.ONE`. This prevents IEEE 754 floating-point precision loss that `Double` would introduce.

### 9.2 Room (Local)

Add-ons are stored as a **JSON string column** via `AddOnListConverter` (Room `@TypeConverter`):

- Serialization: Manual JSON building (no `org.json`) for pure JVM test compatibility
- Exchange rates serialized as String (same precision rules as Firestore)
- Defensive parsing with safe defaults for all enum fields

### 9.3 Mapping Chain

```
Domain AddOn ←→ AddOnDocument (Firestore)   via ExpenseDocumentMapper / CashWithdrawalDocumentMapper
Domain AddOn ←→ JSON String (Room)           via AddOnListConverter
Domain AddOn ←→ AddOnUiModel (Presentation)  via AddExpenseAddOnUiMapper
```

---

## 10. Presentation Layer

### 10.1 `AddOnUiModel`

The UI model carries both domain data and UI-specific state:

| Field | Purpose |
|---|---|
| `amountInput` | Raw user input (editable text field, locale-aware) |
| `resolvedAmountCents` | Computed absolute amount in add-on currency |
| `groupAmountCents` | Converted to group currency |
| `showExchangeRateSection` | Whether add-on currency ≠ group currency |
| `isExchangeRateLocked` | CASH payment → rate is system-determined |
| `preCashExchangeRate` | Snapshot before switching to CASH (restored on switch back) |
| `isLoadingRate` | Rate fetch in progress |
| `isAmountValid` | Input validation state |

### 10.2 Event Handler Architecture

Add-on UI logic is managed by the **`AddOnEventHandler`** (implements `AddExpenseEventHandler`) with two delegates:

```
AddOnEventHandler (Event Handler — participates in bind())
  ├── AddOnCrudDelegate          (Stateless delegate — creation, currency context, payment method switching)
  └── AddOnExchangeRateDelegate  (Lambda-based delegate — API rates, CASH/FIFO rates, debouncing)
```

**Why delegates?** The `AddOnEventHandler` handles 13 public event methods + 7 private helpers. Without delegation, it would exceed the 600-line Konsist limit. The delegates extract cohesive concerns:

- **`AddOnCrudDelegate`**: Building new add-ons, resolving currency context flags, applying payment method switch state transitions.
- **`AddOnExchangeRateDelegate`**: API rate fetching, CASH/FIFO blended rate previews, forward/reverse recalculation, debounced amount-change rate updates. Manages its own `ConcurrentHashMap<String, Job>` maps for tracked in-flight requests.

### 10.3 Cross-Handler Communication

The `AddOnEventHandler` exposes `recalculateEffectiveTotal()` for other handlers to call when the source amount or currency changes. This re-resolves all add-on amounts and updates the effective total and included base cost displays.

### 10.4 UI State Fields

The `AddExpenseUiState` carries add-on-related display state:

| Field | Purpose |
|---|---|
| `addOns: ImmutableList<AddOnUiModel>` | The list of add-ons in the form (each carrying `isExpanded: Boolean` for per-item progressive disclosure) |
| `effectiveTotal: String` | Formatted effective amount (shown when add-ons modify the base) |
| `includedBaseCost: String` | Formatted derived base cost (shown when INCLUDED add-ons are present) |
| `addOnError: UiText?` | Validation error message for the add-on section |

---

## 11. Cash Withdrawal Add-Ons

The `:features:withdrawals` module supports a single optional ATM fee add-on:

- User toggles "ATM fee" on in the Details step
- Enters fee amount and optionally selects a different currency
- If fee currency ≠ group currency, a fee exchange rate step appears
- The fee is built as an `AddOn(type = FEE, mode = ON_TOP)` by `WithdrawalSubmitHandler.buildFeeAddOn()`

The wizard step sequence adapts dynamically:

```
AMOUNT → [EXCHANGE_RATE] → SCOPE → DETAILS → [ATM_FEE] → [FEE_EXCHANGE_RATE] → REVIEW
```

Steps in brackets are conditionally shown based on form state.

---

## 12. Testing

### 12.1 Domain Service Tests

- **`AddOnCalculationServiceTest`**: Comprehensive tests for all calculation methods — effective amounts, base cost extraction, mixed add-on scenarios, cash withdrawal deduction.
- **`ExpenseValidationServiceTest`**: Add-on validation rules (amount > 0, currency not blank, INCLUDED < source).

### 12.2 Balance Integration Tests

- **`GetMemberBalancesFlowUseCaseAddOnTest`**: End-to-end scenarios verifying that add-ons correctly affect member balances — ON_TOP fees, INCLUDED tips, mixed add-ons, multi-currency fees, cash withdrawal ATM fees.

### 12.3 Mapper & Converter Tests

- **`AddExpenseUiMapperTest`**: Add-on mapping from UI model to domain, including exchange rate handling and description mapping.
- **`AddOnListConverterTest`**: Round-trip serialization, backward compatibility, defensive parsing of malformed JSON, BigDecimal precision preservation.

### 12.4 ViewModel Tests

- **`AddExpenseViewModelTest`**: Integration tests for add-on event handling through the full handler/delegate chain.

---

## 13. Real-World Scenarios

### Scenario E1: Foreign Expense with Bank Fee

> 4,000 MXN boat trip. Bank charges 2.50 EUR fee.

```
Expense:  4000 MXN → 200.00 EUR (rate: 0.05)
Fee:      2.50 EUR (ON_TOP, FEE, same as group currency)

groupAmount = 20000
fee.groupAmountCents = 250
effectiveGroupAmount = 20250 (202.50 EUR)
```

### Scenario E2: ATM Withdrawal with Foreign Fee

> 5,000 THB withdrawal, ATM charges 260 THB fee.

```
Withdrawal: 5000 THB → 135.87 EUR (rate: 0.027135)
Fee: 260 THB → 7.06 EUR (ON_TOP, FEE)

deductedBaseAmount = 13587
fee.groupAmountCents = 706
effectiveDeductedAmount = 14293 (142.93 EUR)
```

### Scenario E3: Dinner with Included Tip

> 80 EUR dinner, total already includes 20% tip.

```
User enters: 80.00 EUR
Marks: 20% tip INCLUDED

baseCost = 8000 / 1.20 = 6667 (66.67 EUR)
tip = 8000 − 6667 = 1333 (13.33 EUR)

Stored: groupAmount = 6667, tip.groupAmountCents = 1333
effectiveGroupAmount = 6667 + 1333 = 8000 (80.00 EUR) ✓
```

### Scenario E4: Mixed Add-Ons

> 100 EUR base + 10 EUR tip ON_TOP + 2.50 EUR fee ON_TOP + 8 EUR tip INCLUDED − 5 EUR discount.

```
At submission:
  adjustForIncludedAddOns → base becomes 92 EUR (100 − 8 = 92... simplified)
  adjustForOnTopDiscounts → base becomes 87 EUR (92 − 5)

Effective = 87 + 10 + 2.50 + 8 = 107.50 EUR
```

---

## 14. File Reference

| Layer | File | Purpose |
|---|---|---|
| **Domain Model** | `domain/model/AddOn.kt` | Data class |
| **Domain Enums** | `domain/enums/AddOnType.kt`, `AddOnMode.kt`, `AddOnValueType.kt` | Type, mode, and value type enums |
| **Domain Service** | `domain/service/AddOnCalculationService.kt` | All add-on math |
| **Domain Strategy** | `domain/service/addon/AddOnResolverFactory.kt`, `ExactAddOnResolver.kt`, `PercentageAddOnResolver.kt` | Amount resolution strategy |
| **Domain Validation** | `domain/service/ExpenseValidationService.kt` | `validateAddOn()`, `validateAddOns()` |
| **Firestore** | `data/firebase/.../document/AddOnDocument.kt` | Cloud document model |
| **Firestore Mappers** | `data/firebase/.../mapper/ExpenseDocumentMapper.kt`, `CashWithdrawalDocumentMapper.kt` | Domain ↔ Firestore |
| **Room Converter** | `data/local/.../converter/AddOnListConverter.kt` | JSON serialization for Room |
| **Room Entity Mapper** | `data/local/.../mapper/ExpenseEntityMapper.kt` | Domain ↔ Room entity |
| **UI Model** | `features/expenses/.../model/AddOnUiModel.kt` | Presentation model |
| **UI Mapper** | `features/expenses/.../mapper/AddExpenseAddOnUiMapper.kt` | UI model ↔ Domain |
| **Event Handler** | `features/expenses/.../handler/AddOnEventHandler.kt` | Add-on form event handling |
| **CRUD Delegate** | `features/expenses/.../handler/AddOnCrudDelegate.kt` | Creation & payment method switching |
| **Rate Delegate** | `features/expenses/.../handler/AddOnExchangeRateDelegate.kt` | Exchange rate fetching & calculation |
| **Submit Handler** | `features/expenses/.../handler/SubmitEventHandler.kt` | INCLUDED base cost extraction, ON_TOP discount baking |
| **Withdrawal Fee** | `features/withdrawals/.../handler/WithdrawalSubmitHandler.kt` | ATM fee add-on builder |
| **Tests** | `domain/src/test/.../AddOnCalculationServiceTest.kt` | Service tests |
| **Tests** | `domain/src/test/.../GetMemberBalancesFlowUseCaseAddOnTest.kt` | Balance integration tests |
| **Tests** | `data/local/src/test/.../AddOnListConverterTest.kt` | Converter round-trip tests |

