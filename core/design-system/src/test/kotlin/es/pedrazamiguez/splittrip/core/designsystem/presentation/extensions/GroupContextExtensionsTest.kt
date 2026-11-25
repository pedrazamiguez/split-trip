package es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions

import es.pedrazamiguez.splittrip.core.common.R
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Home
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PlaneTilt
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class GroupContextExtensionsTest {

    @Test
    fun `TRAVEL maps to group_context_travel, group_context_travel_description, and PlaneTilt icon`() {
        assertEquals(R.string.group_context_travel, GroupContext.TRAVEL.toStringRes())
        assertEquals(R.string.group_context_travel_description, GroupContext.TRAVEL.toDescriptionRes())
        assertEquals(TablerIcons.Outline.PlaneTilt, GroupContext.TRAVEL.toIconVector())
    }

    @Test
    fun `EVERYDAY maps to group_context_everyday, group_context_everyday_description, and Home icon`() {
        assertEquals(R.string.group_context_everyday, GroupContext.EVERYDAY.toStringRes())
        assertEquals(R.string.group_context_everyday_description, GroupContext.EVERYDAY.toDescriptionRes())
        assertEquals(TablerIcons.Outline.Home, GroupContext.EVERYDAY.toIconVector())
    }

    @Test
    fun `all GroupContext entries map to distinct string resources and non-null icon vectors`() {
        val labelResIds = GroupContext.entries.map { it.toStringRes() }
        val descResIds = GroupContext.entries.map { it.toDescriptionRes() }

        assertEquals(GroupContext.entries.size, labelResIds.toSet().size)
        assertEquals(GroupContext.entries.size, descResIds.toSet().size)
        GroupContext.entries.forEach { context ->
            assertNotNull(context.toIconVector())
        }
    }
}
