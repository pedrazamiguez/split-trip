package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.CashWithdrawalValidationService
import es.pedrazamiguez.splittrip.domain.service.CashWithdrawalValidationService.ValidationResult
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.AddCashWithdrawalUseCaseImpl
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

class AddCashWithdrawalUseCaseTest {

    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var validationService: CashWithdrawalValidationService
    private lateinit var groupMembershipService: GroupMembershipService
    private lateinit var subunitRepository: SubunitRepository
    private lateinit var authenticationService: AuthenticationService
    private lateinit var useCase: AddCashWithdrawalUseCase

    private val groupId = "group-123"
    private val withdrawal = CashWithdrawal(
        id = "cw-1",
        groupId = groupId,
        amountWithdrawn = 100000L,
        remainingAmount = 100000L,
        currency = "THB",
        deductedBaseAmount = 2700L,
        exchangeRate = BigDecimal("37.037")
    )

    @BeforeEach
    fun setUp() {
        cashWithdrawalRepository = mockk(relaxed = true)
        validationService = mockk()
        groupMembershipService = mockk()
        subunitRepository = mockk(relaxed = true)
        authenticationService = mockk(relaxed = true)
        coEvery { groupMembershipService.requireMembership(any()) } just Runs
        every { validationService.validateAmountWithdrawn(any()) } returns ValidationResult.Valid
        every { validationService.validateDeductedBaseAmount(any()) } returns ValidationResult.Valid
        every { validationService.validateCurrency(any()) } returns ValidationResult.Valid
        every { validationService.validateExchangeRate(any()) } returns ValidationResult.Valid
        useCase = AddCashWithdrawalUseCaseImpl(
            cashWithdrawalRepository,
            validationService,
            groupMembershipService,
            subunitRepository,
            authenticationService
        )
    }

    // ── Membership validation ─────────────────────────────────────────────────

    @Nested
    inner class MembershipValidation {

        @Test
        fun `fails with NotGroupMemberException when user is not a member`() = runTest {
            // Given
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When
            val result = useCase(groupId, withdrawal)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NotGroupMemberException)
        }

        @Test
        fun `does not save withdrawal when membership check fails`() = runTest {
            // Given
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When
            useCase(groupId, withdrawal)

            // Then
            coVerify(exactly = 0) { cashWithdrawalRepository.addWithdrawal(any(), any()) }
        }

