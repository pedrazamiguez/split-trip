package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.data.local.datastore.UserPreferences
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.OptionalUpdateDismissal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserPreferenceRepositoryImplTest {

    private lateinit var userPreferences: UserPreferences
    private lateinit var repository: UserPreferenceRepositoryImpl

    @BeforeEach
    fun setUp() {
        userPreferences = mockk()
        repository = UserPreferenceRepositoryImpl(userPreferences)
    }

    @Test
    fun `getBiometricLockEnabled delegates to userPreferences`() = runTest {
        every { userPreferences.isBiometricLockEnabled } returns flowOf(true)

        val result = repository.getBiometricLockEnabled().first()

        assertTrue(result)
    }

    @Test
    fun `setBiometricLockEnabled delegates to userPreferences`() = runTest {
        coEvery { userPreferences.setBiometricLockEnabled(true) } just runs

        repository.setBiometricLockEnabled(true)

        coVerify(exactly = 1) { userPreferences.setBiometricLockEnabled(true) }
    }

    @Test
    fun `getUserDefaultCurrency delegates to userPreferences`() = runTest {
        every { userPreferences.defaultCurrency } returns flowOf("USD")

        val result = repository.getUserDefaultCurrency().first()

        assertEquals("USD", result)
    }

    @Test
    fun `setUserDefaultCurrency delegates to userPreferences`() = runTest {
        coEvery { userPreferences.setDefaultCurrency("EUR") } just runs

        repository.setUserDefaultCurrency("EUR")

        coVerify(exactly = 1) { userPreferences.setDefaultCurrency("EUR") }
    }

    @Test
    fun `getActiveAiEngine delegates to userPreferences`() = runTest {
        every { userPreferences.activeAiEngine } returns flowOf(AiEngineType.AI_CORE_GEMMA_4)

        val result = repository.getActiveAiEngine().first()

        assertEquals(AiEngineType.AI_CORE_GEMMA_4, result)
    }

    @Test
    fun `setActiveAiEngine delegates to userPreferences`() = runTest {
        coEvery { userPreferences.setActiveAiEngine(AiEngineType.LITE_RT_LM) } just runs

        repository.setActiveAiEngine(AiEngineType.LITE_RT_LM)

        coVerify(exactly = 1) { userPreferences.setActiveAiEngine(AiEngineType.LITE_RT_LM) }
    }

    @Test
    fun `getAppLanguage delegates to userPreferences`() = runTest {
        every { userPreferences.appLanguage } returns flowOf("es")

        val result = repository.getAppLanguage().first()

        assertEquals("es", result)
    }

    @Test
    fun `setAppLanguage delegates to userPreferences`() = runTest {
        coEvery { userPreferences.setAppLanguage("en") } just runs

        repository.setAppLanguage("en")

        coVerify(exactly = 1) { userPreferences.setAppLanguage("en") }
    }

    @Test
    fun `getShouldShowLanguagePill delegates to userPreferences`() = runTest {
        every { userPreferences.shouldShowLanguagePill } returns flowOf(true)

        val result = repository.getShouldShowLanguagePill().first()

        assertTrue(result)
    }

    @Test
    fun `setShouldShowLanguagePill delegates to userPreferences`() = runTest {
        coEvery { userPreferences.setShouldShowLanguagePill(false) } just runs

        repository.setShouldShowLanguagePill(false)

        coVerify(exactly = 1) { userPreferences.setShouldShowLanguagePill(false) }
    }

    @Test
    fun `getAppTheme delegates to userPreferences`() = runTest {
        every { userPreferences.appTheme } returns flowOf("dark")

        val result = repository.getAppTheme().first()

        assertEquals("dark", result)
    }

    @Test
    fun `setAppTheme delegates to userPreferences`() = runTest {
        coEvery { userPreferences.setAppTheme("light") } just runs

        repository.setAppTheme("light")

        coVerify(exactly = 1) { userPreferences.setAppTheme("light") }
    }

    @Test
    fun `getHasSignedOut delegates to userPreferences`() = runTest {
        every { userPreferences.hasSignedOut } returns flowOf(true)

        val result = repository.getHasSignedOut().first()

        assertTrue(result)
    }

    @Test
    fun `setHasSignedOut delegates to userPreferences`() = runTest {
        coEvery { userPreferences.setHasSignedOut(false) } just runs

        repository.setHasSignedOut(false)

        coVerify(exactly = 1) { userPreferences.setHasSignedOut(false) }
    }

    @Test
    fun `getIsReconciled delegates to userPreferences`() = runTest {
        every { userPreferences.isReconciled } returns flowOf(false)

        val result = repository.getIsReconciled().first()

        assertFalse(result)
    }

    @Test
    fun `setIsReconciled delegates to userPreferences`() = runTest {
        coEvery { userPreferences.setIsReconciled(true) } just runs

        repository.setIsReconciled(true)

        coVerify(exactly = 1) { userPreferences.setIsReconciled(true) }
    }

    @Test
    fun `getOptionalUpdateDismissal delegates to userPreferences`() = runTest {
        val expected = OptionalUpdateDismissal(versionCode = 1_001_000L, dismissedAtTimestampMs = 1_700_000_000_000L)
        coEvery { userPreferences.getOptionalUpdateDismissal() } returns expected

        val result = repository.getOptionalUpdateDismissal()

        assertEquals(expected, result)
    }

    @Test
    fun `recordOptionalUpdateDismissal delegates to userPreferences`() = runTest {
        coEvery { userPreferences.recordOptionalUpdateDismissal(1_001_000L, 1_700_000_000_000L) } just runs

        repository.recordOptionalUpdateDismissal(1_001_000L, 1_700_000_000_000L)

        coVerify(exactly = 1) {
            userPreferences.recordOptionalUpdateDismissal(1_001_000L, 1_700_000_000_000L)
        }
    }

    @Test
    fun `clearAll delegates to userPreferences`() = runTest {
        coEvery { userPreferences.clearAll() } just runs

        repository.clearAll()

        coVerify(exactly = 1) { userPreferences.clearAll() }
    }
}
