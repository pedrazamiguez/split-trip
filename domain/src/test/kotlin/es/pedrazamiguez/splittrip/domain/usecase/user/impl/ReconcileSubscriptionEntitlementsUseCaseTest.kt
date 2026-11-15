package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.constant.BillingConstants
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.BillingService
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserTierUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReconcileSubscriptionEntitlementsUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var billingService: BillingService
    private lateinit var updateUserTierUseCase: UpdateUserTierUseCase
    private lateinit var useCase: ReconcileSubscriptionEntitlementsUseCaseImpl

    private val freeUser = User(
        userId = "user-1",
        email = "user@example.com",
        tier = SubscriptionTier.FREE
    )

    private val proUser = freeUser.copy(tier = SubscriptionTier.PRO)

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        billingService = mockk()
        updateUserTierUseCase = mockk()
        useCase = ReconcileSubscriptionEntitlementsUseCaseImpl(
            userRepository = userRepository,
            billingService = billingService,
            updateUserTierUseCase = updateUserTierUseCase
        )
    }

    @Test
    fun `returns success without querying billing when user is null (not signed in)`() = runTest {
        coEvery { userRepository.getCurrentUserProfile() } returns null

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { billingService.queryActivePurchases() }
        coVerify(exactly = 0) { updateUserTierUseCase(any(), any()) }
    }

    @Test
    fun `returns success without querying billing when user is FREE`() = runTest {
        coEvery { userRepository.getCurrentUserProfile() } returns freeUser

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { billingService.queryActivePurchases() }
        coVerify(exactly = 0) { updateUserTierUseCase(any(), any()) }
    }

    @Test
    fun `maintains PRO and returns success without updating tier when Play returns active monthly product ID`() =
        runTest {
            coEvery { userRepository.getCurrentUserProfile() } returns proUser
            coEvery {
                billingService.queryActivePurchases()
            } returns Result.success(listOf(BillingConstants.PRODUCT_ID_PRO_MONTHLY))

            val result = useCase()

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { billingService.queryActivePurchases() }
            coVerify(exactly = 0) { updateUserTierUseCase(any(), any()) }
        }

    @Test
    fun `maintains PRO and returns success without updating tier when Play returns active annual product ID`() =
        runTest {
            coEvery { userRepository.getCurrentUserProfile() } returns proUser
            coEvery {
                billingService.queryActivePurchases()
            } returns Result.success(listOf(BillingConstants.PRODUCT_ID_PRO_ANNUAL))

            val result = useCase()

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { billingService.queryActivePurchases() }
            coVerify(exactly = 0) { updateUserTierUseCase(any(), any()) }
        }

    @Test
    fun `downgrades PRO user to FREE via UpdateUserTierUseCase when Play returns OK with empty purchase list`() =
        runTest {
            coEvery { userRepository.getCurrentUserProfile() } returns proUser
            coEvery { billingService.queryActivePurchases() } returns Result.success(emptyList())
            coEvery { updateUserTierUseCase("user-1", SubscriptionTier.FREE) } returns Result.success(Unit)

            val result = useCase()

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { billingService.queryActivePurchases() }
            coVerify(exactly = 1) { updateUserTierUseCase("user-1", SubscriptionTier.FREE) }
        }

    @Test
    fun `downgrades PRO user to FREE when Play returns only unrecognized product IDs`() = runTest {
        coEvery { userRepository.getCurrentUserProfile() } returns proUser
        coEvery { billingService.queryActivePurchases() } returns Result.success(listOf("unknown_sku"))
        coEvery { updateUserTierUseCase("user-1", SubscriptionTier.FREE) } returns Result.success(Unit)

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { updateUserTierUseCase("user-1", SubscriptionTier.FREE) }
    }

    @Test
    fun `preserves PRO and returns failure when queryActivePurchases fails (offline or error)`() =
        runTest {
            val error = IllegalStateException("Billing unavailable")
            coEvery { userRepository.getCurrentUserProfile() } returns proUser
            coEvery { billingService.queryActivePurchases() } returns Result.failure(error)

            val result = useCase()

            assertTrue(result.isFailure)
            assertEquals(error, result.exceptionOrNull())
            coVerify(exactly = 0) { updateUserTierUseCase(any(), any()) }
        }

    @Test
    fun `propagates UpdateUserTierUseCase failure if downgrade persistence fails`() = runTest {
        val dbError = IllegalStateException("Room write failed")
        coEvery { userRepository.getCurrentUserProfile() } returns proUser
        coEvery { billingService.queryActivePurchases() } returns Result.success(emptyList())
        coEvery { updateUserTierUseCase("user-1", SubscriptionTier.FREE) } returns Result.failure(dbError)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(dbError, result.exceptionOrNull())
    }

    @Test
    fun `rethrows CancellationException without catching`() {
        coEvery { userRepository.getCurrentUserProfile() } throws CancellationException("Cancelled")

        assertThrows(CancellationException::class.java) {
            runBlocking {
                useCase()
            }
        }
    }
}
