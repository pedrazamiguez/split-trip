package es.pedrazamiguez.splittrip.core.designsystem.foundation

import androidx.compose.ui.graphics.Color

// =============================================================================
// Horizon Narrative Palette
// Rooted in deep trustworthy blues (primary) and revitalizing teals (secondary),
// balanced by a warm off-white foundation. See issue #877 for full spec.
// =============================================================================

// --- Primary: Horizon Blue (Trust, Depth, Horizon) ---
val HorizonBlue = Color(0xFF00478D) // Light: primary
val HorizonBlueContainer = Color(0xFF005EB8) // Light: primaryContainer
val HorizonOnBlue = Color(0xFFFFFFFF) // Light: onPrimary
val HorizonOnBlueContainer = Color(0xFFFFFFFF) // Light: onPrimaryContainer
val HorizonBlueDark = Color(0xFF9ECAFF) // Dark: primary (M3 tone-80)
val HorizonOnBlueDark = Color(0xFF00315A) // Dark: onPrimary (tone-20)
val HorizonBlueContainerDark = Color(0xFF00478D) // Dark: primaryContainer (tone-30, reuses HorizonBlue)
val HorizonOnBlueContainerDark = Color(0xFFD1E4FF) // Dark: onPrimaryContainer (tone-90)
val HorizonInversePrimary = Color(0xFF9ECAFF) // Light: inversePrimary

// --- Secondary: Revitalizing Teal (M3-derived, seed #006874) ---
val HorizonTeal = Color(0xFF006874) // Light: secondary
val HorizonTealContainer = Color(0xFF97F0FF) // Light: secondaryContainer
val HorizonOnTeal = Color(0xFFFFFFFF) // Light: onSecondary
val HorizonOnTealContainer = Color(0xFF001F24) // Light: onSecondaryContainer
val HorizonTealDark = Color(0xFF4FD8EB) // Dark: secondary (tone-80)
val HorizonTealContainerDark = Color(0xFF004F58) // Dark: secondaryContainer (tone-30)
val HorizonOnTealDark = Color(0xFF00363D) // Dark: onSecondary (tone-20)
val HorizonOnTealContainerDark = Color(0xFF97F0FF) // Dark: onSecondaryContainer (tone-90)

// --- Tertiary: Warm Amber Accent (M3-derived, seed #7C5800) ---
val HorizonAmber = Color(0xFF7C5800) // Light: tertiary
val HorizonAmberContainer = Color(0xFFFFDFA0) // Light: tertiaryContainer
val HorizonOnAmber = Color(0xFFFFFFFF) // Light: onTertiary
val HorizonOnAmberContainer = Color(0xFF261900) // Light: onTertiaryContainer
val HorizonAmberDark = Color(0xFFFABD00) // Dark: tertiary (tone-80)
val HorizonAmberContainerDark = Color(0xFF5C4000) // Dark: tertiaryContainer (tone-30)
val HorizonOnAmberDark = Color(0xFF412D00) // Dark: onTertiary (tone-20)
val HorizonOnAmberContainerDark = Color(0xFFFFDFA0) // Dark: onTertiaryContainer (tone-90)

// --- Surfaces — Light Mode ---
val HorizonSurface = Color(0xFFF9F9FF) // Light: surface / background
val HorizonOnSurface = Color(0xFF191C21) // Light: onSurface / onBackground (not pure black)
val HorizonSurfaceVariant = Color(0xFFDDE3F0) // Light: surfaceVariant
val HorizonOnSurfaceVariant = Color(0xFF41484F) // Light: onSurfaceVariant
val HorizonSurfaceContainerLowest = Color(0xFFFFFFFF) // Level 3 (pop)
val HorizonSurfaceContainerLow = Color(0xFFF2F3FB) // Level 1 (large layout blocks)
val HorizonSurfaceContainer = Color(0xFFECEDF6) // Level 2 (secondary cards)
val HorizonSurfaceContainerHigh = Color(0xFFE6E7F0) // M3-derived step
val HorizonSurfaceContainerHighest = Color(0xFFE1E2EA) // M3-derived step
val HorizonInverseSurface = Color(0xFF2E3036) // Light: inverseSurface
val HorizonInverseOnSurface = Color(0xFFF0F0F8) // Light: inverseOnSurface

