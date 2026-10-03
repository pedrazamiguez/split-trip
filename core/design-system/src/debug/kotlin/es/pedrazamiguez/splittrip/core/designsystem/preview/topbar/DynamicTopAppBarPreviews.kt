package es.pedrazamiguez.splittrip.core.designsystem.preview.topbar

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.DotsVertical
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

@PreviewThemes
@Composable
private fun DynamicTopAppBarStandardPreview() {
    PreviewThemeWrapper {
        DynamicTopAppBar(
            title = "Expenses",
            actions = {
                IconButton(onClick = {}) {
                    Icon(imageVector = TablerIcons.Outline.Search, contentDescription = "Search")
                }
            }
        )
    }
}

@PreviewThemes
@Composable
private fun DynamicTopAppBarWithSubtitlePreview() {
    PreviewThemeWrapper {
        DynamicTopAppBar(
            title = "Trip to Tokyo",
            subtitle = "Japan 2026",
            onBack = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(imageVector = TablerIcons.Outline.DotsVertical, contentDescription = "More")
                }
            }
        )
    }
}

@PreviewThemes
@Composable
private fun DynamicTopAppBarLongTitlePreview() {
    PreviewThemeWrapper {
        DynamicTopAppBar(
            title = "Summer Road Trip Across the Pacific Coast Highway 2026",
            subtitle = "Active group with 12 members and multi-currency balances",
            onBack = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(imageVector = TablerIcons.Outline.DotsVertical, contentDescription = "More")
                }
            }
        )
    }
}

@PreviewThemes
@Composable
private fun DynamicTopAppBarWithoutBackPreview() {
    PreviewThemeWrapper {
        DynamicTopAppBar(
            title = "Balances",
            subtitle = "Overview"
        )
    }
}
