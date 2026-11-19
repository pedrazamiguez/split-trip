package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.AppUpdateDecision
import es.pedrazamiguez.splittrip.domain.model.AppUpdatePolicy
import es.pedrazamiguez.splittrip.domain.model.ForcedUpdateReason
import es.pedrazamiguez.splittrip.domain.model.OptionalUpdateDismissal
import es.pedrazamiguez.splittrip.domain.model.SemanticVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppUpdatePolicyServiceImplTest {

    private val service = AppUpdatePolicyServiceImpl()

    private fun policy(
        inAppUpdatesEnabled: Boolean = true,
        minSupportedVersionCode: Long = 0L,
        autoForcePatchUpdates: Boolean = true,
        optionalUpdateVersionCodes: Set<Long> = emptySet(),
        optionalUpdateCooldownHours: Long = 24L
    ) = AppUpdatePolicy(
        inAppUpdatesEnabled = inAppUpdatesEnabled,
        minSupportedVersionCode = minSupportedVersionCode,
        autoForcePatchUpdates = autoForcePatchUpdates,
        optionalUpdateVersionCodes = optionalUpdateVersionCodes,
        optionalUpdateCooldownHours = optionalUpdateCooldownHours
    )

    @Test
    fun `returns None when killswitch inAppUpdatesEnabled is false even below minSupportedVersionCode`() {
        val decision = service.resolveDecision(
            installedVersionCode = 900_000L,
            availableVersionCode = 2_000_001L,
            policy = policy(inAppUpdatesEnabled = false, minSupportedVersionCode = 1_000_000L),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )

        assertEquals(AppUpdateDecision.None, decision)
    }

    @Test
    fun `returns None when availableVersionCode equals or is less than installedVersionCode`() {
        val sameVersion = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_000_000L,
            policy = policy(),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )
        val olderVersion = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 999_000L,
            policy = policy(),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )

        assertEquals(AppUpdateDecision.None, sameVersion)
        assertEquals(AppUpdateDecision.None, olderVersion)
    }

    @Test
    fun `returns None when installedVersionCode is negative`() {
        val decision = service.resolveDecision(
            installedVersionCode = -1L,
            availableVersionCode = 1_000_000L,
            policy = policy(),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )

        assertEquals(AppUpdateDecision.None, decision)
    }

    @Test
    fun `returns Forced BELOW_MIN_SUPPORTED_VERSION when installed is below minSupportedVersionCode`() {
        val decision = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_001_000L,
            policy = policy(
                minSupportedVersionCode = 1_001_000L,
                optionalUpdateVersionCodes = setOf(1_001_000L)
            ),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )

        assertEquals(
            AppUpdateDecision.Forced(
                availableVersionCode = 1_001_000L,
                availableVersion = SemanticVersion(1, 1, 0),
                reason = ForcedUpdateReason.BELOW_MIN_SUPPORTED_VERSION
            ),
            decision
        )
    }

    @Test
    fun `ignores minSupportedVersionCode when set to 0`() {
        val decision = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_001_000L,
            policy = policy(minSupportedVersionCode = 0L),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )

        assertEquals(
            AppUpdateDecision.Optional(
                availableVersionCode = 1_001_000L,
                availableVersion = SemanticVersion(1, 1, 0)
            ),
            decision
        )
    }

    @Test
    fun `returns Optional when availableVersionCode is in optionalUpdateVersionCodes even for major or patch update`() {
        val majorOverridden = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 2_000_000L,
            policy = policy(optionalUpdateVersionCodes = setOf(2_000_000L, 1_000_001L)),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )
        val patchOverridden = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_000_001L,
            policy = policy(optionalUpdateVersionCodes = setOf(2_000_000L, 1_000_001L)),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )

        assertEquals(
            AppUpdateDecision.Optional(2_000_000L, SemanticVersion(2, 0, 0)),
            majorOverridden
        )
        assertEquals(
            AppUpdateDecision.Optional(1_000_001L, SemanticVersion(1, 0, 1)),
            patchOverridden
        )
    }

    @Test
    fun `returns Forced MAJOR_VERSION_UPDATE when available major is greater than installed major`() {
        val decision = service.resolveDecision(
            installedVersionCode = 1_002_000L,
            availableVersionCode = 2_000_000L,
            policy = policy(),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )

        assertEquals(
            AppUpdateDecision.Forced(
                availableVersionCode = 2_000_000L,
                availableVersion = SemanticVersion(2, 0, 0),
                reason = ForcedUpdateReason.MAJOR_VERSION_UPDATE
            ),
            decision
        )
    }

    @Test
    fun `returns Forced PATCH_HOTFIX_UPDATE when autoForcePatchUpdates is true and patch is positive`() {
        val patchOnly = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_000_001L,
            policy = policy(autoForcePatchUpdates = true),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )
        val minorWithPatch = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_001_001L,
            policy = policy(autoForcePatchUpdates = true),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )

        assertEquals(
            AppUpdateDecision.Forced(
                availableVersionCode = 1_000_001L,
                availableVersion = SemanticVersion(1, 0, 1),
                reason = ForcedUpdateReason.PATCH_HOTFIX_UPDATE
            ),
            patchOnly
        )
        assertEquals(
            AppUpdateDecision.Forced(
                availableVersionCode = 1_001_001L,
                availableVersion = SemanticVersion(1, 1, 1),
                reason = ForcedUpdateReason.PATCH_HOTFIX_UPDATE
            ),
            minorWithPatch
        )
    }

    @Test
    fun `returns Optional for patch update when autoForcePatchUpdates is false`() {
        val decision = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_000_001L,
            policy = policy(autoForcePatchUpdates = false),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )

        assertEquals(
            AppUpdateDecision.Optional(
                availableVersionCode = 1_000_001L,
                availableVersion = SemanticVersion(1, 0, 1)
            ),
            decision
        )
    }

    @Test
    fun `returns Optional for minor update with patch equal to 0`() {
        val decision = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_001_000L,
            policy = policy(),
            lastDismissal = null,
            nowTimestampMs = NOW_MS
        )

        assertEquals(
            AppUpdateDecision.Optional(
                availableVersionCode = 1_001_000L,
                availableVersion = SemanticVersion(1, 1, 0)
            ),
            decision
        )
    }

    @Test
    fun `suppresses Optional update to None while within cooldown for same versionCode`() {
        val dismissal = OptionalUpdateDismissal(
            versionCode = 1_001_000L,
            dismissedAtTimestampMs = NOW_MS - 23L * HOUR_MS
        )

        val decision = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_001_000L,
            policy = policy(optionalUpdateCooldownHours = 24L),
            lastDismissal = dismissal,
            nowTimestampMs = NOW_MS
        )

        assertEquals(AppUpdateDecision.None, decision)
    }

    @Test
    fun `returns Optional once cooldown duration has elapsed for same versionCode`() {
        val dismissal = OptionalUpdateDismissal(
            versionCode = 1_001_000L,
            dismissedAtTimestampMs = NOW_MS - 24L * HOUR_MS
        )

        val decision = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_001_000L,
            policy = policy(optionalUpdateCooldownHours = 24L),
            lastDismissal = dismissal,
            nowTimestampMs = NOW_MS
        )

        assertEquals(
            AppUpdateDecision.Optional(
                availableVersionCode = 1_001_000L,
                availableVersion = SemanticVersion(1, 1, 0)
            ),
            decision
        )
    }

    @Test
    fun `returns Optional immediately when availableVersionCode differs from last dismissed versionCode`() {
        val dismissal = OptionalUpdateDismissal(
            versionCode = 1_001_000L,
            dismissedAtTimestampMs = NOW_MS - HOUR_MS
        )

        val decision = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_002_000L,
            policy = policy(optionalUpdateCooldownHours = 24L),
            lastDismissal = dismissal,
            nowTimestampMs = NOW_MS
        )

        assertEquals(
            AppUpdateDecision.Optional(
                availableVersionCode = 1_002_000L,
                availableVersion = SemanticVersion(1, 2, 0)
            ),
            decision
        )
    }

    @Test
    fun `returns Optional when cooldownHours is zero or negative`() {
        val dismissal = OptionalUpdateDismissal(
            versionCode = 1_001_000L,
            dismissedAtTimestampMs = NOW_MS - 1_000L
        )

        val decision = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_001_000L,
            policy = policy(optionalUpdateCooldownHours = 0L),
            lastDismissal = dismissal,
            nowTimestampMs = NOW_MS
        )

        assertEquals(
            AppUpdateDecision.Optional(
                availableVersionCode = 1_001_000L,
                availableVersion = SemanticVersion(1, 1, 0)
            ),
            decision
        )
    }

    @Test
    fun `returns Optional when clock skew produces negative elapsed time`() {
        val dismissal = OptionalUpdateDismissal(
            versionCode = 1_001_000L,
            dismissedAtTimestampMs = NOW_MS + HOUR_MS
        )

        val decision = service.resolveDecision(
            installedVersionCode = 1_000_000L,
            availableVersionCode = 1_001_000L,
            policy = policy(optionalUpdateCooldownHours = 24L),
            lastDismissal = dismissal,
            nowTimestampMs = NOW_MS
        )

        assertEquals(
            AppUpdateDecision.Optional(
                availableVersionCode = 1_001_000L,
                availableVersion = SemanticVersion(1, 1, 0)
            ),
            decision
        )
    }

    private companion object {
        const val HOUR_MS = 3_600_000L
        const val NOW_MS = 1_700_000_000_000L
    }
}
