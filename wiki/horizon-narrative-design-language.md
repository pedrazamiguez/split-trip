# The Horizon Narrative — Design Language Specification

> **Issue:** [#877 — Design System: The Horizon Narrative](https://github.com/pedrazamiguez/split-trip/issues/877)

## 1. Creative North Star: "The Curated Expedition"

The Horizon Narrative is SplitTrip's design language — a deliberate rejection of rigid, utility-first travel interfaces in favour of an **editorial, journal-like experience**. Every screen is curated like a page from a high-end travel journal.

The aesthetic is defined by **Optimistic Kineticism**: energetic curves, sophisticated surface layering, and a sense of movement and discovery. We treat the interface not as a spreadsheet of expenses but as a destination in itself. The user's journey begins the moment they open the app.

### Brand Pillars

| Pillar | Expression |
|---|---|
| **Trust** | Deep blue primary palette (`#00478D`), anchoring every interaction in reliability |
| **Discovery** | Revitalising teal secondary (`#006874`), evoking water and exploration |
| **Warmth** | Off-white foundation surfaces (`#F9F9FF`), soft and inviting — never clinical |
| **Premium** | Tonal layering over borders, ambient shadows over hard edges, generous whitespace |
| **Welcoming** | Full-roundedness on CTAs and chips, extra-large corner radii on image cards |

---

## 2. Color Palette

The palette is rooted in **deep, trustworthy blues** (primary) and **revitalising teals** (secondary), balanced by a **warm, off-white foundation** (background). An amber tertiary adds warmth for accents.

### 2.1 Primary — Horizon Blue (Trust, Depth, Horizon)

| Role | Light | Dark | Named Constant |
|---|---|---|---|
| `primary` | `#00478D` | `#9ECAFF` | `HorizonBlue` / `HorizonBlueDark` |
| `primaryContainer` | `#005EB8` | `#00478D` | `HorizonBlueContainer` / `HorizonBlueContainerDark` |
| `onPrimary` | `#FFFFFF` | `#00315A` | `HorizonOnBlue` / `HorizonOnBlueDark` |
| `onPrimaryContainer` | `#FFFFFF` | `#D1E4FF` | `HorizonOnBlueContainer` / `HorizonOnBlueContainerDark` |

### 2.2 Secondary — Revitalising Teal (M3-derived, seed `#006874`)

| Role | Light | Dark | Named Constant |
|---|---|---|---|
| `secondary` | `#006874` | `#4FD8EB` | `HorizonTeal` / `HorizonTealDark` |
| `secondaryContainer` | `#97F0FF` | `#004F58` | `HorizonTealContainer` / `HorizonTealContainerDark` |
| `onSecondary` | `#FFFFFF` | `#00363D` | `HorizonOnTeal` / `HorizonOnTealDark` |
| `onSecondaryContainer` | `#001F24` | `#97F0FF` | `HorizonOnTealContainer` / `HorizonOnTealContainerDark` |

### 2.3 Secondary Fixed (Tone-Stable — PassportChip)

These roles are **identical in light and dark** by M3 design, used for the Passport Chip component:

| Role | Hex | Named Constant |
|---|---|---|
| `secondaryFixed` | `#97F0FF` (T90) | `HorizonSecondaryFixed` |
| `secondaryFixedDim` | `#4FD8EB` (T80) | `HorizonSecondaryFixedDim` |
| `onSecondaryFixed` | `#001F24` (T10) | `HorizonOnSecondaryFixed` |
| `onSecondaryFixedVariant` | `#004F58` (T30) | `HorizonOnSecondaryFixedVariant` |

### 2.4 Tertiary — Warm Amber

| Role | Light | Dark |
|---|---|---|
| `tertiary` | `#795900` | `#F2BF48` |
| `tertiaryContainer` | `#FFDEA0` | `#5C4000` |

### 2.5 Surfaces — Light Mode (Tonal Layering Stack)

| Level | Role | Hex | Purpose |
|---|---|---|---|
| **0 (Foundation)** | `surface` / `background` | `#F9F9FF` | Base canvas |
| **1 (Sections)** | `surfaceContainerLow` | `#F2F3FB` | Large layout blocks, page backgrounds |
| **2 (Interaction)** | `surfaceContainer` | `#ECEDF6` | Secondary cards, grouping |
| **Step** | `surfaceContainerHigh` | `#E6E7F0` | M3-derived step |
| **Step** | `surfaceContainerHighest` | `#E1E2EA` | High-density UI elements |
| **3 (Prominence)** | `surfaceContainerLowest` | `#FFFFFF` | Primary cards that "pop" via tonal contrast |

### 2.6 Surfaces — Dark Mode (Inverted Hierarchy)

The surface stack reverses — lighter tones "lift" content from a deep, near-black foundation:

| Level | Role | Hex | Purpose |
|---|---|---|---|
| **0 (Foundation)** | `surface` / `background` | `#111318` | Deep near-black base (blue tint) |
| **1** | `surfaceContainerLow` | `#191C21` | Large layout blocks |
| **2** | `surfaceContainer` | `#1D2025` | Secondary cards |
| **Higher** | `surfaceContainerHigh` | `#282A2F` | Higher prominence |
| **Highest** | `surfaceContainerHighest` | `#32353A` | Primary cards that "pop" in dark mode |
| **Deepest** | `surfaceContainerLowest` | `#0C0E12` | Below-surface elements |

### 2.7 Text & Surface Content

| Role | Light | Dark | Rule |
|---|---|---|---|
| `onSurface` | `#191C21` | `#E2E2E9` | **Never** pure `#000000` or `#FFFFFF` |
| `onSurfaceVariant` | `#41484F` | `#C5C6D0` | Secondary text |
| `outline` | `#71787E` | `#8B9198` | Ghost border focus state |
| `outlineVariant` | `#C2C6D4` | `#44464F` | Ghost border fallback |

### 2.8 Error

| Role | Light | Dark |
|---|---|---|
| `error` | `#BA1A1A` | `#FFB4AB` |
| `errorContainer` | `#FFDAD6` | `#93000A` |
| `onError` | `#FFFFFF` | `#690005` |
| `onErrorContainer` | `#410002` | `#FFDAD6` |

### 2.9 Color Rules

- **`dynamicColor = false`** — The Horizon brand identity is always enforced; Material You wallpaper-based colors are not used.
- **No pure black text** — `onSurface` is `#191C21` (light) or `#E2E2E9` (dark) to maintain the "Premium" feel.
- **No pure white body text** — Dark mode uses soft light-grey to avoid eye strain.
- **Minimum 4.5:1 contrast ratio** — All `onSurface` / `onSurfaceVariant` text against `surfaceContainer` tiers.

> **Implementation:** `Color.kt` + `Theme.kt` in `core/design-system/.../foundation/`

---

## 3. Typography: The Dual-Voice System

Typography is a dialogue between **inspiration** and **information** — large, energetic headers capture the "dream," while clean, functional body text facilitates the "doing."

### 3.1 Plus Jakarta Sans — "Energetic Accents"

Used for **Display** and **Headline** roles. Tight letter-spacing (`-0.02em`) creates an optimistic, bold editorial feel.

| Role | Weight | Size | Line Height | Letter Spacing |
|---|---|---|---|---|
| `displayLarge` | Bold | 57 sp | 64 sp | −1.14 sp |
| `displayMedium` | Bold | 45 sp | 52 sp | −0.90 sp |
| `displaySmall` | Bold | 36 sp | 44 sp | −0.72 sp |
| `headlineLarge` | Bold | 32 sp | 40 sp | −0.64 sp |
| `headlineMedium` | Bold | 28 sp | 36 sp | −0.56 sp |
| `headlineSmall` | Bold | 24 sp | 32 sp | −0.48 sp |

### 3.2 Manrope — "Functional Grounding"

Used for **Title**, **Body**, and **Label** roles. Provides clarity for travel logistics — expense amounts, currency codes, participant names.

| Role | Weight | Size | Line Height | Letter Spacing |
|---|---|---|---|---|
| `titleLarge` | Medium | 22 sp | 28 sp | 0 sp |
| `titleMedium` | Medium | 16 sp | 24 sp | 0.15 sp |
| `titleSmall` | Medium | 14 sp | 20 sp | 0.10 sp |
| `bodyLarge` | Normal | 16 sp | 24 sp | 0.50 sp |
| `bodyMedium` | Normal | 14 sp | 20 sp | 0.25 sp |
| `bodySmall` | Normal | 12 sp | 16 sp | 0.40 sp |
| `labelLarge` | SemiBold | 14 sp | 20 sp | 0.10 sp |
| `labelMedium` | Medium | 12 sp | 16 sp | 0.50 sp |
| `labelSmall` | Medium | 11 sp | 16 sp | 0.50 sp |

### 3.3 Semantic Text Wrappers

To enforce the dual-voice system without requiring per-call-site `style`/`fontWeight` arguments, a set of **semantic text wrapper composables** is provided in `core/design-system/.../component/text/TextComponents.kt`. Each wrapper fixes the correct `TextStyle`, `FontWeight`, and default `Color` for its semantic role:

| Wrapper | Base Style | Typeface | Default Weight | Default Color |
|---|---|---|---|---|
| `ScreenTitleText` | `headlineLarge` (32 sp) | Plus Jakarta Sans | Bold | `onBackground` |
| `SectionHeadingText` | `titleMedium` (16 sp) | Manrope | Bold | `onSurface` |
| `CardTitleText` | `titleSmall` (14 sp) | Manrope | SemiBold | `onSurface` |
| `BodyText` | `bodyMedium` (14 sp) | Manrope | Normal | `onSurface` |
| `SecondaryBodyText` | `bodySmall` (12 sp) | Manrope | Normal | `onSurfaceVariant` |
| `LabelText` | `labelLarge` (14 sp) | Manrope | SemiBold | `onSurface` |
| `CaptionText` | `labelSmall` (11 sp) | Manrope | Medium | `onSurfaceVariant` |
| `AmountText` | `titleMedium` (16 sp) | Manrope | Bold | `onSurface` (overridable) |

**Semantic contract rules:**
* Callers **cannot** pass `style` or `fontWeight` — those are sealed inside each wrapper.
* An optional `color` parameter allows per-call-site overrides for semantic states (e.g., `MaterialTheme.colorScheme.error` for `AmountText` on negative balances, `MaterialTheme.colorScheme.primary` for highlighted values).
* A `maxLines` parameter is available on all wrappers for truncation. The default varies by context: `CardTitleText`, `SecondaryBodyText`, `LabelText`, `CaptionText`, and `AmountText` default to `maxLines = 1`; `ScreenTitleText`, `SectionHeadingText`, and `BodyText` default to unlimited.

**When to use each wrapper:**

| Wrapper | Typical usage |
|---|---|
| `ScreenTitleText` | Hero headline on an onboarding or empty-state screen |
| `SectionHeadingText` | Label above a group of related cards in a list |
| `CardTitleText` | Primary name/title row inside a `FlatCard` |
| `BodyText` | Descriptive copy, notes, paragraph text |
| `SecondaryBodyText` | Date, member count, status — secondary metadata |
| `LabelText` | Form field label, tab label, action label |
| `CaptionText` | Timestamp, sync status, helper text below a field |
| `AmountText` | Any monetary amount; pass `color = error` for negative balances |

**Example:**
```kotlin
// ✅ Correct — semantic wrapper with explicit error colour for negative balance
AmountText(
    text = amountFormatted,
    color = if (isNegative) MaterialTheme.colorScheme.error else Color.Unspecified,
)

// ✅ Correct — section label
SectionHeadingText(text = stringResource(R.string.balances_section_title))

// ❌ Avoid — raw Text with ad-hoc style arguments
Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onSurface,
)
```

> **Implementation:** `TextComponents.kt` in `core/design-system/.../component/text/`

### 3.4 Tabular Numerals

All text styles include the `tnum` OpenType feature for **tabular (monospaced) numerals**. This ensures:
- Decimal separators and currency digits align vertically in lists.
- The `AnimatedAmount` rolling animation renders without layout jitter.

### 3.5 Font Licensing

Both typefaces are licensed under the **SIL Open Font License 1.1**:
- `licenses/Plus_Jakarta_Sans_OFL.txt`
- `licenses/Manrope_OFL.txt`

> **Implementation:** `Typography.kt` in `core/design-system/.../foundation/`

---

## 4. Surface Philosophy & Depth

### 4.1 The "No-Line" Rule

**Borders are a relic of the past.** In this system, 1px solid borders for sectioning are strictly prohibited. Boundaries are defined through:

- **Background shifts:** Transitioning from `surface` to `surfaceContainerLow`.
- **Tonal transitions:** Using `surfaceContainerHighest` for high-density UI elements against a `surfaceBright` background.

### 4.2 The Layering Principle

Depth is achieved by "stacking" — a `surfaceContainerLow` card placed on the off-white page background (`surface` / `background` token, `#F9F9FF`) creates a subtle tonal inset with no border needed. Cards feel grounded and slightly recessed into the page rather than floating above it as bright white panels.

```
┌─────────────────────────────────────────────┐
│  Page Background: surface (#F9F9FF)         │
│  ┌───────────────────────────────────────┐  │
│  │  FlatCard: surfaceContainerLow        │  │
│  │  (tinted inset — no border)           │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

**Dark mode** inverts the hierarchy — lighter tones lift content from the near-black foundation:

```
┌─────────────────────────────────────────────┐
│  Page Background: surface (#111318)         │
│  ┌───────────────────────────────────────┐  │
│  │  FlatCard: surfaceContainerLow        │  │
│  │  (#191C21 — lighter pop from bg)      │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

### 4.3 The "Ghost Border" Fallback

When a container sits on an identical colour (e.g., dark mode where tonal differences are smaller), a subtle ghost border is used:

- **Light mode:** `outlineVariant` at **15% opacity**
- **Dark mode:** `outlineVariant` at **22% opacity**
- **Never** use 100% opacity for borders — the border should whisper, not shout

In `FlatCard`, this is available via an opt-in `ghostBorder = true` parameter, reserved for edge cases only.

### 4.4 Ambient Shadows

For floating elements (FAB, navigation bar, gradient buttons), use ultra-diffused shadows that feel like ambient light rather than dark smudges:

- **Light mode:** Standard elevation shadow via `Modifier.shadow()`.
- **Dark mode:** Ambient shadows are invisible. Replace shadow-based depth with **tonal layering only**. For critical floating elements, a faint luminous glow is used.

> **Implementation:** `FlatCard.kt`, `SectionCard.kt` in `core/design-system/.../component/layout/`

#### Hero Card Shadow — `FlatCard(elevation = …)`

For hero / featured cards that need to visually "float" above the surrounding content, pass a non-zero `elevation` to `FlatCard`. The shadow is handled internally: when `elevation > 0.dp`, `FlatCard` wraps its `Surface` in an **unclipped `Box` with `Modifier.shadow()`**, ensuring the ambient shadow renders outside the card bounds even when the caller's modifier includes a `clip()` for ripple effects.

```kotlin
// Hero card with ambient shadow
FlatCard(
    modifier = Modifier.fillMaxWidth().clip(cardShape).combinedClickable(…),
    elevation = 8.dp   // light mode: shadow rendered; dark mode: suppressed automatically
) {
    // card content
}
```

The shadow `Box` is the parent of the `Surface`; the caller's `modifier` (including `clip()` and `combinedClickable()`) is forwarded to the `Surface`, not to the shadow `Box`. This means:
- The ripple from `combinedClickable()` is still correctly clipped to the card shape (by `clip()` on the `Surface`).
- The shadow is drawn in the shadow `Box`'s parent draw scope — outside any clip context — so it is fully visible.

#### Elevated Hero Cards in `LazyColumn` — The Alpha-Buffer Rule

When a hero card using `FlatCard(elevation > 0)` is placed inside a `LazyColumn` with `animateItem()`, you **must** disable the default alpha fade animations:

```kotlin
SelectedGroupCard(
    modifier = Modifier
        .animateItem(fadeInSpec = null, fadeOutSpec = null)  // ← required
        …
)
```

**Why:** `animateItem()`'s default fade-in/out creates a **rectangular offscreen hardware buffer** (Android's alpha compositing layer). `FlatCard`'s `graphicsLayer { clip = false }` shadow bleeds outside the card bounds — but that bleed is silently clipped by the rectangular buffer edge, producing a hard squared-shadow artifact. Disabling the alpha animations eliminates the buffer entirely; the spring placement animation is retained and unaffected.

`FlatCard` uses `graphicsLayer { shape = shapes.large; clip = false }` internally, so the shadow always follows the card's rounded shape regardless of card size or transition frame. No external elevation animation or `isTransitionActive` kill-switch is needed.

> **Reference:** `SelectedGroupCard.kt` in `features/groups/.../component/`, `GroupsScreen.kt` (`animateItem(fadeInSpec = null, fadeOutSpec = null)` call-site)

---

## 5. Components

### 5.1 Buttons — The Three-Tier System

| Tier | Component | Style | Usage |
|---|---|---|---|
| **Primary** | `GradientButton` | Linear gradient (`primary` → `primaryContainer`), `CircleShape`, `titleSmall` Bold | Main form submits, hero CTAs |
| **Secondary** | `SecondaryButton` | `surfaceContainerHigh` fill, `CircleShape`, same height as gradient | Supporting actions (Back, Cancel) |
| **Destructive** | `DestructiveButton` | Ghost style, `error` text | Delete, leave actions |

**`GradientButton` details:**
- Full-pill shape (`CircleShape`) for the "Welcoming" brand pillar.
- Loading state: replaces label with `CircularProgressIndicator` in `onPrimary` colour.
- Disabled state: reduced opacity on gradient background.
- Built as `Box` with `clickable` + gradient `Modifier.background` (not Material `Button`) to support brush fills.
- Supports `leadingIcon` and `trailingIcon`.
- Three colour tiers: `GradientButtonDefaults.primaryColors()`, `.secondaryColors()`, `.tertiaryColors()`.

**`FormSubmitButton`** delegates to `GradientButton` and handles keyboard-aware bottom padding.

> **Implementation:** `GradientButton.kt`, `SecondaryButton.kt`, `DestructiveButton.kt`, `FormSubmitButton.kt` in `core/design-system/.../component/form/`

### 5.2 Cards

- **`FlatCard`** — Standard card wrapper. Zero elevation, `surfaceContainerLow` background (inset tier — slightly tinted relative to the off-white page background in light mode; lighter than the near-black background in dark mode), `shapes.large` corners, no border by default.
- **`SectionCard`** — Card with a section title header, built on `FlatCard`.
- **Forbidden:** Raw `Surface(…)` with manual `BorderStroke`/`color`/`shape` for card containers. Always use `FlatCard`.
- **Forbidden:** Divider lines between list items. Use 16dp or 24dp vertical spacing instead.

### 5.3 Inputs — The "Soft Field"

All text inputs use the **Soft Field** pattern:

- **Background:** `surfaceContainerLow` — fields look like soft, borderless containers.
- **Border at rest:** Fully transparent — no visible outline.
- **Border on focus:** `outline` at **20% opacity** — a subtle ghost border on all four sides.
- **Border in error:** Full `error` colour — always visible.

`StyledOutlinedTextField` wraps `OutlinedTextField` with `softFieldColors()` defaults and is the **only** field component used across the app (13+ call-sites).

> **Implementation:** `StyledOutlinedTextField.kt` in `core/design-system/.../component/input/`

> ⚠️ **Layering constraint:** Do **not** nest `StyledOutlinedTextField` (or any component using `softFieldColors()`) inside a `SectionCard` or `FlatCard`. Both the card and the field use `surfaceContainerLow` as their background — this makes the field indistinguishable from the card surface at rest (transparent border + identical background = zero contrast).
>
> Fields must sit directly on the `surface` page background to achieve the tonal contrast the Soft Field pattern requires:
>
> ```
> Page background: surface (#F9F9FF)
>   └─ StyledOutlinedTextField: surfaceContainerLow  ← visible contrast ✅
> ```
>
> **In wizard write-flows:** Place `StyledOutlinedTextField` directly inside `WizardStepLayout { }`. If a section title is needed, render it as a standalone `Text(style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)` above the field — **do not** wrap the pair in a `SectionCard`.
>
> `SectionCard` remains correct for grouping **read-only** content (e.g., review/summary steps) where the card background provides helpful visual grouping for plain `Text` rows.

### 5.4 Passport Chip — Signature Travel Component

A signature chip designed to feel like a **collectible travel stamp**:

| State | Background | Text |
|---|---|---|
| **Selected** | `secondaryFixedDim` (`#4FD8EB`) | `onSecondaryFixed` (`#001F24`) |
| **Unselected** | `surfaceContainerHigh` | `onSurfaceVariant` |

- **Shape:** `CircleShape` (full pill).
- **No border** in any state — colour shift defines the selected state (No-Line rule).
- Both `secondaryFixedDim` and `onSecondaryFixed` are tone-stable (same in light and dark by M3 design).

Used across: `CategoryChips`, `PaymentMethodChips`, `PaymentStatusChips`, `CondensedChips`, `AddOnItemEditor`, `SearchableChipSelector`, `AsyncSearchableChipSelector`.

> **Implementation:** `PassportChip.kt` in `core/design-system/.../component/chip/`

### 5.5 Glassmorphism — Atmospheric Glass-Blur

Floating UI elements use a glass-blur effect to evoke the atmospheric quality of travel (clouds, water, horizons):

| Mode | Surface Opacity | Blur Radius |
|---|---|---|
| **Light** | 70% | 20 dp |
| **Dark** | 60% | 24 dp |

The dark mode recipe uses slightly lower opacity and higher blur to compensate for reduced tonal contrast.

```kotlin
// Any floating element
Box(modifier = Modifier.horizonGlassEffect(hazeState = hazeState))

// With layout-specific customisation (bottom bar gradient mask)
Box(
    modifier = Modifier.horizonGlassEffect(hazeState = hazeState) {
        mask = Brush.verticalGradient(...)
    }
)
```

Powered by the `dev.chrisbanes.haze` library. Constants and the `Modifier.horizonGlassEffect()` extension live in `GlassmorphismDefaults`.

> **Implementation:** `GlassmorphismDefaults.kt` in `core/design-system/.../foundation/`

---

## 6. Spacing & Whitespace

Travel is about breathing room — the interface must never feel claustrophobic.

| Context | Spacing |
|---|---|
| Between major screen sections | 24–48 dp |
| Within cards (internal padding) | 16–20 dp |
| Between list items | 16–24 dp (NO divider lines) |
| Card corner radius | `shapes.large` (default) |
| Page margins | 16–24 dp (never full-width edge-to-edge) |

---

## 7. Dark Mode

The Horizon Narrative must feel **equally premium** in dark mode. The same principles apply — tonal layering over borders, ambient shadows over hard edges — but inverted for dark surfaces.

### Key Differences

| Aspect | Light Mode | Dark Mode |
|---|---|---|
| **Card surface tier** | `surfaceContainerLow` (#F2F3FB) | `surfaceContainerLow` (#191C21) |
| **Ghost border opacity** | 15% | 22% |
| **Glass blur radius** | 20 dp | 24 dp |
| **Glass surface opacity** | 70% | 60% |
| **Shadows** | Standard elevation | Tonal layering only (shadows invisible) |
| **Text colour** | `#191C21` (not #000) | `#E2E2E9` (not #FFF) |
| **Gradient CTAs** | `primary → primaryContainer` | Same (derives from theme tokens automatically) |

---

## 8. Do's and Don'ts

### ✅ Do:

- **Embrace tonal depth** — Always check if a background colour shift can replace a line or a shadow.
- **Maximise whitespace** — Give content 32–48 dp of "air" between major sections.
- **Use theme tokens** — All colours come from `MaterialTheme.colorScheme`, never hardcoded hex values.
- **Use `FlatCard`** — For all card-like containers. Never raw `Surface(…)` with manual borders.
- **Use `PassportChip`** — For all selectable tags/filters. Never stock `FilterChip`/`InputChip`.
- **Use `GradientButton`** — For all primary CTAs. Never flat `Button(…)`.
- **Use `StyledOutlinedTextField`** — For all text inputs. Never raw `OutlinedTextField`.
- **Test both themes** — Use `@PreviewThemes` (light/dark) for all component previews.

### ❌ Don't:

- **Don't use pure black** — Use `onSurface` (`#191C21`) for text.
- **Don't use pure white body text** — Use `onSurface` (`#E2E2E9`) in dark mode.
- **Don't use 1px borders** — They create visual noise and make the UI feel like a spreadsheet.
- **Don't crowd margins** — Avoid "Full Width" containers; use 16–24 dp internal padding.
- **Don't use hard shadows** — Ambient, diffused shadows only for floating elements.
- **Don't use divider lines** — Vertical spacing separates list items, not `Divider()`.

---

## 9. Accessibility

- **Minimum 4.5:1 contrast ratio** for all `onSurface` / `onSurfaceVariant` text against `surfaceContainer` tiers.
- **Error states** use the `error` token sparingly, always accompanied by descriptive text (`labelSmall`).
- **Dark mode errors** use the theme's dark-mode `error` token — never hardcoded hex — to maintain proper contrast against dark containers.
- **Tabular numerals** (`tnum` feature) ensure currency amounts remain readable and aligned in lists.

---

## 10. Implementation Summary

The Horizon Narrative was implemented across 7 focused sub-issues:

| Sub-Issue | Scope | Files |
|---|---|---|
| [#880](https://github.com/pedrazamiguez/split-trip/issues/880) | Colour scheme — replace Ocean Voyage with Horizon palette | `Color.kt`, `Theme.kt` |
| [#881](https://github.com/pedrazamiguez/split-trip/issues/881) | FlatCard — remove 1dp border, adopt tonal layering | `FlatCard.kt`, `SectionCard.kt` |
| [#882](https://github.com/pedrazamiguez/split-trip/issues/882) | Inputs — migrate to "Soft Field" filled style | `StyledOutlinedTextField.kt` |
| [#883](https://github.com/pedrazamiguez/split-trip/issues/883) | Buttons — gradient primary CTA | `GradientButton.kt`, `FormSubmitButton.kt`, `SecondaryButton.kt` |
| [#884](https://github.com/pedrazamiguez/split-trip/issues/884) | Glassmorphism — extract reusable utility | `GlassmorphismDefaults.kt`, `BottomNavigationBar.kt` |
| [#885](https://github.com/pedrazamiguez/split-trip/issues/885) | Passport Chip — signature chip component | `PassportChip.kt`, chip call-sites across features |
| [#886](https://github.com/pedrazamiguez/split-trip/issues/886) | Screen compliance audit — surfaces, whitespace, tonal depth | 16+ feature files |

### Foundation Files

| File | Location | Purpose |
|---|---|---|
| `Color.kt` | `core/design-system/.../foundation/` | All `Horizon*` named colour constants (93 values) |
| `Theme.kt` | `core/design-system/.../foundation/` | `LightColorScheme` + `DarkColorScheme` from named constants |
| `Typography.kt` | `core/design-system/.../foundation/` | Dual-voice type scale (Plus Jakarta Sans + Manrope) |
| `GlassmorphismDefaults.kt` | `core/design-system/.../foundation/` | Glass-blur constants + `Modifier.horizonGlassEffect()` |

### Component Files

| File | Location | Purpose |
|---|---|---|
| `GradientButton.kt` | `core/design-system/.../component/form/` | Gradient CTA with Box-based shadow and loading state |
| `SecondaryButton.kt` | `core/design-system/.../component/form/` | Flat neutral secondary button |
| `DestructiveButton.kt` | `core/design-system/.../component/form/` | Ghost-style destructive action button |
| `FormSubmitButton.kt` | `core/design-system/.../component/form/` | Keyboard-aware pinned submit (delegates to `GradientButton`) |
| `ButtonContentRow.kt` | `core/design-system/.../component/form/` | Shared label + icon row for all button tiers |
| `FlatCard.kt` | `core/design-system/.../component/layout/` | Standard card wrapper with tonal layering |
| `SectionCard.kt` | `core/design-system/.../component/layout/` | Titled card section (built on `FlatCard`) |
| `PassportChip.kt` | `core/design-system/.../component/chip/` | Signature travel chip with tone-stable colours |
| `StyledOutlinedTextField.kt` | `core/design-system/.../component/input/` | Soft Field text input with `softFieldColors()` |
| `StickyActionBar.kt` | `core/design-system/.../component/scaffold/` | Full-width bottom CTA bar (delegates to `GradientButton`) |
| `TextComponents.kt` | `core/design-system/.../component/text/` | Semantic text wrappers (§3.3) |

---

## 11. Preview Gallery

All Horizon Narrative components include `@PreviewThemes` previews (light + dark) in `src/debug`:

| Preview File | Components |
|---|---|
| `ThemePreviews.kt` | Full Horizon colour palette swatch |
| `FlatCardPreviews.kt` | Default card, ghost border variant, `SectionCard` |
| `GradientButtonPreviews.kt` | Enabled, disabled, loading states |
| `PassportChipPreviews.kt` | Selected/unselected, removable, overflow variants |
| `TextComponentsPreviews.kt` | All semantic text wrappers (gallery + per-wrapper light/dark + locale variants) |

