---
trigger: always_on
description: Enforce Horizon Narrative UI/UX rules (Soft Field layering, IME keyboard handling, debounced clicks, and semantic tags).
---

## Horizon UI/UX Rules

1. **Soft Field Layering (Never Nest Inputs Inside Cards):**
   - Never wrap `StyledOutlinedTextField` (or any input/dropdown trigger using `softFieldColors()`) inside `FlatCard` or `SectionCard`. Both use `surfaceContainerLow` with a transparent rest border, so the field blends into the card.
   - Place `StyledOutlinedTextField` directly on the foundation `surface` canvas with a standalone `SectionHeadingText` above it (see `docs/design-system/horizon-narrative-design.md` §5.3 & §12.3).
   - **Expandable container exception:** When inputs must live inside an expandable card (e.g., `AddOnItemCard`), elevate the card container to `surfaceContainerLowest` (light) / `surfaceContainerHighest` (dark) with `ghostBorder = true`.

2. **Keyboard / IME Handling:**
   - `FeatureScaffold` excludes `WindowInsets.ime`. Any scrollable screen or tab containing text inputs MUST apply `.imePadding()` on the scrollable container and overlay fixed bottom navigation in a `Box` (so the keyboard slides over bottom chrome and inputs stay visible on small screens).

3. **Debounced Clicks & Semantic Tags:**
   - Always use `Modifier.debouncedClickable` / `Modifier.debouncedCombinedClickable` and `PassportTag` with semantic `PassportTagStyle` (`INFO`, `SUCCESS`, `WARNING`, `ERROR`, `NEUTRAL`, `PRIMARY`) — never `secondaryContainer` or bright cyan on read-only pills.
