package es.pedrazamiguez.splittrip.features.settings.navigation

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.EmailStamp
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

object AvatarGenTabItem : FloatingNavTab {
    override val id = "avatar_gen"

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) {
        Icon(TablerIcons.Outline.EmailStamp, contentDescription = null, tint = tint)
    }

    @Composable
    override fun getLabel() = stringResource(R.string.developer_services_tab_avatar)
}

val SERVICE_TABS = listOf(OcrTabItem, AiExtractionTabItem, AvatarGenTabItem)
