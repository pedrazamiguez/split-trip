package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Semantic text wrappers for the Horizon Narrative dual-voice typography system.
 *
 * Each wrapper encodes the correct `TextStyle`, [FontWeight], and default [Color] for its
 * semantic role. Callers cannot override `TextStyle` or [FontWeight] — those are fixed by the
 * semantic contract, ensuring typographic consistency across every screen without per-call-site
 * style arguments.
 *
 * An optional [color] parameter allows per-call-site color overrides for special cases
 * (e.g., error-coloured [AmountText] for negative balances).
 *
 * ### Typeface mapping (Horizon Narrative §3)
 * - **Screen titles** → `headlineLarge` → **Plus Jakarta Sans**
 * - **Section headings / card titles / amounts** → `titleMedium` / `titleSmall` → **Manrope**
 * - **Body / label / caption** → `bodyMedium` / `bodySmall` / `bodyLarge` / `labelLarge` / `labelSmall` → **Manrope**
 * - **Sheet / feature titles** → `titleLarge` → **Manrope**
 *
 * See `wiki/horizon-narrative-design-language.md §3.3` for full usage guidance.
 */

// ─── Private helpers ─────────────────────────────────────────────────────────

/**
 * Returns [fallback] when this colour is [Color.Unspecified], otherwise returns this colour.
 * Eliminates the repeated `if (color != Color.Unspecified) color else …` pattern across wrappers.
 */
private fun Color.orElse(fallback: Color): Color = if (this != Color.Unspecified) this else fallback

/**
 * Resolves the correct [TextOverflow] strategy for a given [maxLines] constraint.
 * When [maxLines] is unconstrained ([Int.MAX_VALUE]), content clips; otherwise it ellipsises.
 */
private fun overflowFor(maxLines: Int): TextOverflow =
    if (maxLines < Int.MAX_VALUE) TextOverflow.Ellipsis else TextOverflow.Clip

// ─── Display / Headline tier (Plus Jakarta Sans) ────────────────────────────

/**
 * Screen-level title. Maps to `headlineLarge` (32 sp, Plus Jakarta Sans Bold).
 *
 * Use for the primary hero title of a full-screen view (e.g., an onboarding headline
 * or the empty-state title on the groups screen).
 *
 * @param text       Text content to display.
 * @param modifier   Modifier applied to the underlying [Text].
 * @param color      Overrides the default `onBackground` colour. Useful for hero screens
 *                   where the title sits on a custom background.
 * @param maxLines   Defaults to unlimited. Pass a value to truncate with ellipsis.
 */
@Composable
fun ScreenTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.onBackground),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        maxLines = maxLines,
        overflow = overflowFor(maxLines),
        textAlign = textAlign
    )
}

// ─── Title tier (Manrope) ────────────────────────────────────────────────────

/**
 * Section or card group heading. Maps to `titleMedium` (16 sp, Manrope Bold).
 *
 * Use above a group of related cards or content blocks to label the section.
 *
 * @param text     Text content to display.
 * @param modifier Modifier applied to the underlying [Text].
 * @param color    Overrides the default `onSurface` colour.
 * @param maxLines Defaults to unlimited. Pass a value to truncate with ellipsis.
 */
@Composable
fun SectionHeadingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.onSurface),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = maxLines,
        overflow = overflowFor(maxLines)
    )
}

/**
 * Primary label inside a card (e.g., expense name, group name). Maps to `titleSmall`
 * (14 sp, Manrope SemiBold).
 *
 * Use for the main identifier text within a [FlatCard] row.
 *
 * @param text     Text content to display.
 * @param modifier Modifier applied to the underlying [Text].
 * @param color    Overrides the default `onSurface` colour.
 * @param maxLines Defaults to 1 with ellipsis, matching the typical single-line card title.
 */
@Composable
fun CardTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.onSurface),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = maxLines,
        overflow = overflowFor(maxLines)
    )
}

/**
 * Primary title for a bottom sheet, dialog, or prominent feature content block.
 * Maps to `titleLarge` (22 sp, Manrope Bold).
 *
 * Use above a sheet's action list, as the main title of a modal, or for a prominent
 * item name (e.g., expense title, selected group name) displayed at feature level.
 *
 * @param text     Text content to display.
 * @param modifier Modifier applied to the underlying [Text].
 * @param color    Overrides the default `onSurface` colour.
 * @param maxLines Defaults to unlimited. Pass a value to truncate with ellipsis.
 */
@Composable
fun SheetTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.onSurface),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        maxLines = maxLines,
        overflow = overflowFor(maxLines)
    )
}

