package es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Bed
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Category
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.MasksTheater
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Route
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Run
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Shield
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ShoppingBag
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ToolsKitchen2
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory

fun ExpenseCategory.toIconVector(): ImageVector = when (this) {
    ExpenseCategory.TRANSPORT -> TablerIcons.Outline.Route
    ExpenseCategory.FOOD -> TablerIcons.Outline.ToolsKitchen2
    ExpenseCategory.LODGING -> TablerIcons.Outline.Bed
    ExpenseCategory.ACTIVITIES -> TablerIcons.Outline.Run
    ExpenseCategory.INSURANCE -> TablerIcons.Outline.Shield
    ExpenseCategory.ENTERTAINMENT -> TablerIcons.Outline.MasksTheater
    ExpenseCategory.SHOPPING -> TablerIcons.Outline.ShoppingBag
    ExpenseCategory.OTHER -> TablerIcons.Outline.Category
}