        @Test
        fun `calls requireMembership with correct groupId on success`() = runTest {
            // When
            useCase(groupId, withdrawal)

            // Then
            coVerify(exactly = 1) { groupMembershipService.requireMembership(groupId) }
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    inner class Validation {

        @Test
        fun `fails when groupId is null`() = runTest {
            val result = useCase(null, withdrawal)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Group ID") == true)
        }

        @Test
        fun `fails when groupId is blank`() = runTest {
            val result = useCase("  ", withdrawal)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Group ID") == true)
        }

        @Test
        fun `fails when amount withdrawn is invalid`() = runTest {
            every { validationService.validateAmountWithdrawn(any()) } returns
                ValidationResult.Invalid(CashWithdrawalValidationService.ValidationError.AMOUNT_MUST_BE_POSITIVE)

            val result = useCase(groupId, withdrawal)

            assertTrue(result.isFailure)
            coVerify(exactly = 0) { cashWithdrawalRepository.addWithdrawal(any(), any()) }
        }

        @Test
        fun `fails when deducted base amount is invalid`() = runTest {
            every { validationService.validateDeductedBaseAmount(any()) } returns
                ValidationResult.Invalid(
                    CashWithdrawalValidationService.ValidationError.DEDUCTED_AMOUNT_MUST_BE_POSITIVE
                )

            val result = useCase(groupId, withdrawal)

            assertTrue(result.isFailure)
            coVerify(exactly = 0) { cashWithdrawalRepository.addWithdrawal(any(), any()) }
        }

        @Test
        fun `fails when currency is invalid`() = runTest {
            every { validationService.validateCurrency(any()) } returns
                ValidationResult.Invalid(CashWithdrawalValidationService.ValidationError.CURRENCY_REQUIRED)

            val result = useCase(groupId, withdrawal)

            assertTrue(result.isFailure)
            coVerify(exactly = 0) { cashWithdrawalRepository.addWithdrawal(any(), any()) }
        }

        @Test
        fun `fails when exchange rate is invalid`() = runTest {
            every { validationService.validateExchangeRate(any()) } returns
                ValidationResult.Invalid(CashWithdrawalValidationService.ValidationError.EXCHANGE_RATE_MUST_BE_POSITIVE)

            val result = useCase(groupId, withdrawal)

            assertTrue(result.isFailure)
            coVerify(exactly = 0) { cashWithdrawalRepository.addWithdrawal(any(), any()) }
        }
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    inner class HappyPath {

        @Test
        fun `saves withdrawal when all validations pass`() = runTest {
            // When
            val result = useCase(groupId, withdrawal)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { cashWithdrawalRepository.addWithdrawal(groupId, withdrawal) }
        }
    }

    // ── Withdrawal scope validation ───────────────────────────────────────────

    @Nested
    inner class WithdrawalScopeValidation {

        private val testUserId = "user-456"
        private val subunitId = "subunit-1"
        private val subunit = Subunit(
            id = subunitId,
            name = "Couple",
            groupId = groupId,
            memberIds = listOf(testUserId, "user-789")
        )

        @Test
        fun `SUBUNIT-scoped withdrawal succeeds when scope validation passes`() = runTest {
            // Given
            val subunitWithdrawal = withdrawal.copy(
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = subunitId
            )
            every { authenticationService.requireUserId() } returns testUserId
            coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(subunit)
            every {
                validationService.validateWithdrawalScope(
                    PayerType.SUBUNIT,
                    subunitId,
                    testUserId,
                    listOf(subunit)
                )
            } returns ValidationResult.Valid

            // When
            val result = useCase(groupId, subunitWithdrawal)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { cashWithdrawalRepository.addWithdrawal(groupId, subunitWithdrawal) }
        }

        @Test
        fun `SUBUNIT-scoped withdrawal fails when user is not in subunit`() = runTest {
            // Given
            val subunitWithdrawal = withdrawal.copy(
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = subunitId
            )
            every { authenticationService.requireUserId() } returns testUserId
            coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(subunit)
            every {
                validationService.validateWithdrawalScope(
                    PayerType.SUBUNIT,
                    subunitId,
                    testUserId,
                    listOf(subunit)
                )
            } returns ValidationResult.Invalid(
                CashWithdrawalValidationService.ValidationError.USER_NOT_IN_SUBUNIT
            )

            // When
            val result = useCase(groupId, subunitWithdrawal)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 0) { cashWithdrawalRepository.addWithdrawal(any(), any()) }
        }

        @Test
        fun `GROUP-scoped withdrawal skips scope validation entirely`() = runTest {
            // Given — withdrawal defaults to GROUP scope with null subunitId

            // When
            val result = useCase(groupId, withdrawal)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 0) {
                validationService.validateWithdrawalScope(any(), any(), any(), any())
            }
            coVerify(exactly = 0) { authenticationService.requireUserId() }
            coVerify(exactly = 0) { subunitRepository.getGroupSubunits(any()) }
        }

        @Test
        fun `USER-scoped withdrawal skips scope validation entirely`() = runTest {
            // Given
            val personalWithdrawal = withdrawal.copy(
                withdrawalScope = PayerType.USER,
                subunitId = null
            )

            // When
            val result = useCase(groupId, personalWithdrawal)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 0) {
                validationService.validateWithdrawalScope(any(), any(), any(), any())
            }
        }

