package es.pedrazamiguez.splittrip.features.settings.presentation.extensions

import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AdOff
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReportAnalytics
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Share
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Shield
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersPlus
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowancePerkType
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceQuotaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PlanAllowanceIconExtensionsTest {

    @Nested
    inner class QuotaIconMapping {

        @Test
        fun `AI_RECEIPT_SCANS maps to PhotoAi icon`() {
            assertSame(TablerIcons.Outline.PhotoAi, AllowanceQuotaType.AI_RECEIPT_SCANS.toIconVector())
        }

        @Test
        fun `MEDIA_UPLOADS maps to Photo icon`() {
            assertSame(TablerIcons.Outline.Photo, AllowanceQuotaType.MEDIA_UPLOADS.toIconVector())
        }

        @Test
        fun `REPORT_EXPORTS maps to Share icon`() {
            assertSame(TablerIcons.Outline.Share, AllowanceQuotaType.REPORT_EXPORTS.toIconVector())
        }

        @Test
        fun `REPORT_EXPORTS does not reuse ReportAnalytics icon`() {
            assertNotSame(TablerIcons.Outline.ReportAnalytics, AllowanceQuotaType.REPORT_EXPORTS.toIconVector())
        }

        @Test
        fun `OWNED_GROUPS maps to UsersGroup icon`() {
            assertSame(TablerIcons.Outline.UsersGroup, AllowanceQuotaType.OWNED_GROUPS.toIconVector())
        }

        @Test
        fun `all quota types map to distinct icons`() {
            val icons = AllowanceQuotaType.entries.map { it.toIconVector() }
            assertEquals(icons.size, icons.toSet().size)
        }
    }

    @Nested
    inner class PerkIconMapping {

        @Test
        fun `MEMBERS_PER_GROUP maps to UsersPlus icon`() {
            assertSame(TablerIcons.Outline.UsersPlus, AllowancePerkType.MEMBERS_PER_GROUP.toIconVector())
        }

        @Test
        fun `SUBUNITS_PER_GROUP maps to Sitemap icon`() {
            assertSame(TablerIcons.Outline.Sitemap, AllowancePerkType.SUBUNITS_PER_GROUP.toIconVector())
        }

        @Test
        fun `AD_FREE maps to AdOff icon`() {
            assertSame(TablerIcons.Outline.AdOff, AllowancePerkType.AD_FREE.toIconVector())
        }

        @Test
        fun `AD_FREE does not reuse Shield icon`() {
            assertNotSame(TablerIcons.Outline.Shield, AllowancePerkType.AD_FREE.toIconVector())
        }

        @Test
        fun `all perk types map to distinct icons`() {
            val icons = AllowancePerkType.entries.map { it.toIconVector() }
            assertEquals(icons.size, icons.toSet().size)
        }

        @Test
        fun `AdOff icon vector is cached across repeated accesses`() {
            assertSame(TablerIcons.Outline.AdOff, TablerIcons.Outline.AdOff)
            assertEquals("Outline.AdOff", TablerIcons.Outline.AdOff.name)
        }
    }
}
