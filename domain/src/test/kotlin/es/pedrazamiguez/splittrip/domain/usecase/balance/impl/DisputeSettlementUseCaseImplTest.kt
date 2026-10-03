package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DisputeSettlementUseCaseImplTest {

    private val settlementRepository = mockk<SettlementRepository>()
    private val authenticationService = mockk<AuthenticationService>()
    private lateinit var useCase: DisputeSettlementUseCaseImpl

    private val groupId = "group-123"
    private val settlementId = "settlement-1"
    private val payerId = "user-payer"
    private val payeeId = "user-payee"
    private val baseSettlement = Settlement(
        fromUserId = payerId,
        toUserId = payeeId,
        amount = 1000L,
        currency = "EUR",
        sourcePocket = SettlementPocketType.CASH
    )

    @BeforeEach
    fun setUp() {
        useCase = DisputeSettlementUseCaseImpl(
            settlementRepository = settlementRepository,
            authenticationService = authenticationService
        )
    }

    @Test
    fun `payer disputes SUGGESTED settlement`() = runTest {
        every { authenticationService.requireUserId() } returns payerId
        val record = SettlementRecord(
            id = settlementId,
            groupId = groupId,
            settlement = baseSettlement,
            status = SettlementStatus.SUGGESTED,
            createdAt = LocalDateTime.now()
        )
        coEvery { settlementRepository.getSettlementById(settlementId) } returns record
        coEvery { settlementRepository.updateSettlement(any()) } returns Unit

        val result = useCase(groupId, settlementId, "Wrong amount")

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(SettlementStatus.DISPUTED, updated.status)
        assertEquals(payerId, updated.disputedBy)
        assertEquals("Wrong amount", updated.disputeReason)
        coVerify(exactly = 1) { settlementRepository.updateSettlement(any()) }
    }

    @Test
    fun `payee disputes CONFIRMED_BY_PAYER settlement`() = runTest {
        every { authenticationService.requireUserId() } returns payeeId
        val record = SettlementRecord(
            id = settlementId,
            groupId = groupId,
            settlement = baseSettlement,
            status = SettlementStatus.CONFIRMED_BY_PAYER,
            createdAt = LocalDateTime.now(),
            confirmedByPayerAt = LocalDateTime.now()
        )
        coEvery { settlementRepository.getSettlementById(settlementId) } returns record
        coEvery { settlementRepository.updateSettlement(any()) } returns Unit

        val result = useCase(groupId, settlementId, "Not received")

        assertTrue(result.isSuccess)
        assertEquals(SettlementStatus.DISPUTED, result.getOrThrow().status)
    }

    @Test
    fun `third party cannot dispute`() = runTest {
        every { authenticationService.requireUserId() } returns "user-third-party"
        val record = SettlementRecord(
            id = settlementId,
            groupId = groupId,
            settlement = baseSettlement,
            status = SettlementStatus.SUGGESTED,
            createdAt = LocalDateTime.now()
        )
        coEvery { settlementRepository.getSettlementById(settlementId) } returns record

        val result = useCase(groupId, settlementId, "Interfering")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `RESOLVED settlement cannot be disputed`() = runTest {
        every { authenticationService.requireUserId() } returns payerId
        val record = SettlementRecord(
            id = settlementId,
            groupId = groupId,
            settlement = baseSettlement,
            status = SettlementStatus.RESOLVED,
            createdAt = LocalDateTime.now(),
            resolvedAt = LocalDateTime.now()
        )
        coEvery { settlementRepository.getSettlementById(settlementId) } returns record

        val result = useCase(groupId, settlementId, "Too late")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `already DISPUTED settlement cannot be disputed again`() = runTest {
        every { authenticationService.requireUserId() } returns payerId
        val record = SettlementRecord(
            id = settlementId,
            groupId = groupId,
            settlement = baseSettlement,
            status = SettlementStatus.DISPUTED,
            createdAt = LocalDateTime.now(),
            disputedBy = payeeId,
            disputeReason = "Already disputed"
        )
        coEvery { settlementRepository.getSettlementById(settlementId) } returns record

        val result = useCase(groupId, settlementId, "Again")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `disputing ephemeral settlement materializes record and persists directly with DISPUTED`() = runTest {
        every { authenticationService.requireUserId() } returns payerId
        coEvery { settlementRepository.getGroupSettlements(groupId) } returns emptyList()
        coEvery { settlementRepository.addSettlement(any()) } returns Unit

        val ephemeralId = "ephemeral|$payerId|$payeeId|POCKET|EUR|5000"
        val result = useCase(groupId, ephemeralId, "Incorrect amount")

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(SettlementStatus.DISPUTED, updated.status)
        assertEquals(payerId, updated.disputedBy)
        assertEquals("Incorrect amount", updated.disputeReason)
        assertEquals(5000L, updated.settlement.amount)
        coVerify(exactly = 1) {
            settlementRepository.addSettlement(
                match { it.status == SettlementStatus.DISPUTED && it.disputeReason == "Incorrect amount" }
            )
        }
    }
}
