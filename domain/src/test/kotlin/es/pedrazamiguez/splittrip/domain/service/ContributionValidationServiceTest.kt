package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.impl.ContributionValidationServiceImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ContributionValidationService")
class ContributionValidationServiceTest {

    private val service = ContributionValidationServiceImpl()

    @Nested
    @DisplayName("validateAmount")
    inner class ValidateAmount {

        @Test
        fun `returns Valid for positive amount`() {
            val result = service.validateAmount(100L)
            assertTrue(result is ContributionValidationService.ValidationResult.Valid)
        }

        @Test
        fun `returns Invalid for zero amount`() {
            val result = service.validateAmount(0L)
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
            val invalid = result as ContributionValidationService.ValidationResult.Invalid
            assertTrue(invalid.error == ContributionValidationService.ValidationError.AMOUNT_MUST_BE_POSITIVE)
        }

        @Test
        fun `returns Invalid for negative amount`() {
            val result = service.validateAmount(-500L)
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
        }

        @Test
        fun `returns Valid for large amount`() {
            val result = service.validateAmount(999_999_99L)
            assertTrue(result is ContributionValidationService.ValidationResult.Valid)
        }

        @Test
        fun `returns Valid for 1 cent`() {
            val result = service.validateAmount(1L)
            assertTrue(result is ContributionValidationService.ValidationResult.Valid)
        }
    }

    @Nested
    @DisplayName("validate (full Contribution)")
    inner class ValidateContribution {

        @Test
        fun `returns Valid for contribution with positive amount`() {
            val contribution = Contribution(
                id = "test-1",
                groupId = "group-1",
                userId = "user-1",
                amount = 2550L,
                currency = "EUR"
            )
            val result = service.validate(contribution)
            assertTrue(result is ContributionValidationService.ValidationResult.Valid)
        }

        @Test
        fun `returns Invalid for contribution with zero amount`() {
            val contribution = Contribution(
                id = "test-1",
                groupId = "group-1",
                userId = "user-1",
                amount = 0L,
                currency = "EUR"
            )
            val result = service.validate(contribution)
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
        }
    }

