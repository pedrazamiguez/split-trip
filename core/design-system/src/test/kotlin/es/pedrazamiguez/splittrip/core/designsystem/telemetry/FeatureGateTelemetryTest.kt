package es.pedrazamiguez.splittrip.core.designsystem.telemetry

import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class FeatureGateTelemetryTest {

    private val telemetryTracker: TelemetryTracker = mockk(relaxed = true)

    @Test
    fun `trackFeatureGateBlocked emits feature_gate_blocked with lowercase feature name and metadata`() {
        telemetryTracker.trackFeatureGateBlocked(
            feature = GatedFeature.AI_RECEIPT_SCANNING,
            entryPoint = MonetizationTelemetry.GateEntryPoint.AI_MODE_TOGGLE,
            upgradePrompted = true
        )
        telemetryTracker.trackFeatureGateBlocked(
            feature = GatedFeature.SUBUNIT_CREATION,
            entryPoint = MonetizationTelemetry.GateEntryPoint.SUBUNIT_FAB,
            upgradePrompted = false
        )
        telemetryTracker.trackFeatureGateBlocked(
            feature = GatedFeature.GROUP_COVER_UPLOAD,
            entryPoint = MonetizationTelemetry.GateEntryPoint.GROUP_COVER_PICK,
            upgradePrompted = true
        )

        verify {
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                mapOf(
                    MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.FEATURE,
                    MonetizationTelemetry.Param.GATE_NAME to "ai_receipt_scanning",
                    MonetizationTelemetry.Param.ENTRY_POINT to MonetizationTelemetry.GateEntryPoint.AI_MODE_TOGGLE,
                    MonetizationTelemetry.Param.UPGRADE_PROMPTED to true
                )
            )
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                mapOf(
                    MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.FEATURE,
                    MonetizationTelemetry.Param.GATE_NAME to "subunit_creation",
                    MonetizationTelemetry.Param.ENTRY_POINT to MonetizationTelemetry.GateEntryPoint.SUBUNIT_FAB,
                    MonetizationTelemetry.Param.UPGRADE_PROMPTED to false
                )
            )
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                mapOf(
                    MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.FEATURE,
                    MonetizationTelemetry.Param.GATE_NAME to "group_cover_upload",
                    MonetizationTelemetry.Param.ENTRY_POINT to MonetizationTelemetry.GateEntryPoint.GROUP_COVER_PICK,
                    MonetizationTelemetry.Param.UPGRADE_PROMPTED to true
                )
            )
        }
    }

    @Test
    fun `trackLimitGateBlocked emits feature_gate_blocked with lowercase limit name and upgrade_prompted false`() {
        telemetryTracker.trackLimitGateBlocked(
            limit = GatedLimit.MAX_OWNED_GROUPS_COUNT,
            entryPoint = MonetizationTelemetry.GateEntryPoint.GROUPS_FAB,
            upgradePrompted = false
        )
        telemetryTracker.trackLimitGateBlocked(
            limit = GatedLimit.MAX_MEMBERS_PER_GROUP,
            entryPoint = MonetizationTelemetry.GateEntryPoint.GROUP_MEMBER_SELECT,
            upgradePrompted = false
        )

        verify {
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                mapOf(
                    MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.LIMIT,
                    MonetizationTelemetry.Param.GATE_NAME to "max_owned_groups_count",
                    MonetizationTelemetry.Param.ENTRY_POINT to MonetizationTelemetry.GateEntryPoint.GROUPS_FAB,
                    MonetizationTelemetry.Param.UPGRADE_PROMPTED to false
                )
            )
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                mapOf(
                    MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.LIMIT,
                    MonetizationTelemetry.Param.GATE_NAME to "max_members_per_group",
                    MonetizationTelemetry.Param.ENTRY_POINT to MonetizationTelemetry.GateEntryPoint.GROUP_MEMBER_SELECT,
                    MonetizationTelemetry.Param.UPGRADE_PROMPTED to false
                )
            )
        }
    }
}
