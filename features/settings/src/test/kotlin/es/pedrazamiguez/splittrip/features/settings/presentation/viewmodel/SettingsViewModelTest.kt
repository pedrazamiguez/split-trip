package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.enums.Currency
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignOutUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ConsumeLanguagePillUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppLanguageUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetShouldShowLanguagePillUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetAppLanguageUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SettingsViewModel")
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var signOutUseCase: SignOutUseCase
    private lateinit var getUserDefaultCurrencyUseCase: GetUserDefaultCurrencyUseCase
    private lateinit var getAppLanguageUseCase: GetAppLanguageUseCase
    private lateinit var setAppLanguageUseCase: SetAppLanguageUseCase
    private lateinit var getShouldShowLanguagePillUseCase: GetShouldShowLanguagePillUseCase
    private lateinit var consumeLanguagePillUseCase: ConsumeLanguagePillUseCase

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        signOutUseCase = mockk()
        getUserDefaultCurrencyUseCase = mockk()
        getAppLanguageUseCase = mockk()
        setAppLanguageUseCase = mockk()
        getShouldShowLanguagePillUseCase = mockk()
        consumeLanguagePillUseCase = mockk()

        every { getAppLanguageUseCase() } returns flowOf(null)
        every { getShouldShowLanguagePillUseCase() } returns flowOf(false)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
        signOutUseCase = signOutUseCase,
        getUserDefaultCurrencyUseCase = getUserDefaultCurrencyUseCase,
        getAppLanguageUseCase = getAppLanguageUseCase,
        setAppLanguageUseCase = setAppLanguageUseCase,
        getShouldShowLanguagePillUseCase = getShouldShowLanguagePillUseCase,
        consumeLanguagePillUseCase = consumeLanguagePillUseCase
    )

    // ── currentCurrency StateFlow ───────────────────────────────────────────

    @Nested
    @DisplayName("currentCurrency")
    inner class CurrentCurrency {

        @Test
        fun `emits Currency from use case flow`() = runTest(testDispatcher) {
            every { getUserDefaultCurrencyUseCase() } returns flowOf("USD")

            val viewModel = createViewModel()

            // Subscribe to trigger WhileSubscribed upstream
            val collectJob = launch { viewModel.currentCurrency.collect {} }
            advanceUntilIdle()

            assertEquals(Currency.USD, viewModel.currentCurrency.value)
            collectJob.cancel()
        }

        @Test
        fun `invalid code falls back to EUR`() = runTest(testDispatcher) {
            every { getUserDefaultCurrencyUseCase() } returns flowOf("INVALID_CODE")

            val viewModel = createViewModel()

            val collectJob = launch { viewModel.currentCurrency.collect {} }
            advanceUntilIdle()

            assertEquals(Currency.EUR, viewModel.currentCurrency.value)
            collectJob.cancel()
        }

        @Test
        fun `initial value is null before flow emits`() = runTest(testDispatcher) {
            every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")

            val viewModel = createViewModel()
            // Don't advance — check initial value
            assertNull(viewModel.currentCurrency.value)
        }
    }

    // ── updateNotificationPermission ────────────────────────────────────────

    @Nested
    @DisplayName("updateNotificationPermission")
    inner class UpdateNotificationPermission {

        @Test
        fun `updates hasNotificationPermission state`() = runTest(testDispatcher) {
            every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")
            val viewModel = createViewModel()

            assertFalse(viewModel.hasNotificationPermission.value)

            viewModel.updateNotificationPermission(true)

            assertTrue(viewModel.hasNotificationPermission.value)
        }
    }

    // ── signOut ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("signOut")
    inner class SignOut {

        @Test
        fun `success calls onSignedOut`() = runTest(testDispatcher) {
            every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")
            coEvery { signOutUseCase() } returns Result.success(Unit)

            val viewModel = createViewModel()

            var signedOutCalled = false
            viewModel.signOut { signedOutCalled = true }
            advanceUntilIdle()

            assertTrue(signedOutCalled)
        }

        @Test
        fun `failure does not call onSignedOut`() = runTest(testDispatcher) {
            every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")
            coEvery { signOutUseCase() } returns Result.failure(RuntimeException("Error"))

            val viewModel = createViewModel()

            var signedOutCalled = false
            viewModel.signOut { signedOutCalled = true }
            advanceUntilIdle()

            assertFalse(signedOutCalled)
        }
    }

    // ── currentLanguageCode StateFlow ───────────────────────────────────────

    @Nested
    @DisplayName("currentLanguageCode")
    inner class CurrentLanguageCode {

        @Test
        fun `emits Language from use case flow`() = runTest(testDispatcher) {
            every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")
            every { getAppLanguageUseCase() } returns flowOf("es")

            val viewModel = createViewModel()

            val collectJob = launch { viewModel.currentLanguageCode.collect {} }
            advanceUntilIdle()

            assertEquals("es", viewModel.currentLanguageCode.value)
            collectJob.cancel()
        }

        @Test
        fun `null language falls back to system locale`() = runTest(testDispatcher) {
            every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")
            every { getAppLanguageUseCase() } returns flowOf(null)

            val viewModel = createViewModel()

            val collectJob = launch { viewModel.currentLanguageCode.collect {} }
            advanceUntilIdle()

            val expected = if (Locale.getDefault().language == "es") "es" else "en"
            assertEquals(expected, viewModel.currentLanguageCode.value)
            collectJob.cancel()
        }
    }

    // ── shouldShowLanguagePill StateFlow ────────────────────────────────────

    @Nested
    @DisplayName("shouldShowLanguagePill")
    inner class ShouldShowLanguagePill {

        @Test
        fun `emits value from use case flow`() = runTest(testDispatcher) {
            every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")
            every { getShouldShowLanguagePillUseCase() } returns flowOf(true)

            val viewModel = createViewModel()

            val collectJob = launch { viewModel.shouldShowLanguagePill.collect {} }
            advanceUntilIdle()

            assertTrue(viewModel.shouldShowLanguagePill.value)
            collectJob.cancel()
        }
    }

    // ── onLanguageSelected ──────────────────────────────────────────────────

    @Nested
    @DisplayName("onLanguageSelected")
    inner class OnLanguageSelected {

        @Test
        fun `calls SetAppLanguageUseCase`() = runTest(testDispatcher) {
            every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")
            io.mockk.coJustRun { setAppLanguageUseCase("es") }

            val viewModel = createViewModel()
            viewModel.onLanguageSelected("es")
            advanceUntilIdle()

            io.mockk.coVerify(exactly = 1) { setAppLanguageUseCase("es") }
        }
    }

    // ── consumeLanguagePill ─────────────────────────────────────────────────

    @Nested
    @DisplayName("consumeLanguagePill")
    inner class ConsumeLanguagePill {

        @Test
        fun `calls ConsumeLanguagePillUseCase`() = runTest(testDispatcher) {
            every { getUserDefaultCurrencyUseCase() } returns flowOf("EUR")
            io.mockk.coJustRun { consumeLanguagePillUseCase() }

            val viewModel = createViewModel()
            viewModel.consumeLanguagePill()
            advanceUntilIdle()

            io.mockk.coVerify(exactly = 1) { consumeLanguagePillUseCase() }
        }
    }
}
