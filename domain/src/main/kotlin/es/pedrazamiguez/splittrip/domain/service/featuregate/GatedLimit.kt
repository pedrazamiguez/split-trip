package es.pedrazamiguez.splittrip.domain.service.featuregate

/**
 * Limits on resource creations that can vary depending on user tier.
 */
enum class GatedLimit {
    MAX_GROUPS_COUNT,
    MAX_MEMBERS_PER_GROUP
}
