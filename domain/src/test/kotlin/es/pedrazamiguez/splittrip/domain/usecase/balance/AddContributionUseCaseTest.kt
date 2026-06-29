package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.impl.ContributionValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.AddContributionUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AddContributionUseCaseTest {

    private lateinit var contributionRepository: ContributionRepository
    private lateinit var groupMembershipService: GroupMembershipService
    private lateinit var subunitRepository: SubunitRepository
    private lateinit var authenticationService: AuthenticationService
    private lateinit var groupRepository: GroupRepository
    private val contributionValidationService = ContributionValidationServiceImpl()
    private lateinit var useCase: AddContributionUseCase

    private val groupId = "group-123"
    private val currentUserId = "user-123"
    private val contribution = Contribution(
        id = "contribution-1",
        groupId = groupId,
        amount = 5000L,
        currency = "EUR"
    )

    @BeforeEach
    fun setUp() {
        contributionRepository = mockk(relaxed = true)
        groupMembershipService = mockk()
        subunitRepository = mockk()
        authenticationService = mockk()
        groupRepository = mockk()

        coEvery { groupMembershipService.requireMembership(any()) } just Runs
        every { authenticationService.requireUserId() } returns currentUserId
        coEvery { subunitRepository.getGroupSubunits(any()) } returns emptyList()
        coEvery { groupRepository.getGroupById(any()) } returns mockk {
            every { status } returns GroupStatus.ACTIVE
        }

        useCase = AddContributionUseCaseImpl(
            contributionRepository,
            groupMembershipService,
            contributionValidationService,
            subunitRepository,
            authenticationService,
            groupRepository
        )
    }

    // ── Group Archived validation ─────────────────────────────────────────────

    @Nested
    inner class GroupArchivedValidation {

        @Test
        fun `fails when group is archived`() = runTest {
            coEvery { groupRepository.getGroupById(groupId) } returns mockk {
                every { status } returns GroupStatus.ARCHIVED
            }

            assertThrows<GroupArchivedException> {
                useCase(groupId, contribution)
            }
        }
    }

    // ── Delegation ────────────────────────────────────────────────────────────

    @Nested
    inner class Delegation {

        @Test
        fun `delegates to contribution repository`() = runTest {
            // When
            useCase(groupId, contribution)

            // Then
            coVerify(exactly = 1) { contributionRepository.addContribution(groupId, contribution) }
        }

        @Test
        fun `passes correct groupId and contribution`() = runTest {
            // When
            useCase(groupId, contribution)

            // Then
            coVerify {
                contributionRepository.addContribution(
                    match { it == groupId },
                    match { it == contribution }
                )
            }
        }
    }

    // ── Membership validation ─────────────────────────────────────────────────

    @Nested
    inner class MembershipValidation {

        @Test
        fun `throws NotGroupMemberException when user is not a member`() = runTest {
            // Given
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When / Then
            val exception = assertThrows<NotGroupMemberException> {
                useCase(groupId, contribution)
            }
            assertTrue(exception.groupId == groupId)
        }

        @Test
        fun `does not save contribution when membership check fails`() = runTest {
            // Given
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When
            runCatching { useCase(groupId, contribution) }

            // Then
            coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
        }

        @Test
        fun `calls requireMembership before saving`() = runTest {
            // When
            useCase(groupId, contribution)

            // Then
            coVerify(exactly = 1) { groupMembershipService.requireMembership(groupId) }
        }
    }

    // ── Amount validation ───────────────────────────────────────────────────────

    @Nested
    inner class AmountValidation {

        @Test
        fun `throws when amount is zero`() = runTest {
            val zeroContribution = contribution.copy(amount = 0L)

            val exception = assertThrows<IllegalArgumentException> {
                useCase(groupId, zeroContribution)
            }
            assertTrue(exception.message!!.contains("AMOUNT_MUST_BE_POSITIVE"))

            coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
        }

        @Test
        fun `throws when amount is negative`() = runTest {
            val negativeContribution = contribution.copy(amount = -100L)

            val exception = assertThrows<IllegalArgumentException> {
                useCase(groupId, negativeContribution)
            }
            assertTrue(exception.message!!.contains("AMOUNT_MUST_BE_POSITIVE"))

            coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
        }
    }

    // ── Scope validation ──────────────────────────────────────────────────────

    @Nested
    inner class SubunitValidation {

        private val testSubunit = Subunit(
            id = "sub-1",
            name = "Couple A",
            groupId = groupId,
            memberIds = listOf(currentUserId, "user-456")
        )

        @Test
        fun `allows USER scope contribution without subunit`() = runTest {
            val noSubunitContribution = contribution.copy(
                contributionScope = PayerType.USER,
                subunitId = null
            )

            useCase(groupId, noSubunitContribution)

            coVerify(exactly = 1) { contributionRepository.addContribution(groupId, noSubunitContribution) }
        }

        @Test
        fun `allows GROUP scope contribution`() = runTest {
            val groupContribution = contribution.copy(
                contributionScope = PayerType.GROUP,
                subunitId = null
            )

            useCase(groupId, groupContribution)

            coVerify(exactly = 1) { contributionRepository.addContribution(groupId, groupContribution) }
            // GROUP scope should NOT validate subunits
            coVerify(exactly = 0) { subunitRepository.getGroupSubunits(any()) }
        }

        @Test
        fun `allows contribution with valid subunit`() = runTest {
            coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(testSubunit)
            val subunitContribution = contribution.copy(
                contributionScope = PayerType.SUBUNIT,
                subunitId = "sub-1"
            )

            useCase(groupId, subunitContribution)

            coVerify(exactly = 1) { contributionRepository.addContribution(groupId, subunitContribution) }
        }

        @Test
        fun `throws when subunit does not exist`() = runTest {
            coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(testSubunit)
            val invalidContribution = contribution.copy(
                contributionScope = PayerType.SUBUNIT,
                subunitId = "nonexistent-sub"
            )

            val exception = assertThrows<IllegalArgumentException> {
                useCase(groupId, invalidContribution)
            }
            assertTrue(exception.message!!.contains("SUBUNIT_NOT_FOUND"))

            coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
        }

        @Test
        fun `throws when user is not member of subunit`() = runTest {
            val otherSubunit = testSubunit.copy(memberIds = listOf("user-999"))
            coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(otherSubunit)
            val subunitContribution = contribution.copy(
                contributionScope = PayerType.SUBUNIT,
                subunitId = "sub-1"
            )

            val exception = assertThrows<IllegalArgumentException> {
                useCase(groupId, subunitContribution)
            }
            assertTrue(exception.message!!.contains("USER_NOT_IN_SUBUNIT"))

            coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
        }

        // ── Impersonation validation ──────────────────────────────────────

        @Nested
        @DisplayName("Impersonation")
        inner class ImpersonationValidation {

            private val targetUserId = "target-user-789"

            private val subunitWithTarget = Subunit(
                id = "sub-1",
                name = "Couple A",
                groupId = groupId,
                memberIds = listOf(targetUserId, "user-456")
            )

            @Test
            fun `allows impersonated user in valid subunit (actor not in subunit)`() = runTest {
                // Given — target is in subunit, actor is NOT
                coEvery {
                    groupMembershipService.requireUserInGroup(groupId, targetUserId)
                } just Runs
                coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(subunitWithTarget)

                val subunitContribution = contribution.copy(
                    userId = targetUserId,
                    contributionScope = PayerType.SUBUNIT,
                    subunitId = "sub-1"
                )

                // When
                useCase(groupId, subunitContribution)

                // Then
                coVerify(exactly = 1) {
                    groupMembershipService.requireUserInGroup(groupId, targetUserId)
                }
                coVerify(exactly = 1) {
                    contributionRepository.addContribution(groupId, subunitContribution)
                }
            }

            @Test
            fun `throws when impersonated user is not member of subunit`() = runTest {
                // Given — target is a group member but NOT in the specified subunit
                val subunitWithoutTarget = testSubunit.copy(
                    memberIds = listOf("user-999")
                )
                coEvery {
                    groupMembershipService.requireUserInGroup(groupId, targetUserId)
                } just Runs
                coEvery {
                    subunitRepository.getGroupSubunits(groupId)
                } returns listOf(subunitWithoutTarget)

                val subunitContribution = contribution.copy(
                    userId = targetUserId,
                    contributionScope = PayerType.SUBUNIT,
                    subunitId = "sub-1"
                )

                // When / Then
                val exception = assertThrows<IllegalArgumentException> {
                    useCase(groupId, subunitContribution)
                }
                assertTrue(exception.message!!.contains("USER_NOT_IN_SUBUNIT"))

                coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
            }

            @Test
            fun `throws when impersonated user is not a group member`() = runTest {
                // Given — target is NOT a member of the group at all
                coEvery {
                    groupMembershipService.requireUserInGroup(groupId, targetUserId)
                } throws NotGroupMemberException(groupId = groupId, userId = targetUserId)

                val subunitContribution = contribution.copy(
                    userId = targetUserId,
                    contributionScope = PayerType.SUBUNIT,
                    subunitId = "sub-1"
                )

                // When / Then
                val exception = assertThrows<NotGroupMemberException> {
                    useCase(groupId, subunitContribution)
                }
                assertTrue(exception.groupId == groupId)
                assertTrue(exception.userId == targetUserId)

                coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
            }

            @Test
            fun `does not call requireUserInGroup when userId is blank (self contribution)`() =
                runTest {
                    // Given — userId is blank → fallback to actor, no impersonation check
                    coEvery {
                        subunitRepository.getGroupSubunits(groupId)
                    } returns listOf(testSubunit)

                    val selfContribution = contribution.copy(
                        userId = "", // blank = self
                        contributionScope = PayerType.SUBUNIT,
                        subunitId = "sub-1"
                    )

                    // When
                    useCase(groupId, selfContribution)

                    // Then — requireUserInGroup should NOT be called
                    coVerify(exactly = 0) {
                        groupMembershipService.requireUserInGroup(any(), any())
                    }
                    coVerify(exactly = 1) {
                        contributionRepository.addContribution(groupId, selfContribution)
                    }
                }
        }
    }
}
