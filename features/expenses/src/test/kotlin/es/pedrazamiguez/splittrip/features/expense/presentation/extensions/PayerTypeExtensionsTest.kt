package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PayerTypeExtensionsTest {

    @Nested
    inner class ToFundingSourceStringRes {

        @Test
        fun `GROUP maps to group pocket string resource`() {
            assertEquals(R.string.funding_source_group_pocket, PayerType.GROUP.toFundingSourceStringRes())
        }

        @Test
        fun `USER maps to your money string resource`() {
            assertEquals(R.string.funding_source_your_money, PayerType.USER.toFundingSourceStringRes())
        }

        @Test
        fun `SUBUNIT throws IllegalStateException`() {
            assertThrows(IllegalStateException::class.java) {
                PayerType.SUBUNIT.toFundingSourceStringRes()
            }
        }
    }

    @Nested
    inner class ToFundingSourceDescriptionRes {

        @Test
        fun `GROUP maps to group pocket description resource`() {
            assertEquals(
                R.string.funding_source_group_pocket_description,
                PayerType.GROUP.toFundingSourceDescriptionRes()
            )
        }

        @Test
        fun `USER maps to your money description resource`() {
            assertEquals(
                R.string.funding_source_your_money_description,
                PayerType.USER.toFundingSourceDescriptionRes()
            )
        }

        @Test
        fun `SUBUNIT throws IllegalStateException`() {
            assertThrows(IllegalStateException::class.java) {
                PayerType.SUBUNIT.toFundingSourceDescriptionRes()
            }
        }
    }
}
