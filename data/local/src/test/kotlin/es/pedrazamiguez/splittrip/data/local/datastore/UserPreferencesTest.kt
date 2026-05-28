package es.pedrazamiguez.splittrip.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesTest {

    private lateinit var context: Context
    private lateinit var authenticationService: AuthenticationService

    private companion object {
        private const val USER_A_ID = "user-a-123"
        private const val USER_B_ID = "user-b-456"
        private const val TEST_GROUP_ID = "group-001"
    }

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        authenticationService = mockk()
        context.dataStore.edit { it.clear() }
    }

    @After
    fun tearDown() = runTest {
        // Clear the entire DataStore after each test to prevent cross-test pollution
        context.dataStore.edit { it.clear() }
    }

    private fun createUserPreferences(userId: String? = USER_A_ID): UserPreferences {
        every { authenticationService.currentUserId() } returns userId
        every { authenticationService.authState } returns flowOf(userId != null)
        return UserPreferences(context, authenticationService)
    }

    // ── User Isolation Tests ─────────────────────────────────────────────

    @Test
    fun `selectedGroupId is isolated per user`() = runTest {
        // Given — User A sets a selected group
        val prefsA = createUserPreferences(USER_A_ID)
        prefsA.setSelectedGroup("groupA", "Group A Name", "EUR")

        // When — User B reads selectedGroupId
        val prefsB = createUserPreferences(USER_B_ID)
        val result = prefsB.selectedGroupId.first()

        // Then — User B sees null (no cross-user leakage)
        assertNull(result)
    }

    @Test
    fun `selectedGroupName is isolated per user`() = runTest {
        // Given — User A sets a selected group
        val prefsA = createUserPreferences(USER_A_ID)
        prefsA.setSelectedGroup("groupA", "Group A Name", "EUR")

        // When — User B reads selectedGroupName
        val prefsB = createUserPreferences(USER_B_ID)
        val result = prefsB.selectedGroupName.first()

        // Then — User B sees null
        assertNull(result)
    }

    @Test
    fun `selectedGroupCurrency is isolated per user`() = runTest {
        // Given — User A sets a selected group with currency
        val prefsA = createUserPreferences(USER_A_ID)
        prefsA.setSelectedGroup("groupA", "Group A Name", "EUR")

        // When — User B reads selectedGroupCurrency
        val prefsB = createUserPreferences(USER_B_ID)
        val result = prefsB.selectedGroupCurrency.first()

        // Then — User B sees null
        assertNull(result)
    }

    @Test
    fun `defaultCurrency is isolated per user`() = runTest {
        // Given — User A sets default currency to USD
        val prefsA = createUserPreferences(USER_A_ID)
        prefsA.setDefaultCurrency("USD")

        // When — User B reads default currency
        val prefsB = createUserPreferences(USER_B_ID)
        val result = prefsB.defaultCurrency.first()

        // Then — User B gets the fallback, NOT User A's currency
        assertEquals(AppConstants.DEFAULT_CURRENCY_CODE, result)
    }

    @Test
    fun `same user reads back their own selectedGroup`() = runTest {
        // Given — User A sets a selected group
        val prefs = createUserPreferences(USER_A_ID)
        prefs.setSelectedGroup("groupA", "Group A Name", "USD")

        // When — Same user reads it back
        val groupId = prefs.selectedGroupId.first()
        val groupName = prefs.selectedGroupName.first()
        val groupCurrency = prefs.selectedGroupCurrency.first()

        // Then
        assertEquals("groupA", groupId)
        assertEquals("Group A Name", groupName)
        assertEquals("USD", groupCurrency)
    }

    @Test
    fun `same user reads back their own defaultCurrency`() = runTest {
        // Given
        val prefs = createUserPreferences(USER_A_ID)
        prefs.setDefaultCurrency("JPY")

        // When
        val result = prefs.defaultCurrency.first()

        // Then
        assertEquals("JPY", result)
    }

    // ── Per-Group Key Isolation ──────────────────────────────────────────

    @Test
    fun `groupLastUsedCurrency is isolated per user`() = runTest {
        // Given — User A sets last used currency for a group
        val prefsA = createUserPreferences(USER_A_ID)
        prefsA.setGroupLastUsedCurrency(TEST_GROUP_ID, "USD")

        // When — User B reads the same group's last used currency
        val prefsB = createUserPreferences(USER_B_ID)
        val result = prefsB.getGroupLastUsedCurrency(TEST_GROUP_ID).first()

        // Then — User B sees null
        assertNull(result)
    }

    @Test
    fun `groupLastUsedPaymentMethod is isolated per user`() = runTest {
        // Given — User A sets last used payment method for a group
        val prefsA = createUserPreferences(USER_A_ID)
        prefsA.setGroupLastUsedPaymentMethod(TEST_GROUP_ID, "CREDIT_CARD")

        // When — User B reads the same group's last used payment method
        val prefsB = createUserPreferences(USER_B_ID)
        val result = prefsB.getGroupLastUsedPaymentMethod(TEST_GROUP_ID).first()

        // Then — User B sees empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun `groupLastUsedCategory is isolated per user`() = runTest {
        // Given — User A sets last used category for a group
        val prefsA = createUserPreferences(USER_A_ID)
        prefsA.setGroupLastUsedCategory(TEST_GROUP_ID, "food")

        // When — User B reads the same group's last used category
        val prefsB = createUserPreferences(USER_B_ID)
        val result = prefsB.getGroupLastUsedCategory(TEST_GROUP_ID).first()

        // Then — User B sees empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun `lastSeenBalance is isolated per user`() = runTest {
        // Given — User A sets last seen balance for a group
        val prefsA = createUserPreferences(USER_A_ID)
        prefsA.setLastSeenBalance(TEST_GROUP_ID, "€150.00")

        // When — User B reads the same group's last seen balance
        val prefsB = createUserPreferences(USER_B_ID)
        val result = prefsB.getLastSeenBalance(TEST_GROUP_ID).first()

        // Then — User B sees null
        assertNull(result)
    }

    // ── Onboarding (Device-scoped — SHARED across users) ─────────────────

    @Test
    fun `onboardingComplete is shared across users (device-scoped)`() = runTest {
        // Given — User A completes onboarding
        val prefsA = createUserPreferences(USER_A_ID)
        prefsA.setOnboardingComplete()

        // When — User B reads onboarding status
        val prefsB = createUserPreferences(USER_B_ID)
        val result = prefsB.isOnboardingComplete.first()

        // Then — User B also sees onboarding as complete (device-level concern)
        assertTrue(result)
    }

    @Test
    fun `onboardingComplete defaults to false`() = runTest {
        val prefs = createUserPreferences(USER_A_ID)
        val result = prefs.isOnboardingComplete.first()
        assertFalse(result)
    }

    // ── clearAll() Selective Removal ─────────────────────────────────────

    @Test
    fun `clearAll only removes current user keys`() = runTest {
        // Use a single instance — the userId is resolved dynamically via the mock
        every { authenticationService.authState } returns flowOf(true)
        val prefs = UserPreferences(context, authenticationService)

        // Given — User A writes preferences
        every { authenticationService.currentUserId() } returns USER_A_ID
        prefs.setSelectedGroup("groupA", "Group A", "EUR")
        prefs.setDefaultCurrency("USD")
        prefs.setGroupLastUsedCurrency(TEST_GROUP_ID, "USD")

        // Given — User B writes preferences
        every { authenticationService.currentUserId() } returns USER_B_ID
        prefs.setSelectedGroup("groupB", "Group B", "GBP")
        prefs.setDefaultCurrency("GBP")

        // When — User A clears their preferences
        every { authenticationService.currentUserId() } returns USER_A_ID
        prefs.clearAll()

        // Then — User A's data is gone
        assertNull(prefs.selectedGroupId.first())
        assertNull(prefs.selectedGroupName.first())
        assertNull(prefs.selectedGroupCurrency.first())
        assertEquals(AppConstants.DEFAULT_CURRENCY_CODE, prefs.defaultCurrency.first())

        // Then — User B's data is still intact
        every { authenticationService.currentUserId() } returns USER_B_ID
        assertEquals("groupB", prefs.selectedGroupId.first())
        assertEquals("Group B", prefs.selectedGroupName.first())
        assertEquals("GBP", prefs.selectedGroupCurrency.first())
        assertEquals("GBP", prefs.defaultCurrency.first())
    }

    @Test
    fun `clearAll preserves device-scoped onboarding key`() = runTest {
        // Given — Onboarding completed and user has preferences
        val prefs = createUserPreferences(USER_A_ID)
        prefs.setOnboardingComplete()
        prefs.setSelectedGroup("groupA", "Group A", "EUR")

        // When — User clears their preferences
        prefs.clearAll()

        // Then — Onboarding stays complete (device-scoped)
        assertTrue(prefs.isOnboardingComplete.first())
        // But user-scoped data is gone
        assertNull(prefs.selectedGroupId.first())
    }

    // ── Anonymous Fallback ───────────────────────────────────────────────

    @Test
    fun `uses anonymous prefix when userId is null`() = runTest {
        // Given — No user authenticated
        val prefs = createUserPreferences(userId = null)
        prefs.setDefaultCurrency("CHF")

        // When — Reading as anonymous
        val result = prefs.defaultCurrency.first()

        // Then — Value is stored under anonymous prefix and readable
        assertEquals("CHF", result)
    }

    @Test
    fun `anonymous preferences are isolated from authenticated user`() = runTest {
        // Given — Anonymous user sets a preference
        val prefsAnon = createUserPreferences(userId = null)
        prefsAnon.setDefaultCurrency("CHF")

        // When — Authenticated user reads the same preference
        val prefsAuth = createUserPreferences(USER_A_ID)
        val result = prefsAuth.defaultCurrency.first()

        // Then — Authenticated user gets the default, not the anonymous value
        assertEquals(AppConstants.DEFAULT_CURRENCY_CODE, result)
    }

    // ── setSelectedGroup null handling ────────────────────────────────────

    @Test
    fun `setSelectedGroup with null values removes the keys`() = runTest {
        // Given — User has a selected group
        val prefs = createUserPreferences(USER_A_ID)
        prefs.setSelectedGroup("groupA", "Group A", "EUR")

        // When — Setting to null
        prefs.setSelectedGroup(null, null, null)

        // Then — Keys are removed
        assertNull(prefs.selectedGroupId.first())
        assertNull(prefs.selectedGroupName.first())
        assertNull(prefs.selectedGroupCurrency.first())
    }

    @Test
    fun `setSelectedGroup with null currency removes only currency key`() = runTest {
        // Given — User has a selected group with currency
        val prefs = createUserPreferences(USER_A_ID)
        prefs.setSelectedGroup("groupA", "Group A", "EUR")

        // When — Setting group with null currency
        prefs.setSelectedGroup("groupA", "Group A", null)

        // Then — Group ID and name preserved, currency removed
        assertEquals("groupA", prefs.selectedGroupId.first())
        assertEquals("Group A", prefs.selectedGroupName.first())
        assertNull(prefs.selectedGroupCurrency.first())
    }

    // ── MRU List Behavior ────────────────────────────────────────────────

    @Test
    fun `MRU list limits to 3 items`() = runTest {
        val prefs = createUserPreferences(USER_A_ID)

        // Add 4 payment methods
        prefs.setGroupLastUsedPaymentMethod(TEST_GROUP_ID, "CASH")
        prefs.setGroupLastUsedPaymentMethod(TEST_GROUP_ID, "CREDIT_CARD")
        prefs.setGroupLastUsedPaymentMethod(TEST_GROUP_ID, "DEBIT_CARD")
        prefs.setGroupLastUsedPaymentMethod(TEST_GROUP_ID, "BANK_TRANSFER")

        val result = prefs.getGroupLastUsedPaymentMethod(TEST_GROUP_ID).first()

        // Only the 3 most recent
        assertEquals(3, result.size)
        assertEquals("BANK_TRANSFER", result[0])
        assertEquals("DEBIT_CARD", result[1])
        assertEquals("CREDIT_CARD", result[2])
    }

    @Test
    fun `MRU list deduplicates entries`() = runTest {
        val prefs = createUserPreferences(USER_A_ID)

        prefs.setGroupLastUsedCategory(TEST_GROUP_ID, "food")
        prefs.setGroupLastUsedCategory(TEST_GROUP_ID, "transport")
        prefs.setGroupLastUsedCategory(TEST_GROUP_ID, "food") // re-add food

        val result = prefs.getGroupLastUsedCategory(TEST_GROUP_ID).first()

        assertEquals(2, result.size)
        assertEquals("food", result[0]) // most recent
        assertEquals("transport", result[1])
    }

    // ── Active AI Model Settings ─────────────────────────────────────────

    @Test
    fun `activeAiModel is isolated per user`() = runTest {
        val prefsA = createUserPreferences(USER_A_ID)
        prefsA.setActiveAiModel("LITE_RT_LM")

        val prefsB = createUserPreferences(USER_B_ID)
        val result = prefsB.activeAiModel.first()

        assertNull(result)
    }

    @Test
    fun `same user reads back their own activeAiModel`() = runTest {
        val prefs = createUserPreferences(USER_A_ID)
        prefs.setActiveAiModel("AI_CORE_GEMMA_4")

        val result = prefs.activeAiModel.first()

        assertEquals("AI_CORE_GEMMA_4", result)
    }
}
