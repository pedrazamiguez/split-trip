package es.pedrazamiguez.splittrip.domain.service.featuregate

/**
 * Limits on resource creations that can vary depending on user tier.
 */
enum class GatedLimit {
    MAX_OWNED_GROUPS_COUNT,
    MAX_MEMBERS_PER_GROUP,
    AI_RECEIPT_MONTHLY_SCANS,
    MANUAL_RECEIPT_MONTHLY_ATTACHMENTS,
    MAX_SUBUNITS_PER_GROUP
}
