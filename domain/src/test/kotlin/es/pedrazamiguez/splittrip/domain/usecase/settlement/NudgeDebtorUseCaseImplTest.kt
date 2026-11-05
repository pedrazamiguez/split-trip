package es.pedrazamiguez.splittrip.domain.usecase.settlement

import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.repository.SettlementNudgeRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EphemeralSettlementIdHelper
import es.pedrazamiguez.splittrip.domain.usecase.settlement.impl.NudgeDebtorUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NudgeDebtorUseCaseImplTest {

    private val settlementRepository: SettlementRepository = mockk()
    private val settlementNudgeRepository: SettlementNudgeRepository = mockk()
    private val appConfigService: AppConfigService = mockk()
    private val authenticationService: AuthenticationService = mockk()

    private lateinit var useCase: NudgeDebtorUseCase

    @BeforeEach
    fun setUp() {
        every { appConfigService.settlementNudgeRateLimitHours } returns MutableStateFlow(24L)
        useCase = NudgeDebtorUseCaseImpl(
            settlementRepository = settlementRepository,
            settlementNudgeRepository = settlementNudgeRepository,
            appConfigService = appConfigService,
            authenticationService = authenticationService
        )
    }

    private fun createRecord(
        id: String,
        fromUserId: String,
        toUserId: String
    ) = SettlementRecord(
        id = id,
        groupId = "g1",
        settlement = Settlement(
            fromUserId = fromUserId,
            toUserId = toUserId,
            amount = 1000L,
            currency = "EUR"
        ),
        status = SettlementStatus.SUGGESTED,
        createdAt = LocalDateTime.now()
    )

    @Test
    fun `invoke whenUserNotAuthenticated returnsFailure`() = runTest {
        every { authenticationService.currentUserId() } returns null

        val result = useCase("g1", "s1")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { settlementNudgeRepository.sendDebtorNudge(any(), any(), any(), any()) }
    }

    @Test
    fun `invoke whenSettlementNotFound returnsFailure`() = runTest {
        every { authenticationService.currentUserId() } returns "user1"
        coEvery { settlementRepository.getSettlementById("s1") } returns null

        val result = useCase("g1", "s1")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { settlementNudgeRepository.sendDebtorNudge(any(), any(), any(), any()) }
    }

    @Test
    fun `invoke whenUserIsNotCreditor returnsFailure`() = runTest {
        every { authenticationService.currentUserId() } returns "debtor1"
        val record = createRecord("s1", fromUserId = "debtor1", toUserId = "creditor1")
        coEvery { settlementRepository.getSettlementById("s1") } returns record

        val result = useCase("g1", "s1")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { settlementNudgeRepository.sendDebtorNudge(any(), any(), any(), any()) }
    }

    @Test
    fun `invoke whenRateLimited returnsFailureWithoutSendingNudge`() = runTest {
        every { authenticationService.currentUserId() } returns "creditor1"
        val record = createRecord("s1", fromUserId = "debtor1", toUserId = "creditor1")
        coEvery { settlementRepository.getSettlementById("s1") } returns record
        val recentTimestamp = System.currentTimeMillis() - (1 * 3600 * 1000L) // 1 hour ago (within 24h limit)
        coEvery { settlementNudgeRepository.getLastNudgeTimestamp("s1") } returns recentTimestamp

        val result = useCase("g1", "s1")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { settlementNudgeRepository.sendDebtorNudge(any(), any(), any(), any()) }
    }

    @Test
    fun `invoke whenValidAndNotRateLimited sendsNudgeAndRecordsTimestamp`() = runTest {
        every { authenticationService.currentUserId() } returns "creditor1"
        val record = createRecord("s1", fromUserId = "debtor1", toUserId = "creditor1")
        coEvery { settlementRepository.getSettlementById("s1") } returns record
        coEvery { settlementNudgeRepository.getLastNudgeTimestamp("s1") } returns 0L
        coEvery { settlementNudgeRepository.sendDebtorNudge("g1", "s1", 1000L, "EUR") } returns Result.success(Unit)
        coEvery { settlementNudgeRepository.recordNudgeTimestamp(eq("s1"), any()) } returns Unit

        val result = useCase("g1", "s1")

        assertFalse(result.isFailure)
        coVerify(exactly = 1) { settlementNudgeRepository.sendDebtorNudge("g1", "s1", 1000L, "EUR") }
        coVerify(exactly = 1) { settlementNudgeRepository.recordNudgeTimestamp(eq("s1"), any()) }
    }

    @Test
    fun `invoke whenSettlementIsEphemeral returnsFailureWithoutQueryingRepository`() = runTest {
        every { authenticationService.currentUserId() } returns "creditor1"
        val ephemeralId = EphemeralSettlementIdHelper.createId(
            fromUserId = "debtor1",
            toUserId = "creditor1",
            sourcePocket = SettlementPocketType.POCKET,
            currency = "EUR",
            amount = 1000L
        )

        val result = useCase("g1", ephemeralId)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { settlementRepository.getSettlementById(any()) }
        coVerify(exactly = 0) { settlementNudgeRepository.sendDebtorNudge(any(), any(), any(), any()) }
    }
}