/**
 * Sub-section label inside a card or form group. Maps to `titleSmall`
 * (14 sp, Manrope Bold) with a subdued `onSurfaceVariant` colour.
 *
 * Use for the heading of an input group, card sub-section, or picker panel where
 * the label needs weight but should be visually lighter than a primary [CardTitleText].
 *
 * @param text     Text content to display.
 * @param modifier Modifier applied to the underlying [Text].
 * @param color    Overrides the default `onSurfaceVariant` colour.
 * @param maxLines Defaults to 1 with ellipsis, matching the typical single-line label.
 */
@Composable
fun CardSectionLabelText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.onSurfaceVariant),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = maxLines,
        overflow = overflowFor(maxLines)
    )
}

// ─── Body tier (Manrope) ─────────────────────────────────────────────────────

/**
 * Standard body copy. Maps to `bodyMedium` (14 sp, Manrope Normal).
 *
 * Use for descriptive text, notes, and any paragraph-level content on a screen.
 *
 * @param text     Text content to display.
 * @param modifier Modifier applied to the underlying [Text].
 * @param color    Overrides the default `onSurface` colour.
 * @param maxLines Defaults to unlimited.
 */
@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.onSurface),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Normal,
        maxLines = maxLines,
        overflow = overflowFor(maxLines),
        textAlign = textAlign
    )
}

/**
 * Supporting body copy or metadata. Maps to `bodySmall` (12 sp, Manrope Normal).
 *
 * Use for secondary information within a card row (e.g., date, participant count)
 * or below a primary label.
 *
 * @param text     Text content to display.
 * @param modifier Modifier applied to the underlying [Text].
 * @param color    Overrides the default `onSurfaceVariant` colour.
 * @param maxLines Defaults to 1 with ellipsis.
 */
@Composable
fun SecondaryBodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.onSurfaceVariant),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Normal,
        maxLines = maxLines,
        overflow = overflowFor(maxLines)
    )
}

/**
 * Large body copy for prominent descriptive text. Maps to `bodyLarge` (16 sp, Manrope Normal).
 *
 * Use for primary labels in settings rows, profile attribute values, or any body content
 * that needs slightly more visual presence than standard [BodyText].
 *
 * @param text     Text content to display.
 * @param modifier Modifier applied to the underlying [Text].
 * @param color    Overrides the default `onSurface` colour.
 * @param maxLines Defaults to unlimited.
 */
@Composable
fun LargeBodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.onSurface),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Normal,
        maxLines = maxLines,
        overflow = overflowFor(maxLines)
    )
}

// ─── Label / Caption tier (Manrope) ─────────────────────────────────────────

/**
 * Prominent action or field label. Maps to `labelLarge` (14 sp, Manrope SemiBold).
 *
 * Use for form field labels, button-adjacent labels, and tab / nav item text where
 * emphasis is needed but the role is labelling rather than titling.
 *
 * @param text     Text content to display.
 * @param modifier Modifier applied to the underlying [Text].
 * @param color    Overrides the default `onSurface` colour.
 * @param maxLines Defaults to 1 with ellipsis.
 */
@Composable
fun LabelText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.onSurface),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = maxLines,
        overflow = overflowFor(maxLines)
    )
}

/**
 * De-emphasised micro-label. Maps to `labelSmall` (11 sp, Manrope Medium).
 *
 * Use for timestamps, status badges, helper text beneath form fields, and
 * any supplementary annotation that must be present but visually subordinate.
 *
 * @param text     Text content to display.
 * @param modifier Modifier applied to the underlying [Text].
 * @param color    Overrides the default `onSurfaceVariant` colour.
 * @param maxLines Defaults to 1 with ellipsis.
 */
@Composable
fun CaptionText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.onSurfaceVariant),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        maxLines = maxLines,
        overflow = overflowFor(maxLines)
    )
}

// ─── Amount tier (Manrope) ───────────────────────────────────────────────────

/**
 * Monetary amount display. Maps to `titleMedium` (16 sp, Manrope Bold).
 *
 * The `tnum` (tabular numerals) OpenType feature is already active on this style via the
 * app's `Typography.kt`, ensuring decimal separators and currency digits align in lists
 * (Horizon Narrative §3.4).
 *
 * The [color] parameter is intentionally prominent here — amounts frequently need
 * semantic colour (e.g., `MaterialTheme.colorScheme.error` for negative balances,
 * `MaterialTheme.colorScheme.primary` for the user's own share). Always pass the
 * appropriate colour explicitly at the call site when semantic meaning requires it.
 *
 * @param text     Formatted amount string (e.g., "€ 42.50"). Format with [AmountFormatter].
 * @param modifier Modifier applied to the underlying [Text].
 * @param color    Optional colour override. Defaults to `onSurface`; pass `error`,
 *                 `primary`, or any other theme token to convey semantic state.
 * @param maxLines Defaults to 1 with ellipsis — amounts should never wrap.
 */
@Composable
fun AmountText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier,
        color = color.orElse(MaterialTheme.colorScheme.onSurface),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = maxLines,
        overflow = overflowFor(maxLines)
    )
}
