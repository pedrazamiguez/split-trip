package es.pedrazamiguez.splittrip.features.settings.navigation

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCard
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.TextScan2
import es.pedrazamiguez.splittrip.core.designsystem.navigation.FloatingNavTab
import es.pedrazamiguez.splittrip.features.settings.R

object OcrTabItem : FloatingNavTab {
    override val id = "ocr"

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) {
        Icon(TablerIcons.Outline.TextScan2, contentDescription = null, tint = tint)
    }

    @Composable
    override fun getLabel() = stringResource(R.string.developer_services_tab_ocr)
}

object AiExtractionTabItem : FloatingNavTab {
    override val id = "ai_extraction"

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) {
        Icon(TablerIcons.Outline.PhotoAi, contentDescription = null, tint = tint)
    }

    @Composable
    override fun getLabel() = stringResource(R.string.developer_services_tab_ai_extraction)
}

object BillingTabItem : FloatingNavTab {
    override val id = "billing"

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) {
        Icon(
            imageVector = TablerIcons.Outline.CreditCard,
            contentDescription = getLabel(),
            tint = tint
        )
    }

    @Composable
    override fun getLabel(): String = stringResource(R.string.developer_services_tab_billing)
}

val SERVICE_TABS = listOf(OcrTabItem, AiExtractionTabItem, BillingTabItem)
