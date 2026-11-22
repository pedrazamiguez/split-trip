package es.pedrazamiguez.splittrip.core.designsystem.preview.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SegmentedPillSelector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SegmentedPillSelectorDefaults
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.rememberLocale
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewLocales
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes
import kotlinx.collections.immutable.persistentListOf

private const val DARK_THEME_LUMINANCE_THRESHOLD = 0.5f
private const val LARGE_FONT_SCALE = 1.3f

private val englishSplitOptions = persistentListOf("Equal", "Exact", "Percent")
private val spanishSplitOptions = persistentListOf("Partes iguales", "Cantidades", "Porcentaje")
private val binaryOptions = persistentListOf("Group", "Personal")

@PreviewThemes
@Composable
private fun SegmentedPillSelectorStandalonePreview() {
    PreviewThemeWrapper {
        SegmentedPillSelector(
            options = englishSplitOptions,
            selectedOption = englishSplitOptions.first(),
            onOptionSelected = {},
            optionLabel = { it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewThemes
@Composable
private fun SegmentedPillSelectorNestedInCardPreview() {
    PreviewThemeWrapper {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_THEME_LUMINANCE_THRESHOLD
        val cardColor = if (isDark) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        }
        FlatCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            color = cardColor,
            ghostBorder = true
        ) {
            SegmentedPillSelector(
                options = englishSplitOptions,
                selectedOption = englishSplitOptions[1],
                onOptionSelected = {},
                optionLabel = { it },
                colors = SegmentedPillSelectorDefaults.nestedColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default)
            )
        }
    }
}

@PreviewThemes
@Composable
private fun SegmentedPillSelectorVariantsPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)
        ) {
            SegmentedPillSelector(
                options = binaryOptions,
                selectedOption = binaryOptions.first(),
                onOptionSelected = {},
                optionLabel = { it }
            )
            SegmentedPillSelector(
                options = englishSplitOptions,
                selectedOption = null,
                onOptionSelected = {},
                optionLabel = { it }
            )
            SegmentedPillSelector(
                options = englishSplitOptions,
                selectedOption = englishSplitOptions.last(),
                onOptionSelected = {},
                optionLabel = { it },
                enabled = false
            )
        }
    }
}

@PreviewLocales
@Composable
private fun SegmentedPillSelectorLocalesPreview() {
    PreviewThemeWrapper {
        val isSpanish = rememberLocale().language == "es"
        val options = if (isSpanish) spanishSplitOptions else englishSplitOptions
        SegmentedPillSelector(
            options = options,
            selectedOption = options.first(),
            onOptionSelected = {},
            optionLabel = { it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun SegmentedPillSelectorNarrowStandalonePreview() {
    PreviewThemeWrapper {
        SegmentedPillSelector(
            options = spanishSplitOptions,
            selectedOption = spanishSplitOptions.first(),
            onOptionSelected = {},
            optionLabel = { it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun SegmentedPillSelectorNarrowNestedLargeFontPreview() {
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = LARGE_FONT_SCALE
        )
    ) {
        PreviewThemeWrapper {
            val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_THEME_LUMINANCE_THRESHOLD
            val cardColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
            FlatCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                color = cardColor,
                ghostBorder = true
            ) {
                SegmentedPillSelector(
                    options = spanishSplitOptions,
                    selectedOption = spanishSplitOptions[1],
                    onOptionSelected = {},
                    optionLabel = { it },
                    colors = SegmentedPillSelectorDefaults.nestedColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
        }
    }
}
