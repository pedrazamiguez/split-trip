// Thematic filename — groups SplitTripSpacing + MaterialTheme.spacing.
@file:Suppress("MatchingDeclarationName")

package es.pedrazamiguez.splittrip.core.designsystem.foundation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design-token spacing scale for the **Horizon Narrative** design system.
 *
 * Named levels replace ad-hoc hardcoded `dp` values across every screen and component,
 * providing a single source of truth for padding, gap, and margin decisions.
 *
 * ## Token → dp mapping (aligned with `wiki/horizon-narrative-design-language.md §6`)
 *
 * | Token         | dp  | Typical context                              |
 * |---------------|-----|----------------------------------------------|
 * | [None]        | 0   | No spacing (explicit zero for clarity)        |
 * | [ExtraSmall]  | 4   | Icon padding, dense chip gaps                 |
 * | [Small]       | 8   | Intra-row gaps, compact label offsets         |
 * | [Medium]      | 12  | Secondary row padding, tight card internals   |
 * | [Default]     | 16  | Card internal padding, page margins           |
 * | [Large]       | 20  | Generous card padding, top/bottom row spacing |
 * | [ExtraLarge]  | 24  | Section-to-section gaps, list item spacing    |
 * | [Section]     | 32  | Major section separators                      |
 * | [Screen]      | 48  | Hero-area breathing room, top padding         |
 *
 * ## Usage
 *
 * Prefer `MaterialTheme.spacing.*` inside Composables so the accessor is consistent
 * with other theme extensions:
 *
 * ```kotlin
 * Column(
 *     modifier = Modifier.padding(horizontal = MaterialTheme.spacing.Default),
 *     verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraLarge)
 * ) { … }
 * ```
 *
 * In non-Composable contexts (e.g. Modifier factory functions, layout tests) reference
 * [SplitTripSpacing] directly:
 *
 * ```kotlin
 * val padding = SplitTripSpacing.Default  // 16.dp
 * ```
 *
 * @see MaterialTheme.spacing
 */
object SplitTripSpacing {

    /** `0.dp` — Explicit zero; use instead of a bare `0.dp` for readability. */
    val None: Dp = 0.dp

    /** `4.dp` — Icon padding, dense chip gaps. */
    val ExtraSmall: Dp = 4.dp

    /** `8.dp` — Intra-row gaps, compact label offsets. */
    val Small: Dp = 8.dp

    /** `12.dp` — Secondary row padding, tight card internals. */
    val Medium: Dp = 12.dp

    /** `16.dp` — Card internal padding, page margins (default). */
    val Default: Dp = 16.dp

    /** `20.dp` — Generous card padding, top/bottom row spacing. */
    val Large: Dp = 20.dp

    /** `24.dp` — Section-to-section gaps, list item spacing. */
    val ExtraLarge: Dp = 24.dp

    /** `32.dp` — Major section separators. */
    val Section: Dp = 32.dp

    /** `48.dp` — Hero-area breathing room, large top / bottom screen padding. */
    val Screen: Dp = 48.dp
}

/**
 * Provides the [SplitTripSpacing] token object via a `MaterialTheme` extension property,
 * giving call sites the same ergonomic access pattern as `MaterialTheme.colorScheme`
 * or `MaterialTheme.typography`.
 *
 * The `@Composable` getter is intentional: it mirrors the M3 convention for theme
 * extensions and keeps the door open for future context-aware (e.g. responsive) variants
 * without a breaking API change.
 */
val MaterialTheme.spacing: SplitTripSpacing
    @Composable get() = SplitTripSpacing
