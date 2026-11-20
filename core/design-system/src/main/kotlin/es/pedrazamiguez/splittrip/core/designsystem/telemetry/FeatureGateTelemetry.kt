package es.pedrazamiguez.splittrip.core.designsystem.telemetry

import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit

fun TelemetryTracker.trackFeatureGateBlocked(
    feature: GatedFeature,
    entryPoint: String,
    upgradePrompted: Boolean
) {
    trackGateBlocked(
        gateType = MonetizationTelemetry.GateType.FEATURE,
        gateName = feature.name.lowercase(),
        entryPoint = entryPoint,
        upgradePrompted = upgradePrompted
    )
}

fun TelemetryTracker.trackLimitGateBlocked(
    limit: GatedLimit,
    entryPoint: String,
    upgradePrompted: Boolean
) {
    trackGateBlocked(
        gateType = MonetizationTelemetry.GateType.LIMIT,
        gateName = limit.name.lowercase(),
        entryPoint = entryPoint,
        upgradePrompted = upgradePrompted
    )
}

private fun TelemetryTracker.trackGateBlocked(
    gateType: String,
    gateName: String,
    entryPoint: String,
    upgradePrompted: Boolean
) {
    trackEvent(
        MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
        mapOf(
            MonetizationTelemetry.Param.GATE_TYPE to gateType,
            MonetizationTelemetry.Param.GATE_NAME to gateName,
            MonetizationTelemetry.Param.ENTRY_POINT to entryPoint,
            MonetizationTelemetry.Param.UPGRADE_PROMPTED to upgradePrompted
        )
    )
}
