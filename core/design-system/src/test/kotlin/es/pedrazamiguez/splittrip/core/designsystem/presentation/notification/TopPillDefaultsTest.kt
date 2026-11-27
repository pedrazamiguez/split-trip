package es.pedrazamiguez.splittrip.core.designsystem.presentation.notification

import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlertTriangle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TopPillDefaults")
class TopPillDefaultsTest {

    @Test
    fun `icon returns null for NEUTRAL`() {
        assertNull(TopPillDefaults.icon(TopPillStyle.NEUTRAL))
    }

    @Test
    fun `icon returns InfoCircle for INFO`() {
        assertEquals(TablerIcons.Outline.InfoCircle, TopPillDefaults.icon(TopPillStyle.INFO))
    }

    @Test
    fun `icon returns CircleCheck for SUCCESS`() {
        assertEquals(TablerIcons.Outline.CircleCheck, TopPillDefaults.icon(TopPillStyle.SUCCESS))
    }

    @Test
    fun `icon returns AlertTriangle for WARNING`() {
        assertEquals(TablerIcons.Outline.AlertTriangle, TopPillDefaults.icon(TopPillStyle.WARNING))
    }

    @Test
    fun `icon returns AlertTriangle for ERROR`() {
        assertEquals(TablerIcons.Outline.AlertTriangle, TopPillDefaults.icon(TopPillStyle.ERROR))
    }
}
