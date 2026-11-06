package es.pedrazamiguez.splittrip.core.designsystem.preview.chip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Clock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCard
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plane
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

@PreviewThemes
@Composable
private fun PassportTagAllStylesPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PassportTagStyle.entries.forEach { style ->
                PassportTag(
                    text = style.name,
                    style = style
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun PassportTagWithIconPreview() {
    PreviewThemeWrapper {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PassportTag(
                text = "Flights",
                icon = TablerIcons.Outline.Plane,
                style = PassportTagStyle.PRIMARY
            )
            PassportTag(
                text = "Card",
                icon = TablerIcons.Outline.CreditCard,
                style = PassportTagStyle.SECONDARY
            )
            PassportTag(
                text = "Pending",
                icon = TablerIcons.Outline.Clock,
                style = PassportTagStyle.WARNING
            )
            PassportTag(
                text = "12 Oct",
                icon = TablerIcons.Outline.Calendar,
                style = PassportTagStyle.NEUTRAL
            )
        }
    }
}

@PreviewThemes
@Composable
private fun PassportTagLongTextPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .width(160.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PassportTag(
                text = "Super long expense category that overflows",
                icon = TablerIcons.Outline.Plane,
                style = PassportTagStyle.PRIMARY
            )
            PassportTag(
                text = "Extremely long text without icon",
                style = PassportTagStyle.NEUTRAL
            )
        }
    }
}
