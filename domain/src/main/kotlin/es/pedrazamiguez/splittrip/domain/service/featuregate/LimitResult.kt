package es.pedrazamiguez.splittrip.domain.service.featuregate

/**
 * Result of checking a resource limit for a user.
 */
sealed interface LimitResult {

    /**
     * Action is permitted.
     */
    data object Allowed : LimitResult

    /**
     * Action is blocked because limit has been exceeded.
     *
     * @property limit The limit that was exceeded.
     * @property upgradeRequired True if linking/upgrading the account can lift this limit.
     */
    data class Blocked(
        val limit: GatedLimit,
        val upgradeRequired: Boolean
    ) : LimitResult
}
