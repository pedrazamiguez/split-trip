package es.pedrazamiguez.splittrip.core.designsystem.preview.scaffold

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BasketPlus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChartArcs
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.navigation.FloatingNavTab
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation.FloatingNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.NavigationBarIcon
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

// ---------------------------------------------------------------------------
// Minimal stub tab for preview purposes only
// ---------------------------------------------------------------------------

private data class PreviewTab(
    override val id: String,
    val label: String,
    val icon: ImageVector
) : FloatingNavTab {

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) {
        NavigationBarIcon(
            icon = icon,
            contentDescription = label,
            isSelected = isSelected
        )
    }

    @Composable
    override fun getLabel(): String = label
}

private val previewTabs = listOf(
    PreviewTab("groups", "Groups", TablerIcons.Outline.UsersGroup),
    PreviewTab("balances", "Balances", TablerIcons.Outline.UsersGroup),
    PreviewTab("expenses", "Expenses", TablerIcons.Outline.UsersGroup),
    PreviewTab("activity", "Activity", TablerIcons.Outline.UsersGroup)
)

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

/** No main action — pill is narrower, no action button visible. */
@PreviewThemes
@Composable
private fun FloatingNavigationBarWithNoActionPreview() {
    PreviewThemeWrapper {
        FloatingNavigationBar(
            items = previewTabs,
            selectedId = "groups",
            mainAction = null,
            applyWindowInsets = false
        )
    }
}

/** Add action — simulates Groups or Expenses tab selection. */
@PreviewThemes
@Composable
private fun FloatingNavigationBarWithPlusActionPreview() {
    PreviewThemeWrapper {
        FloatingNavigationBar(
            items = previewTabs,
            selectedId = "groups",
            mainAction = MainAction(
                icon = TablerIcons.Outline.BasketPlus,
                contentDescription = "Add",
                onClick = {}
            ),
            applyWindowInsets = false
        )
    }
}

/** ChartArcs action — simulates Balances tab selection. */
@PreviewThemes
@Composable
private fun FloatingNavigationBarWithFilterActionPreview() {
    PreviewThemeWrapper {
        FloatingNavigationBar(
            items = previewTabs,
            selectedId = "balances",
            mainAction = MainAction(
                icon = TablerIcons.Outline.ChartArcs,
                contentDescription = "Chart",
                onClick = {}
            ),
            applyWindowInsets = false
        )
    }
}
