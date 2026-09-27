package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.service.impl.SubunitShareDistributionServiceImpl
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Subunit")
class SubunitTest {

    @Nested
    @DisplayName("hasCustomShares")
    inner class HasCustomShares {

        @Test
        @DisplayName("returns false when memberShares is empty")
        fun returnsFalseWhenMemberSharesIsEmpty() {
            val subunit = Subunit(
                memberIds = listOf("u1", "u2"),
                memberShares = emptyMap()
            )

            assertFalse(subunit.hasCustomShares())
        }

        @Test
        @DisplayName("returns false when memberShares has single member")
        fun returnsFalseWhenMemberSharesHasSingleMember() {
            val subunit = Subunit(
                memberIds = listOf("u1"),
                memberShares = mapOf("u1" to BigDecimal.ONE)
            )

            assertFalse(subunit.hasCustomShares())
        }

        @Test
        @DisplayName("returns false when memberShares has two equal 50-50 shares")
        fun returnsFalseWhenMemberSharesHasTwoEqualShares() {
            val subunit = Subunit(
                memberIds = listOf("u1", "u2"),
                memberShares = mapOf(
                    "u1" to BigDecimal("0.5"),
                    "u2" to BigDecimal("0.5")
                )
            )

            assertFalse(subunit.hasCustomShares())
        }

        @Test
        @DisplayName("returns false when memberShares has three equal 1-3 shares from distributeEvenly")
        fun returnsFalseWhenMemberSharesHasThreeEqualSharesFromDistributeEvenly() {
            val memberIds = listOf("u1", "u2", "u3")
            val evenShares = SubunitShareDistributionServiceImpl().distributeEvenly(memberIds)
            val subunit = Subunit(
                memberIds = memberIds,
                memberShares = evenShares
            )

            assertFalse(subunit.hasCustomShares())
        }

        @Test
        @DisplayName("returns false when memberShares has slight rounding difference within tolerance")
        fun returnsFalseWhenMemberSharesHasSlightRoundingDifferenceWithinTolerance() {
            val subunit = Subunit(
                memberIds = listOf("u1", "u2", "u3"),
                memberShares = mapOf(
                    "u1" to BigDecimal("0.3334"),
                    "u2" to BigDecimal("0.3333"),
                    "u3" to BigDecimal("0.3333")
                )
            )

            assertFalse(subunit.hasCustomShares())
        }

        @Test
        @DisplayName("returns true when memberShares has custom 60-40 shares")
        fun returnsTrueWhenMemberSharesHasCustom6040Shares() {
            val subunit = Subunit(
                memberIds = listOf("u1", "u2"),
                memberShares = mapOf(
                    "u1" to BigDecimal("0.6"),
                    "u2" to BigDecimal("0.4")
                )
            )

            assertTrue(subunit.hasCustomShares())
        }

        @Test
        @DisplayName("returns true when memberShares has custom 30-30-25-15 shares")
        fun returnsTrueWhenMemberSharesHasCustom30302515Shares() {
            val subunit = Subunit(
                memberIds = listOf("u1", "u2", "u3", "u4"),
                memberShares = mapOf(
                    "u1" to BigDecimal("0.3"),
                    "u2" to BigDecimal("0.3"),
                    "u3" to BigDecimal("0.25"),
                    "u4" to BigDecimal("0.15")
                )
            )

            assertTrue(subunit.hasCustomShares())
        }
    }
}