        @Test
        fun `SUBUNIT scope with null subunitId still triggers scope validation`() = runTest {
            // withdrawalScope == SUBUNIT (True) || subunitId == null (False) → enters block
            val subunitWithdrawal = withdrawal.copy(
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = null
            )
            every { authenticationService.requireUserId() } returns testUserId
            coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(subunit)
            every {
                validationService.validateWithdrawalScope(
                    PayerType.SUBUNIT,
                    null,
                    testUserId,
                    listOf(subunit)
                )
            } returns ValidationResult.Valid

            val result = useCase(groupId, subunitWithdrawal)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                validationService.validateWithdrawalScope(PayerType.SUBUNIT, null, testUserId, any())
            }
        }

        @Test
        fun `GROUP scope with non-null subunitId triggers scope validation`() = runTest {
            // withdrawalScope == SUBUNIT (False) || subunitId != null (True) → enters block
            val hybridWithdrawal = withdrawal.copy(
                withdrawalScope = PayerType.GROUP,
                subunitId = subunitId
            )
            every { authenticationService.requireUserId() } returns testUserId
            coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(subunit)
            every {
                validationService.validateWithdrawalScope(
                    PayerType.GROUP,
                    subunitId,
                    testUserId,
                    listOf(subunit)
                )
            } returns ValidationResult.Valid

            val result = useCase(groupId, hybridWithdrawal)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                validationService.validateWithdrawalScope(PayerType.GROUP, subunitId, testUserId, any())
            }
        }

        // ── Impersonation validation ──────────────────────────────────────

        @Nested
        @DisplayName("Impersonation")
        inner class ImpersonationValidation {

            private val targetUserId = "target-user-789"

            @Test
            fun `allows impersonated user in valid subunit (actor not in subunit)`() = runTest {
                // Given — target is in subunit, actor is NOT
                val subunitWithTarget = subunit.copy(
                    memberIds = listOf(targetUserId, "user-other")
                )
                coEvery {
                    groupMembershipService.requireUserInGroup(groupId, targetUserId)
                } just Runs
                coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(subunitWithTarget)
                every {
                    validationService.validateWithdrawalScope(
                        PayerType.SUBUNIT,
                        subunitId,
                        targetUserId,
                        listOf(subunitWithTarget)
                    )
                } returns ValidationResult.Valid

                val subunitWithdrawal = withdrawal.copy(
                    withdrawnBy = targetUserId,
                    withdrawalScope = PayerType.SUBUNIT,
                    subunitId = subunitId
                )

                // When
                val result = useCase(groupId, subunitWithdrawal)

                // Then
                assertTrue(result.isSuccess)
                coVerify(exactly = 1) {
                    groupMembershipService.requireUserInGroup(groupId, targetUserId)
                }
                coVerify(exactly = 1) {
                    cashWithdrawalRepository.addWithdrawal(groupId, subunitWithdrawal)
                }
            }

            @Test
            fun `fails when impersonated user is not member of subunit`() = runTest {
                // Given — target is a group member but NOT in the specified subunit
                coEvery {
                    groupMembershipService.requireUserInGroup(groupId, targetUserId)
                } just Runs
                coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(subunit)
                every {
                    validationService.validateWithdrawalScope(
                        PayerType.SUBUNIT,
                        subunitId,
                        targetUserId,
                        listOf(subunit)
                    )
                } returns ValidationResult.Invalid(
                    CashWithdrawalValidationService.ValidationError.USER_NOT_IN_SUBUNIT
                )

                val subunitWithdrawal = withdrawal.copy(
                    withdrawnBy = targetUserId,
                    withdrawalScope = PayerType.SUBUNIT,
                    subunitId = subunitId
                )

                // When
                val result = useCase(groupId, subunitWithdrawal)

                // Then
                assertTrue(result.isFailure)
                coVerify(exactly = 0) { cashWithdrawalRepository.addWithdrawal(any(), any()) }
            }

            @Test
            fun `fails when impersonated user is not a group member`() = runTest {
                // Given — target is NOT a member of the group at all
                coEvery {
                    groupMembershipService.requireUserInGroup(groupId, targetUserId)
                } throws NotGroupMemberException(groupId = groupId, userId = targetUserId)

                val subunitWithdrawal = withdrawal.copy(
                    withdrawnBy = targetUserId,
                    withdrawalScope = PayerType.SUBUNIT,
                    subunitId = subunitId
                )

                // When
                val result = useCase(groupId, subunitWithdrawal)

                // Then
                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is NotGroupMemberException)
                coVerify(exactly = 0) { cashWithdrawalRepository.addWithdrawal(any(), any()) }
            }

            @Test
            fun `does not call requireUserInGroup when withdrawnBy is blank (self withdrawal)`() =
                runTest {
                    // Given — withdrawnBy is blank → fallback to actor, no impersonation check
                    every { authenticationService.requireUserId() } returns testUserId
                    coEvery { subunitRepository.getGroupSubunits(groupId) } returns listOf(subunit)
                    every {
                        validationService.validateWithdrawalScope(
                            PayerType.SUBUNIT,
                            subunitId,
                            testUserId,
                            listOf(subunit)
                        )
                    } returns ValidationResult.Valid

                    val selfWithdrawal = withdrawal.copy(
                        withdrawnBy = "", // blank = self
                        withdrawalScope = PayerType.SUBUNIT,
                        subunitId = subunitId
                    )

                    // When
                    val result = useCase(groupId, selfWithdrawal)

                    // Then — requireUserInGroup should NOT be called
                    assertTrue(result.isSuccess)
                    coVerify(exactly = 0) {
                        groupMembershipService.requireUserInGroup(any(), any())
                    }
                    coVerify(exactly = 1) {
                        cashWithdrawalRepository.addWithdrawal(groupId, selfWithdrawal)
                    }
                }
        }
    }
}
