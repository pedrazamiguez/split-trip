package es.pedrazamiguez.splittrip.features.main.presentation.viewmodel

import android.os.Bundle
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.usecase.currency.WarmCurrencyCacheUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("MainViewModel")
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var registerDeviceTokenUseCase: RegisterDeviceTokenUseCase
    private lateinit var getGroupByIdUseCase: GetGroupByIdUseCase
    private lateinit var warmCurrencyCacheUseCase: WarmCurrencyCacheUseCase
    private lateinit var observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        registerDeviceTokenUseCase = mockk(relaxed = true)
        getGroupByIdUseCase = mockk(relaxed = true)
        warmCurrencyCacheUseCase = mockk(relaxed = true)
        observeCurrentUserProfileUseCase = mockk(relaxed = true)
        coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)
        every { observeCurrentUserProfileUseCase() } returns flowOf(null)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MainViewModel = MainViewModel(
        registerDeviceTokenUseCase = registerDeviceTokenUseCase,
        getGroupByIdUseCase = getGroupByIdUseCase,
        warmCurrencyCacheUseCase = warmCurrencyCacheUseCase,
        observeCurrentUserProfileUseCase = observeCurrentUserProfileUseCase
    )

    @Nested
    @DisplayName("init - device token registration")
    inner class Init {

        @Test
        fun `registers device token on creation`() = runTest(testDispatcher) {
            // When
            createViewModel()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { registerDeviceTokenUseCase() }
        }

        @Test
        fun `handles device token registration failure gracefully`() = runTest(testDispatcher) {
            // Given
            coEvery { registerDeviceTokenUseCase() } returns Result.failure(
                RuntimeException("FCM unavailable")
            )

            // When - should not throw
            createViewModel()
            advanceUntilIdle()

            // Then - still called, failure is silently caught
            coVerify(exactly = 1) { registerDeviceTokenUseCase() }
        }
    }

    @Nested
    @DisplayName("init - currency cache warm-up")
    inner class CurrencyCacheWarmUp {

        @Test
        fun `warms currency cache on creation`() = runTest(testDispatcher) {
            // When
            createViewModel()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { warmCurrencyCacheUseCase() }
        }
    }

    @Nested
    @DisplayName("bundle management - getBundle / setBundle")
    inner class BundleManagement {

        @Test
        fun `getBundle returns null for unknown route`() = runTest(testDispatcher) {
            // Given
            val viewModel = createViewModel()

            // When / Then
            assertNull(viewModel.getBundle("unknown_route"))
        }

        @Test
        fun `setBundle stores and getBundle retrieves a bundle`() = runTest(testDispatcher) {
            // Given
            val viewModel = createViewModel()
            val bundle = mockk<Bundle>()

            // When
            viewModel.setBundle("groups", bundle)

            // Then
            assertEquals(bundle, viewModel.getBundle("groups"))
        }

        @Test
        fun `setBundle with null removes the stored bundle`() = runTest(testDispatcher) {
            // Given
            val viewModel = createViewModel()
            val bundle = mockk<Bundle>()
            viewModel.setBundle("groups", bundle)

            // When
            viewModel.setBundle("groups", null)

            // Then
            assertNull(viewModel.getBundle("groups"))
        }

        @Test
        fun `setBundle overwrites previously stored bundle`() = runTest(testDispatcher) {
            // Given
            val viewModel = createViewModel()
            val bundle1 = mockk<Bundle>()
            val bundle2 = mockk<Bundle>()
            viewModel.setBundle("groups", bundle1)

            // When
            viewModel.setBundle("groups", bundle2)

            // Then
            assertEquals(bundle2, viewModel.getBundle("groups"))
        }

        @Test
        fun `stores bundles independently per route`() = runTest(testDispatcher) {
            // Given
            val viewModel = createViewModel()
            val groupsBundle = mockk<Bundle>()
            val expensesBundle = mockk<Bundle>()

            // When
            viewModel.setBundle("groups", groupsBundle)
            viewModel.setBundle("expenses", expensesBundle)

            // Then
            assertEquals(groupsBundle, viewModel.getBundle("groups"))
            assertEquals(expensesBundle, viewModel.getBundle("expenses"))
        }
    }

    @Nested
    @DisplayName("clearInvisibleBundles")
    inner class ClearInvisibleBundles {

        @Test
        fun `removes bundles for routes not in the visible set`() = runTest(testDispatcher) {
            // Given
            val viewModel = createViewModel()
            viewModel.setBundle("groups", mockk<Bundle>())
            viewModel.setBundle("expenses", mockk<Bundle>())
            viewModel.setBundle("balances", mockk<Bundle>())
            viewModel.setBundle("profile", mockk<Bundle>())

            // When - only groups and profile are visible (no selected group)
            viewModel.clearInvisibleBundles(setOf("groups", "profile"))

            // Then
            assertNotNull(viewModel.getBundle("groups"))
            assertNotNull(viewModel.getBundle("profile"))
            assertNull(viewModel.getBundle("expenses"))
            assertNull(viewModel.getBundle("balances"))
        }

        @Test
        fun `keeps all bundles when all routes are visible`() = runTest(testDispatcher) {
            // Given
            val viewModel = createViewModel()
            viewModel.setBundle("groups", mockk<Bundle>())
            viewModel.setBundle("expenses", mockk<Bundle>())

            // When
            viewModel.clearInvisibleBundles(setOf("groups", "expenses"))

            // Then - all bundles preserved
            assertNotNull(viewModel.getBundle("groups"))
            assertNotNull(viewModel.getBundle("expenses"))
        }

        @Test
        fun `removes all bundles when visible set is empty`() = runTest(testDispatcher) {
            // Given
            val viewModel = createViewModel()
            viewModel.setBundle("groups", mockk<Bundle>())
            viewModel.setBundle("expenses", mockk<Bundle>())

            // When
            viewModel.clearInvisibleBundles(emptySet())

            // Then
            assertNull(viewModel.getBundle("groups"))
            assertNull(viewModel.getBundle("expenses"))
        }

        @Test
        fun `does nothing when no bundles are stored`() = runTest(testDispatcher) {
            // Given
            val viewModel = createViewModel()

            // When
            viewModel.clearInvisibleBundles(setOf("groups", "profile"))

            // Then - no exception, nothing to clear
            assertNull(viewModel.getBundle("groups"))
        }
    }

    @Nested
    @DisplayName("clearAllBundles")
    inner class ClearAllBundles {

        @Test
        fun `removes all stored bundles`() = runTest(testDispatcher) {
            // Given
            val viewModel = createViewModel()
            viewModel.setBundle("groups", mockk<Bundle>())
            viewModel.setBundle("expenses", mockk<Bundle>())
            viewModel.setBundle("profile", mockk<Bundle>())

            // When
            viewModel.clearAllBundles()

            // Then
            assertNull(viewModel.getBundle("groups"))
            assertNull(viewModel.getBundle("expenses"))
            assertNull(viewModel.getBundle("profile"))
        }

        @Test
        fun `does nothing when no bundles exist`() = runTest(testDispatcher) {
            // Given
            val viewModel = createViewModel()

            // When - should not throw
            viewModel.clearAllBundles()

            // Then
            assertNull(viewModel.getBundle("any_route"))
        }
    }

    @Nested
    @DisplayName("resolveGroupName")
    inner class ResolveGroupName {

        @Test
        fun `returns group name when group exists`() = runTest(testDispatcher) {
            // Given
            val groupId = "group-123"
            val group = Group(id = groupId, name = "Beach Vacation")
            coEvery { getGroupByIdUseCase(groupId) } returns group

            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            val result = viewModel.resolveGroupName(groupId)

            // Then
            assertEquals("Beach Vacation", result)
            coVerify(exactly = 1) { getGroupByIdUseCase(groupId) }
        }

        @Test
        fun `returns null when group does not exist`() = runTest(testDispatcher) {
            // Given
            val groupId = "nonexistent-group"
            coEvery { getGroupByIdUseCase(groupId) } returns null

            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            val result = viewModel.resolveGroupName(groupId)

            // Then
            assertNull(result)
        }

        @Test
        fun `returns null when use case throws exception`() = runTest(testDispatcher) {
            // Given
            val groupId = "group-error"
            coEvery { getGroupByIdUseCase(groupId) } throws RuntimeException("DB error")

            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            val result = viewModel.resolveGroupName(groupId)

            // Then
            assertNull(result)
        }

        @Test
        fun `delegates to GetGroupByIdUseCase with correct groupId`() = runTest(testDispatcher) {
            // Given
            val groupId = "specific-group-789"
            coEvery { getGroupByIdUseCase(groupId) } returns Group(id = groupId, name = "Trip")

            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.resolveGroupName(groupId)

            // Then
            coVerify(exactly = 1) { getGroupByIdUseCase(groupId) }
        }
    }

    @Nested
    @DisplayName("resolveGroupCurrency")
    inner class ResolveGroupCurrency {

        @Test
        fun `returns group currency when group exists`() = runTest(testDispatcher) {
            // Given
            val groupId = "group-123"
            val group = Group(id = groupId, name = "Beach Vacation", currency = "EUR")
            coEvery { getGroupByIdUseCase(groupId) } returns group

            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            val result = viewModel.resolveGroupCurrency(groupId)

            // Then
            assertEquals("EUR", result)
            coVerify(exactly = 1) { getGroupByIdUseCase(groupId) }
        }

        @Test
        fun `returns null when group does not exist`() = runTest(testDispatcher) {
            // Given
            val groupId = "nonexistent-group"
            coEvery { getGroupByIdUseCase(groupId) } returns null

            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            val result = viewModel.resolveGroupCurrency(groupId)

            // Then
            assertNull(result)
        }

        @Test
        fun `returns null when use case throws exception`() = runTest(testDispatcher) {
            // Given
            val groupId = "group-error"
            coEvery { getGroupByIdUseCase(groupId) } throws RuntimeException("DB error")

            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            val result = viewModel.resolveGroupCurrency(groupId)

            // Then
            assertNull(result)
        }

        @Test
        fun `delegates to GetGroupByIdUseCase with correct groupId`() = runTest(testDispatcher) {
            // Given
            val groupId = "specific-group-789"
            coEvery { getGroupByIdUseCase(groupId) } returns Group(
                id = groupId,
                name = "Trip",
                currency = "USD"
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            // When
            viewModel.resolveGroupCurrency(groupId)

            // Then
            coVerify(exactly = 1) { getGroupByIdUseCase(groupId) }
        }
    }
}
