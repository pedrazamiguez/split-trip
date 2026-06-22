package es.pedrazamiguez.splittrip.domain.service.featuregate

/**
 * Features that can be locked or restricted based on user tier.
 */
enum class GatedFeature {
    GROUP_COVER_UPLOAD,
    SUBUNIT_CREATION,
    AI_RECEIPT_SCANNING
}
