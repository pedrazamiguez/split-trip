package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.WithdrawalPoolOption
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GetAvailableWithdrawalPoolsUseCaseTest {

    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var useCase: GetAvailableWithdrawalPoolsUseCase

    private val groupId = "group-123"
    private val currency = "THB"
    private val userId = "user-456"
    private val subunitId = "subunit-789"

    private val sampleWithdrawal = CashWithdrawal(
        id = "w-1",
        groupId = groupId,
        amountWithdrawn = 100_000L,
        remainingAmount = 100_000L,
        currency = currency,
        deductedBaseAmount = 2700L,
        createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
    )

    @BeforeEach
    fun setUp() {
        cashWithdrawalRepository = mockk()
        // Default: all exact-scope queries return empty
        coEvery {
            cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(any(), any(), any(), any())
        } returns emptyList()
        useCase = GetAvailableWithdrawalPoolsUseCase(cashWithdrawalRepository)
    }

    // ── GROUP scope ───────────────────────────────────────────────────────────

    @Nested
    inner class GroupScope {

        @Test
        fun `returns single GROUP option when only GROUP pool has funds and no userId`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            } returns listOf(sampleWithdrawal)

            val result = useCase(groupId, currency, PayerType.GROUP)

            assertEquals(1, result.size)
            assertEquals(WithdrawalPoolOption(PayerType.GROUP), result.first())
        }

        @Test
        fun `returns empty list when GROUP pool has no funds and no userId`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            } returns emptyList()

            val result = useCase(groupId, currency, PayerType.GROUP)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `GROUP scope without userId skips USER pool probe`() = runTest {
            useCase(groupId, currency, PayerType.GROUP)

            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            }
            coVerify(exactly = 0) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    any(),
                    any(),
                    PayerType.USER,
                    any()
                )
            }
        }

        @Test
        fun `returns both GROUP and USER options when both pools have funds`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            } returns listOf(sampleWithdrawal)
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.USER,
                    userId
                )
            } returns listOf(sampleWithdrawal)

            val result = useCase(groupId, currency, PayerType.GROUP, userId)

            assertEquals(2, result.size)
            assertEquals(WithdrawalPoolOption(PayerType.GROUP), result[0])
            assertEquals(WithdrawalPoolOption(PayerType.USER, userId), result[1])
        }

        @Test
        fun `returns only USER option when GROUP pool is empty but USER pool has funds`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.USER,
                    userId
                )
            } returns listOf(sampleWithdrawal)

            val result = useCase(groupId, currency, PayerType.GROUP, userId)

            assertEquals(1, result.size)
            assertEquals(WithdrawalPoolOption(PayerType.USER, userId), result.first())
        }

        @Test
        fun `returns only GROUP option when USER pool is empty for GROUP payerType`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            } returns listOf(sampleWithdrawal)

            val result = useCase(groupId, currency, PayerType.GROUP, userId)

            assertEquals(1, result.size)
            assertEquals(WithdrawalPoolOption(PayerType.GROUP), result.first())
        }

        @Test
        fun `returns empty list when both GROUP and USER pools are empty`() = runTest {
            val result = useCase(groupId, currency, PayerType.GROUP, userId)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `GROUP pool is returned first, USER pool second`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            } returns listOf(sampleWithdrawal)
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.USER,
                    userId
                )
            } returns listOf(sampleWithdrawal)

            val result = useCase(groupId, currency, PayerType.GROUP, userId)

            assertEquals(PayerType.GROUP, result[0].scope)
            assertEquals(PayerType.USER, result[1].scope)
        }

        @Test
        fun `GROUP scope with userId queries both GROUP and USER pools`() = runTest {
            useCase(groupId, currency, PayerType.GROUP, userId)

            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            }
            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.USER,
                    userId
                )
            }
            coVerify(exactly = 0) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    any(),
                    any(),
                    PayerType.SUBUNIT,
                    any()
                )
            }
        }
    }

    // ── USER scope ────────────────────────────────────────────────────────────

    @Nested
    inner class UserScope {

        @Test
        fun `returns both USER and GROUP options when both pools have funds`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.USER,
                    userId
                )
            } returns listOf(sampleWithdrawal)
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            } returns listOf(sampleWithdrawal)

            val result = useCase(groupId, currency, PayerType.USER, userId)

            assertEquals(2, result.size)
            assertEquals(WithdrawalPoolOption(PayerType.USER, userId), result[0])
            assertEquals(WithdrawalPoolOption(PayerType.GROUP), result[1])
        }

        @Test
        fun `returns only USER option when GROUP pool is empty`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.USER,
                    userId
                )
            } returns listOf(sampleWithdrawal)

            val result = useCase(groupId, currency, PayerType.USER, userId)

            assertEquals(1, result.size)
            assertEquals(WithdrawalPoolOption(PayerType.USER, userId), result.first())
        }

        @Test
        fun `returns only GROUP option when USER pool is empty`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            } returns listOf(sampleWithdrawal)

            val result = useCase(groupId, currency, PayerType.USER, userId)

            assertEquals(1, result.size)
            assertEquals(WithdrawalPoolOption(PayerType.GROUP), result.first())
        }

        @Test
        fun `returns empty list when both USER and GROUP pools are empty`() = runTest {
            val result = useCase(groupId, currency, PayerType.USER, userId)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty list and skips USER probe when payerId is null`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            } returns listOf(sampleWithdrawal)

            val result = useCase(groupId, currency, PayerType.USER, payerId = null)

            // Only GROUP option returned — USER probe skipped because no payerId
            assertEquals(1, result.size)
            assertEquals(WithdrawalPoolOption(PayerType.GROUP), result.first())
            coVerify(exactly = 0) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    any(),
                    any(),
                    PayerType.USER,
                    any()
                )
            }
        }

        @Test
        fun `returns empty list and skips USER probe when payerId is blank`() = runTest {
            val result = useCase(groupId, currency, PayerType.USER, payerId = "   ")

            assertTrue(result.isEmpty())
            coVerify(exactly = 0) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    any(),
                    any(),
                    PayerType.USER,
                    any()
                )
            }
        }

        @Test
        fun `personal pool is returned first, GROUP pool second`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.USER,
                    userId
                )
            } returns listOf(sampleWithdrawal)
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            } returns listOf(sampleWithdrawal)

            val result = useCase(groupId, currency, PayerType.USER, userId)

            assertEquals(PayerType.USER, result[0].scope)
            assertEquals(PayerType.GROUP, result[1].scope)
        }
    }

    // ── SUBUNIT scope ─────────────────────────────────────────────────────────

    @Nested
    inner class SubunitScope {

        @Test
        fun `returns both SUBUNIT and GROUP options when both pools have funds`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.SUBUNIT,
                    subunitId
                )
            } returns listOf(sampleWithdrawal)
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            } returns listOf(sampleWithdrawal)

            val result = useCase(groupId, currency, PayerType.SUBUNIT, subunitId)

            assertEquals(2, result.size)
            assertEquals(WithdrawalPoolOption(PayerType.SUBUNIT, subunitId), result[0])
            assertEquals(WithdrawalPoolOption(PayerType.GROUP), result[1])
        }

        @Test
        fun `returns empty list and skips SUBUNIT probe when payerId is null`() = runTest {
            val result = useCase(groupId, currency, PayerType.SUBUNIT, payerId = null)

            assertTrue(result.isEmpty())
            coVerify(exactly = 0) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    any(),
                    any(),
                    PayerType.SUBUNIT,
                    any()
                )
            }
        }
    }
}