    @Nested
    @DisplayName("validateSubunit")
    inner class ValidateSubunit {

        private val testSubunit = Subunit(
            id = "subunit-1",
            groupId = "group-1",
            name = "Antonio & Me",
            memberIds = listOf("user-1", "user-2")
        )

        private val groupSubunits = listOf(testSubunit)

        @Test
        fun `returns Valid when subunitId is null`() {
            val result = service.validateSubunit(
                subunitId = null,
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Valid)
        }

        @Test
        fun `returns Valid when user belongs to the subunit`() {
            val result = service.validateSubunit(
                subunitId = "subunit-1",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Valid)
        }

        @Test
        fun `returns Invalid SUBUNIT_NOT_FOUND when subunit does not exist`() {
            val result = service.validateSubunit(
                subunitId = "nonexistent-subunit",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
            assertEquals(
                ContributionValidationService.ValidationError.SUBUNIT_NOT_FOUND,
                (result as ContributionValidationService.ValidationResult.Invalid).error
            )
        }

        @Test
        fun `returns Invalid USER_NOT_IN_SUBUNIT when user is not a member`() {
            val result = service.validateSubunit(
                subunitId = "subunit-1",
                userId = "user-999",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
            assertEquals(
                ContributionValidationService.ValidationError.USER_NOT_IN_SUBUNIT,
                (result as ContributionValidationService.ValidationResult.Invalid).error
            )
        }

        @Test
        fun `returns Valid with empty subunits list when subunitId is null`() {
            val result = service.validateSubunit(
                subunitId = null,
                userId = "user-1",
                groupSubunits = emptyList()
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Valid)
        }

        @Test
        fun `returns Invalid SUBUNIT_NOT_FOUND with empty subunits list when subunitId is set`() {
            val result = service.validateSubunit(
                subunitId = "subunit-1",
                userId = "user-1",
                groupSubunits = emptyList()
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
            assertEquals(
                ContributionValidationService.ValidationError.SUBUNIT_NOT_FOUND,
                (result as ContributionValidationService.ValidationResult.Invalid).error
            )
        }

        @Test
        fun `validates correctly with multiple subunits`() {
            val subunit2 = Subunit(
                id = "subunit-2",
                groupId = "group-1",
                name = "Family",
                memberIds = listOf("user-3", "user-4", "user-5")
            )
            val multipleSubunits = listOf(testSubunit, subunit2)

            // user-3 belongs to subunit-2, not subunit-1
            val result1 = service.validateSubunit(
                subunitId = "subunit-1",
                userId = "user-3",
                groupSubunits = multipleSubunits
            )
            assertTrue(result1 is ContributionValidationService.ValidationResult.Invalid)
            assertEquals(
                ContributionValidationService.ValidationError.USER_NOT_IN_SUBUNIT,
                (result1 as ContributionValidationService.ValidationResult.Invalid).error
            )

            // user-3 belongs to subunit-2
            val result2 = service.validateSubunit(
                subunitId = "subunit-2",
                userId = "user-3",
                groupSubunits = multipleSubunits
            )
            assertTrue(result2 is ContributionValidationService.ValidationResult.Valid)
        }
    }

    @Nested
    @DisplayName("validateContributionScope")
    inner class ValidateContributionScope {

        private val testSubunit = Subunit(
            id = "subunit-1",
            groupId = "group-1",
            name = "Antonio & Me",
            memberIds = listOf("user-1", "user-2")
        )

        private val groupSubunits = listOf(testSubunit)

        // ── SUBUNIT scope ────────────────────────────────────────────────

        @Test
        fun `SUBUNIT scope returns Valid when user belongs to the subunit`() {
            val result = service.validateContributionScope(
                contributionScope = PayerType.SUBUNIT,
                subunitId = "subunit-1",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Valid)
        }

        @Test
        fun `SUBUNIT scope returns SUBUNIT_REQUIRED when subunitId is null`() {
            val result = service.validateContributionScope(
                contributionScope = PayerType.SUBUNIT,
                subunitId = null,
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
            assertEquals(
                ContributionValidationService.ValidationError.SUBUNIT_REQUIRED,
                (result as ContributionValidationService.ValidationResult.Invalid).error
            )
        }

        @Test
        fun `SUBUNIT scope returns SUBUNIT_REQUIRED when subunitId is blank`() {
            val result = service.validateContributionScope(
                contributionScope = PayerType.SUBUNIT,
                subunitId = "  ",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
            assertEquals(
                ContributionValidationService.ValidationError.SUBUNIT_REQUIRED,
                (result as ContributionValidationService.ValidationResult.Invalid).error
            )
        }

        @Test
        fun `SUBUNIT scope returns SUBUNIT_NOT_FOUND when subunit does not exist`() {
            val result = service.validateContributionScope(
                contributionScope = PayerType.SUBUNIT,
                subunitId = "nonexistent-subunit",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
            assertEquals(
                ContributionValidationService.ValidationError.SUBUNIT_NOT_FOUND,
                (result as ContributionValidationService.ValidationResult.Invalid).error
            )
        }

        @Test
        fun `SUBUNIT scope returns USER_NOT_IN_SUBUNIT when user is not a member`() {
            val result = service.validateContributionScope(
                contributionScope = PayerType.SUBUNIT,
                subunitId = "subunit-1",
                userId = "user-999",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
            assertEquals(
                ContributionValidationService.ValidationError.USER_NOT_IN_SUBUNIT,
                (result as ContributionValidationService.ValidationResult.Invalid).error
            )
        }

        // ── GROUP scope ──────────────────────────────────────────────────

        @Test
        fun `GROUP scope returns Valid when subunitId is null`() {
            val result = service.validateContributionScope(
                contributionScope = PayerType.GROUP,
                subunitId = null,
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Valid)
        }

        @Test
        fun `GROUP scope returns INVALID_SUBUNIT_FOR_SCOPE when subunitId is set`() {
            val result = service.validateContributionScope(
                contributionScope = PayerType.GROUP,
                subunitId = "subunit-1",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
            assertEquals(
                ContributionValidationService.ValidationError.INVALID_SUBUNIT_FOR_SCOPE,
                (result as ContributionValidationService.ValidationResult.Invalid).error
            )
        }

        // ── USER scope ──────────────────────────────────────────────────

        @Test
        fun `USER scope returns Valid when subunitId is null`() {
            val result = service.validateContributionScope(
                contributionScope = PayerType.USER,
                subunitId = null,
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Valid)
        }

        @Test
        fun `USER scope returns INVALID_SUBUNIT_FOR_SCOPE when subunitId is set`() {
            val result = service.validateContributionScope(
                contributionScope = PayerType.USER,
                subunitId = "subunit-1",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is ContributionValidationService.ValidationResult.Invalid)
            assertEquals(
                ContributionValidationService.ValidationError.INVALID_SUBUNIT_FOR_SCOPE,
                (result as ContributionValidationService.ValidationResult.Invalid).error
            )
        }
    }
}
