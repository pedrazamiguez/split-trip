package es.pedrazamiguez.splittrip.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SemanticVersionTest {

    @Test
    fun `fromVersionCode decodes zero as 0_0_0`() {
        assertEquals(SemanticVersion(0, 0, 0), SemanticVersion.fromVersionCode(0L))
    }

    @Test
    fun `fromVersionCode decodes pre-1_0_0 closed testing version codes`() {
        assertEquals(SemanticVersion(0, 99, 0), SemanticVersion.fromVersionCode(99_000L))
        assertEquals(SemanticVersion(0, 99, 1), SemanticVersion.fromVersionCode(99_001L))
    }

    @Test
    fun `fromVersionCode decodes production major minor patch codes`() {
        assertEquals(SemanticVersion(1, 0, 0), SemanticVersion.fromVersionCode(1_000_000L))
        assertEquals(SemanticVersion(1, 2, 3), SemanticVersion.fromVersionCode(1_002_003L))
        assertEquals(SemanticVersion(2, 15, 99), SemanticVersion.fromVersionCode(2_015_099L))
    }

    @Test
    fun `fromVersionCode decodes maximum minor and patch bounds 999`() {
        assertEquals(SemanticVersion(1, 999, 999), SemanticVersion.fromVersionCode(1_999_999L))
    }

    @Test
    fun `fromVersionCode returns null for negative version codes`() {
        assertNull(SemanticVersion.fromVersionCode(-1L))
    }

    @Test
    fun `fromVersionCode returns null when major overflows Int`() {
        val overflowCode = (Int.MAX_VALUE.toLong() + 1L) * SemanticVersion.MAJOR_MULTIPLIER
        assertNull(SemanticVersion.fromVersionCode(overflowCode))
    }

    @Test
    fun `toVersionCode round-trips with fromVersionCode`() {
        val version = SemanticVersion(1, 4, 2)
        val code = version.toVersionCode()
        assertEquals(1_004_002L, code)
        assertEquals(version, SemanticVersion.fromVersionCode(code))
    }

    @Test
    fun `constructor rejects negative major or out-of-range minor or patch`() {
        assertThrows<IllegalArgumentException> { SemanticVersion(-1, 0, 0) }
        assertThrows<IllegalArgumentException> { SemanticVersion(1, -1, 0) }
        assertThrows<IllegalArgumentException> { SemanticVersion(1, 1000, 0) }
        assertThrows<IllegalArgumentException> { SemanticVersion(1, 0, -1) }
        assertThrows<IllegalArgumentException> { SemanticVersion(1, 0, 1000) }
    }

    @Test
    fun `compareTo orders versions by major then minor then patch`() {
        val v100 = SemanticVersion(1, 0, 0)
        val v101 = SemanticVersion(1, 0, 1)
        val v110 = SemanticVersion(1, 1, 0)
        val v200 = SemanticVersion(2, 0, 0)

        assertTrue(v100 < v101)
        assertTrue(v101 < v110)
        assertTrue(v110 < v200)
        assertEquals(0, v100.compareTo(SemanticVersion(1, 0, 0)))
        assertEquals("1.0.1", v101.toString())
    }
}
