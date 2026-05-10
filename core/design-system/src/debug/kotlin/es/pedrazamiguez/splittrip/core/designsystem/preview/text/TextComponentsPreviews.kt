package es.pedrazamiguez.splittrip.core.designsystem.preview.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LargeBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.ScreenTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewLocales
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

// ─── Full gallery (light/dark) ───────────────────────────────────────────────

@PreviewThemes
@Composable
private fun TextComponentsGalleryPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenTitleText(text = "My Trips (headlineLarge Bold)")
            SheetTitleText(text = "Select a Group (titleLarge Bold)")
            SectionHeadingText(text = "Recent Expenses (titleMedium Bold)")
            CardSectionLabelText(text = "Due Date (titleSmall Bold)")
            CardTitleText(text = "Hotel Lisbon (titleSmall SemiBold)")
            LargeBodyText(text = "Group currency symbol € (bodyLarge Normal)")
            BodyText(text = "Shared across 4 participants over 3 nights. (bodyMedium Normal)")
            SecondaryBodyText(text = "12 May 2026 · 4 members (bodySmall Normal)")
            LabelText(text = "Paid by Alice (labelLarge SemiBold)")
            CaptionText(text = "Synced 2 min ago (labelSmall Medium)")
            AmountText(text = "€ 128.50 (titleMedium Bold)")
            AmountText(
                text = "− € 45.00 (error colour)",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ─── Locale variants (EN / ES) ───────────────────────────────────────────────

@PreviewLocales
@Composable
private fun TextComponentsLocalePreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScreenTitleText(text = "My Trips")
            SheetTitleText(text = "Select a Group")
            SectionHeadingText(text = "Recent Expenses")
            CardSectionLabelText(text = "Due Date")
            CardTitleText(text = "Hotel Lisbon")
            LargeBodyText(text = "Group currency")
            BodyText(text = "Shared across 4 participants.")
            SecondaryBodyText(text = "12 May 2026 · 4 members")
            LabelText(text = "Paid by Alice")
            CaptionText(text = "Synced 2 min ago")
            AmountText(text = "€ 128.50")
        }
    }
}

// ─── Individual wrappers (light/dark) for isolated inspection ────────────────

@PreviewThemes
@Composable
private fun ScreenTitleTextPreview() {
    PreviewThemeWrapper {
        ScreenTitleText(
            text = "Your Trips",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun SectionHeadingTextPreview() {
    PreviewThemeWrapper {
        SectionHeadingText(
            text = "This Week",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun CardTitleTextPreview() {
    PreviewThemeWrapper {
        CardTitleText(
            text = "Dinner at Taberna",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun BodyTextPreview() {
    PreviewThemeWrapper {
        BodyText(
            text = "Split equally among all group members. Tap an expense to see breakdown.",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun SecondaryBodyTextPreview() {
    PreviewThemeWrapper {
        SecondaryBodyText(
            text = "3 May 2026 · Paid by Bob",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun LabelTextPreview() {
    PreviewThemeWrapper {
        LabelText(
            text = "Split Method",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun CaptionTextPreview() {
    PreviewThemeWrapper {
        CaptionText(
            text = "Last updated 5 minutes ago",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun AmountTextPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AmountText(text = "€ 256.00")
            // Negative-balance variant — error colour override
            AmountText(
                text = "− € 45.00",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ─── New wrappers: isolated previews ─────────────────────────────────────────

@PreviewThemes
@Composable
private fun SheetTitleTextPreview() {
    PreviewThemeWrapper {
        SheetTitleText(
            text = "Select a Group",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun CardSectionLabelTextPreview() {
    PreviewThemeWrapper {
        CardSectionLabelText(
            text = "Due Date",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun LargeBodyTextPreview() {
    PreviewThemeWrapper {
        LargeBodyText(
            text = "Group currency symbol €",
            modifier = Modifier.padding(16.dp)
        )
    }
}
