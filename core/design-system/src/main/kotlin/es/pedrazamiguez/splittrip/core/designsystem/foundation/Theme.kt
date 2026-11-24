package es.pedrazamiguez.splittrip.core.designsystem.foundation

import android.os.Build
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalSharedTransitionScope

// Horizon — Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = HorizonBlueDark,
    onPrimary = HorizonOnBlueDark,
    primaryContainer = HorizonBlueContainerDark,
    onPrimaryContainer = HorizonOnBlueContainerDark,

    secondary = HorizonTealDark,
    onSecondary = HorizonOnTealDark,
    secondaryContainer = HorizonTealContainerDark,
    onSecondaryContainer = HorizonOnTealContainerDark,

    // Fixed roles: tone-stable, identical in light and dark (M3 spec) — used by PassportChip
    secondaryFixed = HorizonSecondaryFixed,
    secondaryFixedDim = HorizonSecondaryFixedDim,
    onSecondaryFixed = HorizonOnSecondaryFixed,
    onSecondaryFixedVariant = HorizonOnSecondaryFixedVariant,

    tertiary = HorizonAmberDark,
    onTertiary = HorizonOnAmberDark,
    tertiaryContainer = HorizonAmberContainerDark,
    onTertiaryContainer = HorizonOnAmberContainerDark,

    background = HorizonSurfaceDark,
    surface = HorizonSurfaceDark,
    onBackground = HorizonOnSurfaceDark,
    onSurface = HorizonOnSurfaceDark,

    // Surface container hierarchy — lighter tones lift content from the near-black foundation (§7)
    surfaceContainerLowest = HorizonSurfaceContainerLowestDark,
    surfaceContainerLow = HorizonSurfaceContainerLowDark,
    surfaceContainer = HorizonSurfaceContainerDark,
    surfaceContainerHigh = HorizonSurfaceContainerHighDark,
    surfaceContainerHighest = HorizonSurfaceContainerHighestDark,

    surfaceVariant = HorizonSurfaceVariantDark,
    onSurfaceVariant = HorizonOnSurfaceVariantDark,

    inverseSurface = HorizonInverseSurfaceDark,
    inverseOnSurface = HorizonInverseOnSurfaceDark,
    inversePrimary = HorizonBlue,

    outline = HorizonOutlineDark,
    outlineVariant = HorizonOutlineVariantDark,

    error = HorizonErrorDark,
    onError = HorizonOnErrorDark,
    errorContainer = HorizonErrorContainerDark,
    onErrorContainer = HorizonOnErrorContainerDark,

    scrim = HorizonScrim
)

// Horizon — Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = HorizonBlue,
    onPrimary = HorizonOnBlue,
    primaryContainer = HorizonBlueContainer,
    onPrimaryContainer = HorizonOnBlueContainer,

    secondary = HorizonTeal,
    onSecondary = HorizonOnTeal,
    secondaryContainer = HorizonTealContainer,
    onSecondaryContainer = HorizonOnTealContainer,

    // Fixed roles: tone-stable, identical in light and dark (M3 spec) — used by PassportChip
    secondaryFixed = HorizonSecondaryFixed,
    secondaryFixedDim = HorizonSecondaryFixedDim,
    onSecondaryFixed = HorizonOnSecondaryFixed,
    onSecondaryFixedVariant = HorizonOnSecondaryFixedVariant,

    tertiary = HorizonAmber,
    onTertiary = HorizonOnAmber,
    tertiaryContainer = HorizonAmberContainer,
    onTertiaryContainer = HorizonOnAmberContainer,

    background = HorizonSurface,
    surface = HorizonSurface,
    onBackground = HorizonOnSurface,
    onSurface = HorizonOnSurface,

    // Surface container hierarchy — tonal layering for depth without borders (§2)
    surfaceContainerLowest = HorizonSurfaceContainerLowest,
    surfaceContainerLow = HorizonSurfaceContainerLow,
    surfaceContainer = HorizonSurfaceContainer,
    surfaceContainerHigh = HorizonSurfaceContainerHigh,
    surfaceContainerHighest = HorizonSurfaceContainerHighest,

    surfaceVariant = HorizonSurfaceVariant,
    onSurfaceVariant = HorizonOnSurfaceVariant,

    inverseSurface = HorizonInverseSurface,
    inverseOnSurface = HorizonInverseOnSurface,
    inversePrimary = HorizonInversePrimary,

    outline = HorizonOutline,
    outlineVariant = HorizonOutlineVariant,

    error = HorizonError,
    onError = HorizonOnError,
    errorContainer = HorizonErrorContainer,
    onErrorContainer = HorizonOnErrorContainer,

    scrim = HorizonScrim
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SplitTripTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We set dynamicColor to false by default to enforce the Horizon brand identity.
    // If true, it would use the user's wallpaper colors (Material You).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = {
            SharedTransitionLayout {
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this,
                    LocalDarkTheme provides darkTheme
                ) {
                    content()
                }
            }
        }
    )
}
