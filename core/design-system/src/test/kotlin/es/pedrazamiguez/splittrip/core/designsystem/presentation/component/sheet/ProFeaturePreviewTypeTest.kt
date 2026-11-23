package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet

import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReportAnalytics
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersPlus
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProFeaturePreviewTypeTest {

    @Test
    fun `fromGatedLimit maps MAX_OWNED_GROUPS_COUNT to UNLIMITED_GROUPS`() {
        val result = ProFeaturePreviewType.fromGatedLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT)

        assertEquals(ProFeaturePreviewType.UNLIMITED_GROUPS, result)
        assertEquals(R.string.pro_preview_unlimited_groups_title, result.titleRes)
        assertEquals(R.string.pro_preview_unlimited_groups_description, result.descriptionRes)
        assertEquals(R.string.pro_preview_unlimited_groups_highlight, result.highlightRes)
        assertEquals(TablerIcons.Outline.UsersGroup, result.icon)
    }

    @Test
    fun `fromGatedLimit maps MAX_MEMBERS_PER_GROUP to EXPANDED_GROUP_MEMBERS`() {
        val result = ProFeaturePreviewType.fromGatedLimit(GatedLimit.MAX_MEMBERS_PER_GROUP)

        assertEquals(ProFeaturePreviewType.EXPANDED_GROUP_MEMBERS, result)
        assertEquals(TablerIcons.Outline.UsersPlus, result.icon)
    }

    @Test
    fun `fromGatedLimit maps AI_RECEIPT_MONTHLY_SCANS to AI_RECEIPT_SCANNING`() {
        val result = ProFeaturePreviewType.fromGatedLimit(GatedLimit.AI_RECEIPT_MONTHLY_SCANS)

        assertEquals(ProFeaturePreviewType.AI_RECEIPT_SCANNING, result)
        assertEquals(TablerIcons.Outline.PhotoAi, result.icon)
    }

    @Test
    fun `fromGatedLimit maps MANUAL_RECEIPT_MONTHLY_ATTACHMENTS to UNLIMITED_RECEIPTS`() {
        val result = ProFeaturePreviewType.fromGatedLimit(GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS)

        assertEquals(ProFeaturePreviewType.UNLIMITED_RECEIPTS, result)
        assertEquals(TablerIcons.Outline.Receipt, result.icon)
    }

    @Test
    fun `fromGatedLimit maps REPORT_EXPORT_MONTHLY_COUNT to TRIP_REPORT_EXPORT`() {
        val result = ProFeaturePreviewType.fromGatedLimit(GatedLimit.REPORT_EXPORT_MONTHLY_COUNT)

        assertEquals(ProFeaturePreviewType.TRIP_REPORT_EXPORT, result)
        assertEquals(TablerIcons.Outline.ReportAnalytics, result.icon)
    }

    @Test
    fun `fromGatedLimit maps MAX_SUBUNITS_PER_GROUP to SUBUNIT_CREATION`() {
        val result = ProFeaturePreviewType.fromGatedLimit(GatedLimit.MAX_SUBUNITS_PER_GROUP)

        assertEquals(ProFeaturePreviewType.SUBUNIT_CREATION, result)
        assertEquals(TablerIcons.Outline.Sitemap, result.icon)
    }

    @Test
    fun `fromGatedFeature maps GROUP_COVER_UPLOAD to GROUP_COVER_PHOTO`() {
        val result = ProFeaturePreviewType.fromGatedFeature(GatedFeature.GROUP_COVER_UPLOAD)

        assertEquals(ProFeaturePreviewType.GROUP_COVER_PHOTO, result)
        assertEquals(TablerIcons.Outline.Photo, result.icon)
    }

    @Test
    fun `fromGatedFeature maps SUBUNIT_CREATION to SUBUNIT_CREATION`() {
        val result = ProFeaturePreviewType.fromGatedFeature(GatedFeature.SUBUNIT_CREATION)

        assertEquals(ProFeaturePreviewType.SUBUNIT_CREATION, result)
        assertEquals(TablerIcons.Outline.Sitemap, result.icon)
    }

    @Test
    fun `fromGatedFeature maps AI_RECEIPT_SCANNING to AI_RECEIPT_SCANNING`() {
        val result = ProFeaturePreviewType.fromGatedFeature(GatedFeature.AI_RECEIPT_SCANNING)

        assertEquals(ProFeaturePreviewType.AI_RECEIPT_SCANNING, result)
        assertEquals(TablerIcons.Outline.PhotoAi, result.icon)
    }

    @Test
    fun `fromGatedFeature maps AD_FREE to UNLIMITED_GROUPS fallback`() {
        val result = ProFeaturePreviewType.fromGatedFeature(GatedFeature.AD_FREE)

        assertEquals(ProFeaturePreviewType.UNLIMITED_GROUPS, result)
    }

    @Test
    fun `fromGatedFeature maps TRIP_REPORT_EXPORT to TRIP_REPORT_EXPORT`() {
        val result = ProFeaturePreviewType.fromGatedFeature(GatedFeature.TRIP_REPORT_EXPORT)

        assertEquals(ProFeaturePreviewType.TRIP_REPORT_EXPORT, result)
        assertEquals(R.string.pro_preview_group_report_export_title, result.titleRes)
        assertEquals(R.string.pro_preview_group_report_export_description, result.descriptionRes)
        assertEquals(R.string.pro_preview_group_report_export_highlight, result.highlightRes)
        assertEquals(TablerIcons.Outline.ReportAnalytics, result.icon)
    }
}
