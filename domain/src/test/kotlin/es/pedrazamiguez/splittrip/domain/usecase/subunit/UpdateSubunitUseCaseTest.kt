package es.pedrazamiguez.splittrip.domain.usecase.subunit

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.exception.ValidationException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.SubunitValidationService
import es.pedrazamiguez.splittrip.domain.usecase.subunit.impl.UpdateSubunitUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UpdateSubunitUseCase")
class UpdateSubunitUseCaseTest {

    private lateinit var subunitRepository: SubunitRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var groupMembershipService: GroupMembershipService
    private lateinit var subunitValidationService: SubunitValidationService
    private lateinit var useCase: UpdateSubunitUseCase

    private val groupId = "group-123"
    private val groupMembers = listOf("user-1", "user-2", "user-3")
    private val subunit = Subunit(
        id = "subunit-1",
        name = "Couple",
        memberIds = listOf("user-1", "user-2"),
        memberShares = mapOf("user-1" to BigDecimal("0.5"), "user-2" to BigDecimal("0.5"))
    )

    @BeforeEach
    fun setUp() {
        subunitRepository = mockk(relaxed = true)
        groupRepository = mockk()
        groupMembershipService = mockk()
        subunitValidationService = mockk()

        coEvery { groupMembershipService.requireMembership(any()) } just Runs
        coEvery { groupRepository.getGroupById(groupId) } returns Group(
            id = groupId,
            name = "Test Group",
            members = groupMembers
        )
        coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(subunit)
        every {
            subunitValidationService.validate(
                subunit = any(),
                groupMemberIds = any(),
                existingSubunits = any(),
                excludeSubunitId = any()
            )
        } returns SubunitValidationService.ValidationResult.Valid(subunit)

        useCase = UpdateSubunitUseCaseImpl(
            subunitRepository = subunitRepository,
            groupRepository = groupRepository,
            groupMembershipService = groupMembershipService,
            subunitValidationService = subunitValidationService
        )
    }

    // ── Membership validation ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Membership validation")
    inner class MembershipValidation {

        @Test
        fun `fails with NotGroupMemberException when user is not a member`() = runTest {
            // Given
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When
            val result = useCase(groupId, subunit)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NotGroupMemberException)
        }

        @Test
        fun `does not update subunit when membership check fails`() = runTest {
            // Given
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When
            useCase(groupId, subunit)

            // Then
            coVerify(exactly = 0) { subunitRepository.updateSubunit(any(), any()) }
        }

        @Test
        fun `calls requireMembership with correct groupId`() = runTest {
            // When
            useCase(groupId, subunit)

            // Then
            coVerify(exactly = 1) { groupMembershipService.requireMembership(groupId) }
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Validation")
    inner class Validation {

        @Test
        fun `fails when group is archived`() = runTest {
            coEvery { groupRepository.getGroupById(groupId) } returns Group(
                id = groupId,
                name = "Test Group",
                members = groupMembers,
                status = GroupStatus.ARCHIVED
            )

            val result = useCase(groupId, subunit)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is GroupArchivedException)
        }

        @Test
        fun `fails when validation returns Invalid`() = runTest {
            // Given
            every {
                subunitValidationService.validate(
                    subunit = any(),
                    groupMemberIds = any(),
                    existingSubunits = any(),
                    excludeSubunitId = any()
                )
            } returns SubunitValidationService.ValidationResult.Invalid(
                SubunitValidationService.ValidationError.SHARES_DO_NOT_SUM
            )

            // When
            val result = useCase(groupId, subunit)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is ValidationException)
            assertTrue(result.exceptionOrNull()?.message?.contains("SHARES_DO_NOT_SUM") == true)
        }

        @Test
        fun `does not update subunit when validation fails`() = runTest {
            // Given
            every {
                subunitValidationService.validate(
                    subunit = any(),
                    groupMemberIds = any(),
                    existingSubunits = any(),
                    excludeSubunitId = any()
                )
            } returns SubunitValidationService.ValidationResult.Invalid(
                SubunitValidationService.ValidationError.MEMBER_NOT_IN_GROUP
            )

            // When
            useCase(groupId, subunit)

            // Then
            coVerify(exactly = 0) { subunitRepository.updateSubunit(any(), any()) }
        }

        @Test
        fun `passes excludeSubunitId for self-overlap check`() = runTest {
            // When
            useCase(groupId, subunit)

            // Then
            io.mockk.verify {
                subunitValidationService.validate(
                    subunit = subunit,
                    groupMemberIds = groupMembers,
                    existingSubunits = any(),
                    excludeSubunitId = "subunit-1"
                )
            }
        }

        @Test
        fun `fails when subunit ID is blank`() = runTest {
            // Given
            val blankIdSubunit = subunit.copy(id = "")

            // When
            val result = useCase(groupId, blankIdSubunit)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertTrue(result.exceptionOrNull()?.message?.contains("blank") == true)
        }

        @Test
        fun `does not call membership check when subunit ID is blank`() = runTest {
            // Given
            val blankIdSubunit = subunit.copy(id = "  ")

            // When
            useCase(groupId, blankIdSubunit)

            // Then
            coVerify(exactly = 0) { groupMembershipService.requireMembership(any()) }
        }

        @Test
        fun `fails when group is not found after membership check`() = runTest {
            // Given
            coEvery { groupRepository.getGroupById(groupId) } returns null

            // When
            val result = useCase(groupId, subunit)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertTrue(result.exceptionOrNull()?.message?.contains(groupId) == true)
        }
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    inner class HappyPath {

        @Test
        fun `delegates to repository on valid subunit`() = runTest {
            // When
            val result = useCase(groupId, subunit)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { subunitRepository.updateSubunit(groupId, subunit) }
        }

        @Test
        fun `passes normalized subunit from validation to repository`() = runTest {
            // Given
            val normalizedSubunit = subunit.copy(
                memberShares = mapOf("user-1" to BigDecimal("0.6"), "user-2" to BigDecimal("0.4"))
            )
            every {
                subunitValidationService.validate(
                    subunit = any(),
                    groupMemberIds = any(),
                    existingSubunits = any(),
                    excludeSubunitId = any()
                )
            } returns SubunitValidationService.ValidationResult.Valid(normalizedSubunit)

            // When
            useCase(groupId, subunit)

            // Then
            coVerify { subunitRepository.updateSubunit(groupId, normalizedSubunit) }
        }
    }
}
