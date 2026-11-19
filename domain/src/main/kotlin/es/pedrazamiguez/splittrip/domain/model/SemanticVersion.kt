package es.pedrazamiguez.splittrip.domain.model

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {

    init {
        require(major >= 0) { "major must be non-negative: $major" }
        require(minor in 0 until COMPONENT_BASE) { "minor must be in 0..999: $minor" }
        require(patch in 0 until COMPONENT_BASE) { "patch must be in 0..999: $patch" }
    }

    fun toVersionCode(): Long =
        major.toLong() * MAJOR_MULTIPLIER + minor.toLong() * MINOR_MULTIPLIER + patch.toLong()

    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        const val MAJOR_MULTIPLIER: Long = 1_000_000L
        const val MINOR_MULTIPLIER: Long = 1_000L
        private const val COMPONENT_BASE: Int = 1_000

        fun fromVersionCode(versionCode: Long): SemanticVersion? {
            if (versionCode < 0) return null
            val majorLong = versionCode / MAJOR_MULTIPLIER
            if (majorLong > Int.MAX_VALUE.toLong()) return null
            val remainder = versionCode % MAJOR_MULTIPLIER
            val minor = (remainder / MINOR_MULTIPLIER).toInt()
            val patch = (remainder % MINOR_MULTIPLIER).toInt()
            return SemanticVersion(
                major = majorLong.toInt(),
                minor = minor,
                patch = patch
            )
        }
    }
}