// --- Surfaces — Dark Mode (lighter tones lift content from near-black foundation) ---
val HorizonSurfaceDark = Color(0xFF111318) // Dark: surface / background (deep near-black, blue tint)
val HorizonOnSurfaceDark = Color(0xFFE2E2E9) // Dark: onSurface (soft light-grey, not pure white)
val HorizonSurfaceVariantDark = Color(0xFF44464F) // Dark: surfaceVariant
val HorizonOnSurfaceVariantDark = Color(0xFFC5C6D0) // Dark: onSurfaceVariant
val HorizonSurfaceContainerLowestDark = Color(0xFF0C0E12) // Deepest (below surface)
val HorizonSurfaceContainerLowDark = Color(0xFF191C21) // Large layout blocks
val HorizonSurfaceContainerDark = Color(0xFF1D2025) // Dark: surfaceContainer (secondary cards)
val HorizonSurfaceContainerHighDark = Color(0xFF282A2F) // Higher prominence
val HorizonSurfaceContainerHighestDark = Color(0xFF32353A) // Highest prominence (pop in dark)
val HorizonInverseSurfaceDark = Color(0xFFE2E2E9) // Dark: inverseSurface (light tone shown on a dark background)
val HorizonInverseOnSurfaceDark = Color(0xFF2E3036) // Dark: inverseOnSurface (dark tone for content on inverse surface)

// --- Outline ---
val HorizonOutline = Color(0xFF71787E) // Light: outline
val HorizonOutlineVariant = Color(0xFFC2C6D4) // Light: outlineVariant
val HorizonOutlineDark = Color(0xFF8B9198) // Dark: outline
val HorizonOutlineVariantDark = Color(0xFF44464F) // Dark: outlineVariant

// --- Error ---
val HorizonError = Color(0xFFBA1A1A) // Light: error
val HorizonErrorContainer = Color(0xFFFFDAD6) // Light: errorContainer (tone-90)
val HorizonOnError = Color(0xFFFFFFFF) // Light: onError
val HorizonOnErrorContainer = Color(0xFF410002) // Light: onErrorContainer
val HorizonErrorDark = Color(0xFFFFB4AB) // Dark: error (tone-80)
val HorizonErrorContainerDark = Color(0xFF93000A) // Dark: errorContainer (tone-30)
val HorizonOnErrorDark = Color(0xFF690005) // Dark: onError (tone-20)
val HorizonOnErrorContainerDark = Color(0xFFFFDAD6) // Dark: onErrorContainer (tone-90)

// --- Secondary Fixed (M3 tone-stable roles — same value in light and dark by design) ---
// Derived from the teal secondary seed (#006874). Used for the PassportChip selected state.
val HorizonSecondaryFixed = Color(0xFF97F0FF) // T90 — secondaryFixed
val HorizonSecondaryFixedDim = Color(0xFF4FD8EB) // T80 — secondaryFixedDim (selected chip bg)
val HorizonOnSecondaryFixed = Color(0xFF001F24) // T10 — onSecondaryFixed (selected chip text)
val HorizonOnSecondaryFixedVariant = Color(0xFF004F58) // T30 — onSecondaryFixedVariant

// --- Semantic Tag & Status Tonal Pairs (soft Horizon tonal containers) ---
val HorizonSuccessContainer = Color(0xFFD2F1DC) // Light: soft sage green (tone-90)
val HorizonOnSuccessContainer = Color(0xFF0A3D21) // Light: deep forest green (tone-10)
val HorizonSuccessContainerDark = Color(0xFF174D2E) // Dark: forest green (tone-30)
val HorizonOnSuccessContainerDark = Color(0xFFD2F1DC) // Dark: soft sage green (tone-90)

val HorizonInfoContainer = Color(0xFFD6E6FA) // Light: soft sky blue (tone-90)
val HorizonOnInfoContainer = Color(0xFF00315A) // Light: deep Horizon blue (tone-20)
val HorizonInfoContainerDark = Color(0xFF00478D) // Dark: Horizon blue (tone-30)
val HorizonOnInfoContainerDark = Color(0xFFD1E4FF) // Dark: soft sky blue (tone-90)

val HorizonTagSecondaryContainer = Color(0xFFD0ECEF) // Light: muted sea-glass teal
val HorizonTagSecondaryContainerDark = Color(0xFF004F58) // Dark: deep teal (tone-30)
val HorizonTagOnSecondaryContainerDark = Color(0xFFD0ECEF) // Dark: muted sea-glass teal

// --- Scrim ---
val HorizonScrim = Color(0xFF000000)

// --- Charts ---
val ChartColors = listOf(
    Color(0xFF3A7BD5), // Horizon Blue tint
    Color(0xFFE05555), // Warm Red
    Color(0xFFE8A838), // Amber
    Color(0xFF3DAA70), // Teal Green
    Color(0xFF9B59B6), // Violet
    Color(0xFFE67E22), // Orange
    Color(0xFF1ABC9C), // Emerald
    Color(0xFF2C3E50) // Dark Slate
)
