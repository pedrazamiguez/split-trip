package es.pedrazamiguez.splittrip.data.firebase.repository

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAppConfigRepositoryTest {

    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var repository: FirebaseAppConfigRepository

    @BeforeEach
    fun setUp() {
        firebaseRemoteConfig = mockk(relaxed = true)
        every { firebaseRemoteConfig.setDefaultsAsync(any<Int>()) } returns mockk(relaxed = true)

        every { firebaseRemoteConfig.getString("default_currency_code") } returns "USD"
        every { firebaseRemoteConfig.getLong("balance_computation_debounce_ms") } returns 500L
        every { firebaseRemoteConfig.getLong("max_members_per_group") } returns 15L
        every { firebaseRemoteConfig.getString("subscription_gating_enabled") } returns "true"
        every { firebaseRemoteConfig.getBoolean("subscription_gating_enabled") } returns true
        every { firebaseRemoteConfig.getLong("max_owned_groups_free") } returns 2L
        every { firebaseRemoteConfig.getLong("max_owned_groups_pro") } returns 50L
        every { firebaseRemoteConfig.getLong("max_members_per_group_free") } returns 5L
        every { firebaseRemoteConfig.getLong("max_members_per_group_pro") } returns 30L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_free") } returns 5L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_pro") } returns 150L
        every { firebaseRemoteConfig.getLong("extracted_date_max_future_days") } returns 45L
        every { firebaseRemoteConfig.getString("support_email_address") } returns "test-support@splittrip.com"
        every { firebaseRemoteConfig.getLong("settlement_nudge_rate_limit_hours") } returns 48L
        every { firebaseRemoteConfig.getString("ocr_safety_false_positives_blacklist") } returns "blade,secret"
        every { firebaseRemoteConfig.getString("developer_info_json") } returns ""
        every { firebaseRemoteConfig.getString("ads_enabled") } returns "true"
        every { firebaseRemoteConfig.getBoolean("ads_enabled") } returns true
        every { firebaseRemoteConfig.getString("admob_test_mode_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("admob_test_mode_enabled") } returns false
        every { firebaseRemoteConfig.getString("admob_banner_ad_unit_id") } returns "banner-123"
        every { firebaseRemoteConfig.getString("admob_interstitial_ad_unit_id") } returns "interstitial-456"
        every { firebaseRemoteConfig.getLong("ad_interstitial_action_frequency") } returns 3L
        every { firebaseRemoteConfig.getLong("ad_interstitial_min_interval_seconds") } returns 180L

        repository = FirebaseAppConfigRepository(firebaseRemoteConfig)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `init sets default config XML`() {
        verify(exactly = 1) { firebaseRemoteConfig.setDefaultsAsync(any<Int>()) }
    }

    @Test
    fun `init sets default values from RemoteConfig`() {
        assertEquals("USD", repository.defaultCurrencyCode.value)
        assertEquals(500L, repository.balanceComputationDebounceMs.value)
        assertEquals(15, repository.maxMembersPerGroup.value)
        assertEquals(true, repository.subscriptionGatingEnabled.value)
        assertEquals(2, repository.maxOwnedGroupsFree.value)
        assertEquals(50, repository.maxOwnedGroupsPro.value)
        assertEquals(5, repository.maxMembersPerGroupFree.value)
        assertEquals(30, repository.maxMembersPerGroupPro.value)
        assertEquals(5, repository.aiReceiptMonthlyLimitFree.value)
        assertEquals(150, repository.aiReceiptMonthlyLimitPro.value)
        assertEquals(45, repository.extractedDateMaxFutureDays.value)
        assertEquals("test-support@splittrip.com", repository.supportEmailAddress.value)
        assertEquals(48L, repository.settlementNudgeRateLimitHours.value)
        assertEquals(listOf("blade", "secret"), repository.ocrSafetyFalsePositivesBlacklist.value)
        assertEquals(FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO, repository.developerInfo.value)
        assertEquals(true, repository.adsEnabled.value)
        assertEquals(false, repository.admobTestModeEnabled.value)
        assertEquals("banner-123", repository.admobBannerAdUnitId.value)
        assertEquals("interstitial-456", repository.admobInterstitialAdUnitId.value)
        assertEquals(3, repository.adInterstitialActionFrequency.value)
        assertEquals(180L, repository.adInterstitialMinIntervalSeconds.value)
    }

    @Test
    fun `fetchConfiguration delegates to FirebaseRemoteConfig and updates flows`() = runTest {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        val mockTaskA = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.fetchAndActivate() } returns mockTaskA
        coEvery { mockTaskA.await() } returns true

        setupMockRemoteConfigValues()

        val result = repository.fetchConfiguration()

        assertTrue(result)
        verify(exactly = 1) { firebaseRemoteConfig.fetchAndActivate() }
        assertEquals("GBP", repository.defaultCurrencyCode.value)
        assertEquals(100L, repository.balanceComputationDebounceMs.value)
        assertEquals(25, repository.maxMembersPerGroup.value)
        assertEquals(false, repository.subscriptionGatingEnabled.value)
        assertEquals(3, repository.maxOwnedGroupsFree.value)
        assertEquals(200, repository.maxOwnedGroupsPro.value)
        assertEquals(8, repository.maxMembersPerGroupFree.value)
        assertEquals(50, repository.maxMembersPerGroupPro.value)
        assertEquals(10, repository.aiReceiptMonthlyLimitFree.value)
        assertEquals(250, repository.aiReceiptMonthlyLimitPro.value)
        assertEquals(60, repository.extractedDateMaxFutureDays.value)
        assertEquals("fetch-support@splittrip.com", repository.supportEmailAddress.value)
        assertEquals(12L, repository.settlementNudgeRateLimitHours.value)
        assertEquals(listOf("fuck", "dick", "pussy", "cunt"), repository.ocrSafetyFalsePositivesBlacklist.value)
        assertEquals(false, repository.adsEnabled.value)
        assertEquals(false, repository.admobTestModeEnabled.value)
        assertEquals("banner-new", repository.admobBannerAdUnitId.value)
        assertEquals("interstitial-new", repository.admobInterstitialAdUnitId.value)
        assertEquals(3, repository.adInterstitialActionFrequency.value)
        assertEquals(60L, repository.adInterstitialMinIntervalSeconds.value)
        assertEquals("Custom Dev", repository.developerInfo.value.name)
        assertEquals("https://example.com/custom.png", repository.developerInfo.value.avatarUrl)
        assertEquals("Custom Lead", repository.developerInfo.value.roleMap["en"])
    }

    @Test
    fun `fetchConfiguration handles failure safely`() = runTest {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        val mockTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.fetchAndActivate() } returns mockTask
        coEvery { mockTask.await() } throws RuntimeException("Network Error")

        val result = repository.fetchConfiguration()

        assertFalse(result)
    }

    @Test
    fun `init registers config update listener and activates changes on update`() {
        val mockConfigUpdate = mockk<ConfigUpdate>(relaxed = true)
        val updateListenerSlot = slot<ConfigUpdateListener>()

        verify(exactly = 1) { firebaseRemoteConfig.addOnConfigUpdateListener(capture(updateListenerSlot)) }

        val mockActivateTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.activate() } returns mockActivateTask

        val listenerSlot = slot<OnCompleteListener<Boolean>>()
        every { mockActivateTask.addOnCompleteListener(capture(listenerSlot)) } returns mockActivateTask
        every { mockActivateTask.isSuccessful } returns true

        every { firebaseRemoteConfig.getString("subscription_gating_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("subscription_gating_enabled") } returns false
        every { firebaseRemoteConfig.getLong("max_owned_groups_free") } returns 6L

        updateListenerSlot.captured.onUpdate(mockConfigUpdate)

        verify(exactly = 1) { firebaseRemoteConfig.activate() }

        listenerSlot.captured.onComplete(mockActivateTask)

        assertEquals(false, repository.subscriptionGatingEnabled.value)
        assertEquals(6, repository.maxOwnedGroupsFree.value)
    }

    @Test
    fun `config update listener handles error without crashing`() {
        val updateListenerSlot = slot<ConfigUpdateListener>()
        verify(exactly = 1) { firebaseRemoteConfig.addOnConfigUpdateListener(capture(updateListenerSlot)) }

        val error = mockk<FirebaseRemoteConfigException>(relaxed = true)
        updateListenerSlot.captured.onError(error)
    }

    @Test
    fun `updateFlowsFromConfig falls back to defaults when remote config values are invalid or blank`() {
        every { firebaseRemoteConfig.getString("subscription_gating_enabled") } returns ""
        every { firebaseRemoteConfig.getLong("max_owned_groups_free") } returns 0L
        every { firebaseRemoteConfig.getLong("max_owned_groups_pro") } returns -5L
        every { firebaseRemoteConfig.getLong("max_members_per_group_free") } returns 0L
        every { firebaseRemoteConfig.getLong("max_members_per_group_pro") } returns -1L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_free") } returns -1L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_pro") } returns 0L
        every { firebaseRemoteConfig.getString("support_email_address") } returns ""
        every { firebaseRemoteConfig.getLong("ad_interstitial_action_frequency") } returns 0L
        every { firebaseRemoteConfig.getLong("ad_interstitial_min_interval_seconds") } returns 0L

        val fallbackRepo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertEquals(true, fallbackRepo.subscriptionGatingEnabled.value)
        assertEquals(1, fallbackRepo.maxOwnedGroupsFree.value)
        assertEquals(100, fallbackRepo.maxOwnedGroupsPro.value)
        assertEquals(4, fallbackRepo.maxMembersPerGroupFree.value)
        assertEquals(20, fallbackRepo.maxMembersPerGroupPro.value)
        assertEquals(0, fallbackRepo.aiReceiptMonthlyLimitFree.value)
        assertEquals(100, fallbackRepo.aiReceiptMonthlyLimitPro.value)
        assertEquals("support@splittrip.eu", fallbackRepo.supportEmailAddress.value)
        assertEquals(2, fallbackRepo.adInterstitialActionFrequency.value)
        assertEquals(90L, fallbackRepo.adInterstitialMinIntervalSeconds.value)
    }

    @Test
    fun `admobTestModeEnabled when true overrides ad unit IDs with sample IDs`() {
        every { firebaseRemoteConfig.getString("admob_test_mode_enabled") } returns "true"
        every { firebaseRemoteConfig.getBoolean("admob_test_mode_enabled") } returns true
        every { firebaseRemoteConfig.getString("admob_banner_ad_unit_id") } returns "production-banner"
        every { firebaseRemoteConfig.getString("admob_interstitial_ad_unit_id") } returns "production-interstitial"

        val repo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertTrue(repo.admobTestModeEnabled.value)
        assertEquals("ca-app-pub-3940256099942544/6300978111", repo.admobBannerAdUnitId.value)
        assertEquals("ca-app-pub-3940256099942544/1033173712", repo.admobInterstitialAdUnitId.value)
    }

    @Test
    fun `admobTestModeEnabled falls back to default false when empty in RemoteConfig`() {
        every { firebaseRemoteConfig.getString("admob_test_mode_enabled") } returns ""
        every { firebaseRemoteConfig.getString("admob_banner_ad_unit_id") } returns ""
        every { firebaseRemoteConfig.getString("admob_interstitial_ad_unit_id") } returns ""

        val repo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertFalse(repo.admobTestModeEnabled.value)
        assertEquals("ca-app-pub-9638507020441461/2775424807", repo.admobBannerAdUnitId.value)
        assertEquals("ca-app-pub-9638507020441461/6643262487", repo.admobInterstitialAdUnitId.value)
    }

    private fun setupMockRemoteConfigValues() {
        every { firebaseRemoteConfig.getString("default_currency_code") } returns "GBP"
        every { firebaseRemoteConfig.getLong("balance_computation_debounce_ms") } returns 100L
        every { firebaseRemoteConfig.getLong("max_members_per_group") } returns 25L
        every { firebaseRemoteConfig.getString("subscription_gating_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("subscription_gating_enabled") } returns false
        every { firebaseRemoteConfig.getLong("max_owned_groups_free") } returns 3L
        every { firebaseRemoteConfig.getLong("max_owned_groups_pro") } returns 200L
        every { firebaseRemoteConfig.getLong("max_members_per_group_free") } returns 8L
        every { firebaseRemoteConfig.getLong("max_members_per_group_pro") } returns 50L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_free") } returns 10L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_pro") } returns 250L
        every { firebaseRemoteConfig.getLong("extracted_date_max_future_days") } returns 60L
        every { firebaseRemoteConfig.getString("support_email_address") } returns "fetch-support@splittrip.com"
        every { firebaseRemoteConfig.getLong("settlement_nudge_rate_limit_hours") } returns 12L
        every { firebaseRemoteConfig.getString("ocr_safety_false_positives_blacklist") } returns "fuck,dick,pussy,cunt"
        every { firebaseRemoteConfig.getString("ads_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("ads_enabled") } returns false
        every { firebaseRemoteConfig.getString("admob_test_mode_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("admob_test_mode_enabled") } returns false
        every { firebaseRemoteConfig.getString("admob_banner_ad_unit_id") } returns "banner-new"
        every { firebaseRemoteConfig.getString("admob_interstitial_ad_unit_id") } returns "interstitial-new"
        every { firebaseRemoteConfig.getLong("ad_interstitial_action_frequency") } returns 3L
        every { firebaseRemoteConfig.getLong("ad_interstitial_min_interval_seconds") } returns 60L
        every {
            firebaseRemoteConfig.getString("developer_info_json")
        } returns """
            {
              "name": "Custom Dev",
              "avatar_url": "https://example.com/custom.png",
              "github_url": "https://github.com/custom",
              "splittrip_repo_url": "https://github.com/custom/split-trip",
              "linkedin_url": "https://linkedin.com/in/custom",
              "portfolio_url": "https://custom.me",
              "role_map": { "en": "Custom Lead" },
              "bio_map": { "en": "Custom Bio" },
              "credits_map": { "en": "Custom Credits" },
              "copyright_map": { "en": "© 2026 Custom" }
            }
        """.trimIndent()
    }
}
