package es.pedrazamiguez.splittrip.data.firebase.repository

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.CategoryClassifierRuleConfig
import es.pedrazamiguez.splittrip.domain.model.CategoryKeywordRule
import es.pedrazamiguez.splittrip.domain.model.CategoryRuleMergeMode
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
        every { firebaseRemoteConfig.getString("billing_simulation_enabled") } returns "true"
        every { firebaseRemoteConfig.getBoolean("billing_simulation_enabled") } returns true
        every { firebaseRemoteConfig.getLong("max_owned_groups_free") } returns 2L
        every { firebaseRemoteConfig.getLong("max_owned_groups_pro") } returns 50L
        every { firebaseRemoteConfig.getLong("max_members_per_group_free") } returns 5L
        every { firebaseRemoteConfig.getLong("max_members_per_group_pro") } returns 30L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_free") } returns 5L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_pro") } returns 150L
        every { firebaseRemoteConfig.getLong("manual_receipt_monthly_limit_free") } returns 5L
        every { firebaseRemoteConfig.getLong("report_export_monthly_limit_free") } returns 1L
        every { firebaseRemoteConfig.getLong("max_subunits_per_group_free") } returns 1L
        every { firebaseRemoteConfig.getLong("max_subunits_per_group_pro") } returns -1L
        every { firebaseRemoteConfig.getLong("extracted_date_max_future_days") } returns 45L
        every { firebaseRemoteConfig.getString("support_email_address") } returns "test-support@splittrip.com"
        every { firebaseRemoteConfig.getLong("settlement_nudge_rate_limit_hours") } returns 48L
        every { firebaseRemoteConfig.getString("ocr_safety_false_positives_blacklist") } returns "blade,secret"
        every { firebaseRemoteConfig.getString("developer_info_json") } returns ""
        every { firebaseRemoteConfig.getString("developer_allowlist_email_hashes") } returns ""
        every { firebaseRemoteConfig.getString("pro_allowlist_email_hashes") } returns ""
        every { firebaseRemoteConfig.getString("ads_enabled") } returns "true"
        every { firebaseRemoteConfig.getBoolean("ads_enabled") } returns true
        every { firebaseRemoteConfig.getString("admob_test_mode_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("admob_test_mode_enabled") } returns false
        every { firebaseRemoteConfig.getString("admob_banner_ad_unit_id") } returns "banner-123"
        every { firebaseRemoteConfig.getString("admob_interstitial_ad_unit_id") } returns "interstitial-456"
        every { firebaseRemoteConfig.getLong("ad_interstitial_action_frequency") } returns 3L
        every { firebaseRemoteConfig.getLong("ad_interstitial_min_interval_seconds") } returns 180L
        every { firebaseRemoteConfig.getString("in_app_updates_enabled") } returns "true"
        every { firebaseRemoteConfig.getBoolean("in_app_updates_enabled") } returns true
        every { firebaseRemoteConfig.getLong("min_supported_version_code") } returns 950_000L
        every { firebaseRemoteConfig.getString("auto_force_patch_updates") } returns "false"
        every { firebaseRemoteConfig.getBoolean("auto_force_patch_updates") } returns false
        every { firebaseRemoteConfig.getString("optional_update_version_codes") } returns "1001000,1002000"
        every { firebaseRemoteConfig.getLong("optional_update_cooldown_hours") } returns 48L
        every { firebaseRemoteConfig.getString("category_classifier_rules_json") } returns ""

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
        assertEquals(true, repository.billingSimulationEnabled.value)
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
        assertEquals(
            "https://firebasestorage.googleapis.com/v0/b/expshapp.firebasestorage.app/o/" +
                "public%2Favatars%2Fdeveloper.webp?alt=media&token=7151f846-8346-4944-9cc7-2d4c02bf492d",
            repository.developerInfo.value.avatarUrl
        )
        assertEquals(true, repository.adsEnabled.value)
        assertEquals(false, repository.admobTestModeEnabled.value)
        assertEquals("banner-123", repository.admobBannerAdUnitId.value)
        assertEquals("interstitial-456", repository.admobInterstitialAdUnitId.value)
        assertEquals(3, repository.adInterstitialActionFrequency.value)
        assertEquals(180L, repository.adInterstitialMinIntervalSeconds.value)
        assertEquals(true, repository.inAppUpdatesEnabled.value)
        assertEquals(950_000L, repository.minSupportedVersionCode.value)
        assertEquals(false, repository.autoForcePatchUpdates.value)
        assertEquals(setOf(1_001_000L, 1_002_000L), repository.optionalUpdateVersionCodes.value)
        assertEquals(48L, repository.optionalUpdateCooldownHours.value)
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
        assertEquals(false, repository.billingSimulationEnabled.value)
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
        assertEquals(setOf("abc123", "def456"), repository.developerAllowlistEmailHashes.value)
        assertEquals(setOf("pro123", "pro456"), repository.proAllowlistEmailHashes.value)
        assertEquals("Custom Dev", repository.developerInfo.value.name)
        assertEquals("https://example.com/custom.png", repository.developerInfo.value.avatarUrl)
        assertEquals("Custom Lead", repository.developerInfo.value.roleMap["en"])
        assertEquals(false, repository.inAppUpdatesEnabled.value)
        assertEquals(1_000_000L, repository.minSupportedVersionCode.value)
        assertEquals(true, repository.autoForcePatchUpdates.value)
        assertEquals(setOf(1_005_000L, 1_006_001L), repository.optionalUpdateVersionCodes.value)
        assertEquals(72L, repository.optionalUpdateCooldownHours.value)
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
        every { firebaseRemoteConfig.getString("billing_simulation_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("billing_simulation_enabled") } returns false
        every { firebaseRemoteConfig.getLong("max_owned_groups_free") } returns 6L

        updateListenerSlot.captured.onUpdate(mockConfigUpdate)

        verify(exactly = 1) { firebaseRemoteConfig.activate() }

        listenerSlot.captured.onComplete(mockActivateTask)

        assertEquals(false, repository.subscriptionGatingEnabled.value)
        assertEquals(false, repository.billingSimulationEnabled.value)
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
        every { firebaseRemoteConfig.getString("billing_simulation_enabled") } returns ""
        every { firebaseRemoteConfig.getLong("max_owned_groups_free") } returns 0L
        every { firebaseRemoteConfig.getLong("max_owned_groups_pro") } returns 0L
        every { firebaseRemoteConfig.getLong("max_members_per_group_free") } returns 0L
        every { firebaseRemoteConfig.getLong("max_members_per_group_pro") } returns -1L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_free") } returns -1L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_pro") } returns 0L
        every { firebaseRemoteConfig.getLong("manual_receipt_monthly_limit_free") } returns -1L
        every { firebaseRemoteConfig.getLong("max_subunits_per_group_free") } returns -1L
        every { firebaseRemoteConfig.getLong("max_subunits_per_group_pro") } returns 0L
        every { firebaseRemoteConfig.getString("support_email_address") } returns ""
        every { firebaseRemoteConfig.getLong("ad_interstitial_action_frequency") } returns 0L
        every { firebaseRemoteConfig.getLong("ad_interstitial_min_interval_seconds") } returns 0L
        every { firebaseRemoteConfig.getString("in_app_updates_enabled") } returns ""
        every { firebaseRemoteConfig.getLong("min_supported_version_code") } returns -1L
        every { firebaseRemoteConfig.getString("auto_force_patch_updates") } returns ""
        every { firebaseRemoteConfig.getString("optional_update_version_codes") } returns ""
        every { firebaseRemoteConfig.getLong("optional_update_cooldown_hours") } returns 0L

        val fallbackRepo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertEquals(true, fallbackRepo.subscriptionGatingEnabled.value)
        assertEquals(false, fallbackRepo.billingSimulationEnabled.value)
        assertEquals(1, fallbackRepo.maxOwnedGroupsFree.value)
        assertEquals(-1, fallbackRepo.maxOwnedGroupsPro.value)
        assertEquals(4, fallbackRepo.maxMembersPerGroupFree.value)
        assertEquals(20, fallbackRepo.maxMembersPerGroupPro.value)
        assertEquals(3, fallbackRepo.aiReceiptMonthlyLimitFree.value)
        assertEquals(-1, fallbackRepo.aiReceiptMonthlyLimitPro.value)
        assertEquals(5, fallbackRepo.manualReceiptMonthlyLimitFree.value)
        assertEquals(1, fallbackRepo.maxSubunitsPerGroupFree.value)
        assertEquals(-1, fallbackRepo.maxSubunitsPerGroupPro.value)
        assertEquals("support@splittrip.eu", fallbackRepo.supportEmailAddress.value)
        assertEquals(2, fallbackRepo.adInterstitialActionFrequency.value)
        assertEquals(90L, fallbackRepo.adInterstitialMinIntervalSeconds.value)
        assertEquals(true, fallbackRepo.inAppUpdatesEnabled.value)
        assertEquals(0L, fallbackRepo.minSupportedVersionCode.value)
        assertEquals(true, fallbackRepo.autoForcePatchUpdates.value)
        assertEquals(emptySet<Long>(), fallbackRepo.optionalUpdateVersionCodes.value)
        assertEquals(24L, fallbackRepo.optionalUpdateCooldownHours.value)
    }

    @Test
    fun `updateFlowsFromConfig preserves explicit zero for ai_receipt_monthly_limit_free`() {
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_free") } returns 0L

        val repo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertEquals(0, repo.aiReceiptMonthlyLimitFree.value)
    }

    @Test
    fun `updateFlowsFromConfig preserves explicit zero for manual_receipt_monthly_limit_free`() {
        every { firebaseRemoteConfig.getLong("manual_receipt_monthly_limit_free") } returns 0L

        val repo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertEquals(0, repo.manualReceiptMonthlyLimitFree.value)
    }

    @Test
    fun `updateFlowsFromConfig honors 0 and positive for subunit free and normalizes negative pro to -1`() {
        every { firebaseRemoteConfig.getLong("max_subunits_per_group_free") } returns 0L
        every { firebaseRemoteConfig.getLong("max_subunits_per_group_pro") } returns -5L

        val zeroFreeRepo = FirebaseAppConfigRepository(firebaseRemoteConfig)
        assertEquals(0, zeroFreeRepo.maxSubunitsPerGroupFree.value)
        assertEquals(-1, zeroFreeRepo.maxSubunitsPerGroupPro.value)

        every { firebaseRemoteConfig.getLong("max_subunits_per_group_free") } returns 2L
        every { firebaseRemoteConfig.getLong("max_subunits_per_group_pro") } returns 5L

        val positiveRepo = FirebaseAppConfigRepository(firebaseRemoteConfig)
        assertEquals(2, positiveRepo.maxSubunitsPerGroupFree.value)
        assertEquals(5, positiveRepo.maxSubunitsPerGroupPro.value)
    }

    @Test
    fun `updateFlowsFromConfig normalizes negative max_owned_groups_pro to -1 and honors positive values`() {
        every { firebaseRemoteConfig.getLong("max_owned_groups_pro") } returns -5L

        val negativeRepo = FirebaseAppConfigRepository(firebaseRemoteConfig)
        assertEquals(-1, negativeRepo.maxOwnedGroupsPro.value)

        every { firebaseRemoteConfig.getLong("max_owned_groups_pro") } returns 200L

        val positiveRepo = FirebaseAppConfigRepository(firebaseRemoteConfig)
        assertEquals(200, positiveRepo.maxOwnedGroupsPro.value)
    }

    @Test
    fun `updateFlowsFromConfig normalizes negative ai_receipt_monthly_limit_pro to -1 and honors positive values`() {
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_pro") } returns -5L

        val negativeRepo = FirebaseAppConfigRepository(firebaseRemoteConfig)
        assertEquals(-1, negativeRepo.aiReceiptMonthlyLimitPro.value)

        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_pro") } returns 150L

        val positiveRepo = FirebaseAppConfigRepository(firebaseRemoteConfig)
        assertEquals(150, positiveRepo.aiReceiptMonthlyLimitPro.value)
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

    @Test
    fun `parseDeveloperInfo falls back to DEFAULT_DEVELOPER_INFO avatarUrl when avatar_url is omitted in JSON`() {
        every { firebaseRemoteConfig.getString("developer_info_json") } returns """{"name": "Custom Name"}"""

        val repo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertEquals("Custom Name", repo.developerInfo.value.name)
        assertEquals(
            FirebaseAppConfigRepository.DEFAULT_DEVELOPER_INFO.avatarUrl,
            repo.developerInfo.value.avatarUrl
        )
    }

    @Test
    fun `init falls back to default developerAllowlistEmailHashes when config value is blank`() {
        every { firebaseRemoteConfig.getString("developer_allowlist_email_hashes") } returns "   "

        val repo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertEquals(
            setOf("834495755dc5f3b5eaa8541a7e76fa12233135ba4911294620550994dea5ccb4"),
            repo.developerAllowlistEmailHashes.value
        )
    }

    @Test
    fun `parses comma-separated allowlist hashes trimming whitespace, lowercasing hex, and ignoring blank entries`() {
        every {
            firebaseRemoteConfig.getString("developer_allowlist_email_hashes")
        } returns "  ABC123 , , DEF456, "

        val repo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertEquals(setOf("abc123", "def456"), repo.developerAllowlistEmailHashes.value)
    }

    @Test
    fun `real-time ConfigUpdateListener updates developerAllowlistEmailHashes flow`() {
        val mockConfigUpdate = mockk<ConfigUpdate>(relaxed = true)
        val updateListenerSlot = slot<ConfigUpdateListener>()
        verify(exactly = 1) { firebaseRemoteConfig.addOnConfigUpdateListener(capture(updateListenerSlot)) }

        val mockActivateTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.activate() } returns mockActivateTask
        val listenerSlot = slot<OnCompleteListener<Boolean>>()
        every { mockActivateTask.addOnCompleteListener(capture(listenerSlot)) } returns mockActivateTask
        every { mockActivateTask.isSuccessful } returns true

        every { firebaseRemoteConfig.getString("developer_allowlist_email_hashes") } returns "hash1, HASH2"

        updateListenerSlot.captured.onUpdate(mockConfigUpdate)
        listenerSlot.captured.onComplete(mockActivateTask)

        assertEquals(setOf("hash1", "hash2"), repository.developerAllowlistEmailHashes.value)
    }

    @Test
    fun `init falls back to default proAllowlistEmailHashes when config value is blank`() {
        every { firebaseRemoteConfig.getString("pro_allowlist_email_hashes") } returns "   "

        val repo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertEquals(
            setOf("834495755dc5f3b5eaa8541a7e76fa12233135ba4911294620550994dea5ccb4"),
            repo.proAllowlistEmailHashes.value
        )
    }

    @Test
    fun `parses comma-separated pro allowlist hashes trimming whitespace and lowercasing hex`() {
        every {
            firebaseRemoteConfig.getString("pro_allowlist_email_hashes")
        } returns "  PRO123 , , PRO456, "

        val repo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertEquals(setOf("pro123", "pro456"), repo.proAllowlistEmailHashes.value)
    }

    @Test
    fun `fetchConfiguration updates proAllowlistEmailHashes flow`() = runTest {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        val mockTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.fetchAndActivate() } returns mockTask
        coEvery { mockTask.await() } returns true

        setupMockRemoteConfigValues()

        val result = repository.fetchConfiguration()

        assertTrue(result)
        assertEquals(setOf("pro123", "pro456"), repository.proAllowlistEmailHashes.value)
    }

    @Test
    fun `real-time ConfigUpdateListener updates proAllowlistEmailHashes flow`() {
        val mockConfigUpdate = mockk<ConfigUpdate>(relaxed = true)
        val updateListenerSlot = slot<ConfigUpdateListener>()
        verify(exactly = 1) { firebaseRemoteConfig.addOnConfigUpdateListener(capture(updateListenerSlot)) }

        val mockActivateTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.activate() } returns mockActivateTask
        val listenerSlot = slot<OnCompleteListener<Boolean>>()
        every { mockActivateTask.addOnCompleteListener(capture(listenerSlot)) } returns mockActivateTask
        every { mockActivateTask.isSuccessful } returns true

        every { firebaseRemoteConfig.getString("pro_allowlist_email_hashes") } returns "pro_hash1, PRO_HASH2"

        updateListenerSlot.captured.onUpdate(mockConfigUpdate)
        listenerSlot.captured.onComplete(mockActivateTask)

        assertEquals(setOf("pro_hash1", "pro_hash2"), repository.proAllowlistEmailHashes.value)
    }

    @Test
    fun `isInitialFetchSettled is false upon initialization before fetchConfiguration completes`() {
        assertFalse(repository.isInitialFetchSettled.value)
    }

    @Test
    fun `fetchConfiguration sets isInitialFetchSettled to true after updating flows on true result`() =
        runTest {
            mockkStatic("kotlinx.coroutines.tasks.TasksKt")
            val mockTask = mockk<Task<Boolean>>()
            every { firebaseRemoteConfig.fetchAndActivate() } returns mockTask
            coEvery { mockTask.await() } returns true
            every { firebaseRemoteConfig.getString("ads_enabled") } returns "false"
            every { firebaseRemoteConfig.getBoolean("ads_enabled") } returns false

            val result = repository.fetchConfiguration()

            assertTrue(result)
            assertFalse(repository.adsEnabled.value)
            assertTrue(repository.isInitialFetchSettled.value)
        }

    @Test
    fun `fetchConfiguration sets isInitialFetchSettled to true and refreshes flows on false result`() =
        runTest {
            mockkStatic("kotlinx.coroutines.tasks.TasksKt")
            val mockTask = mockk<Task<Boolean>>()
            every { firebaseRemoteConfig.fetchAndActivate() } returns mockTask
            coEvery { mockTask.await() } returns false
            every { firebaseRemoteConfig.getString("ads_enabled") } returns "false"
            every { firebaseRemoteConfig.getBoolean("ads_enabled") } returns false

            val result = repository.fetchConfiguration()

            assertFalse(result)
            assertFalse(repository.adsEnabled.value)
            assertTrue(repository.isInitialFetchSettled.value)
        }

    @Test
    fun `fetchConfiguration keeps isInitialFetchSettled false when fetchAndActivate fails`() = runTest {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        val mockTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.fetchAndActivate() } returns mockTask
        coEvery { mockTask.await() } throws RuntimeException("Network Error")

        val result = repository.fetchConfiguration()

        assertFalse(result)
        assertFalse(repository.isInitialFetchSettled.value)
    }

    @Test
    fun `real-time ConfigUpdateListener activation sets isInitialFetchSettled to true`() {
        val mockConfigUpdate = mockk<ConfigUpdate>(relaxed = true)
        val updateListenerSlot = slot<ConfigUpdateListener>()
        verify(exactly = 1) { firebaseRemoteConfig.addOnConfigUpdateListener(capture(updateListenerSlot)) }

        val mockActivateTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.activate() } returns mockActivateTask
        val listenerSlot = slot<OnCompleteListener<Boolean>>()
        every { mockActivateTask.addOnCompleteListener(capture(listenerSlot)) } returns mockActivateTask
        every { mockActivateTask.isSuccessful } returns true

        updateListenerSlot.captured.onUpdate(mockConfigUpdate)
        listenerSlot.captured.onComplete(mockActivateTask)

        assertTrue(repository.isInitialFetchSettled.value)
    }

    @Test
    fun `parses optional_update_version_codes trimming whitespace and ignoring invalid or non-positive entries`() {
        every {
            firebaseRemoteConfig.getString("optional_update_version_codes")
        } returns " 1001000 , abc , -5 , 0 , 1002001 "

        val repo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertEquals(setOf(1_001_000L, 1_002_001L), repo.optionalUpdateVersionCodes.value)
    }

    @Test
    fun `falls back to defaults for app update keys when remote config values are blank or negative`() {
        every { firebaseRemoteConfig.getString("in_app_updates_enabled") } returns "   "
        every { firebaseRemoteConfig.getLong("min_supported_version_code") } returns -1L
        every { firebaseRemoteConfig.getString("auto_force_patch_updates") } returns "  "
        every { firebaseRemoteConfig.getString("optional_update_version_codes") } returns "  "
        every { firebaseRemoteConfig.getLong("optional_update_cooldown_hours") } returns 0L

        val repo = FirebaseAppConfigRepository(firebaseRemoteConfig)

        assertTrue(repo.inAppUpdatesEnabled.value)
        assertEquals(0L, repo.minSupportedVersionCode.value)
        assertTrue(repo.autoForcePatchUpdates.value)
        assertEquals(emptySet<Long>(), repo.optionalUpdateVersionCodes.value)
        assertEquals(24L, repo.optionalUpdateCooldownHours.value)
    }

    @Test
    fun `real-time ConfigUpdateListener updates app update flows`() {
        val mockConfigUpdate = mockk<ConfigUpdate>(relaxed = true)
        val updateListenerSlot = slot<ConfigUpdateListener>()
        verify(exactly = 1) { firebaseRemoteConfig.addOnConfigUpdateListener(capture(updateListenerSlot)) }

        val mockActivateTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.activate() } returns mockActivateTask
        val listenerSlot = slot<OnCompleteListener<Boolean>>()
        every { mockActivateTask.addOnCompleteListener(capture(listenerSlot)) } returns mockActivateTask
        every { mockActivateTask.isSuccessful } returns true

        every { firebaseRemoteConfig.getString("in_app_updates_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("in_app_updates_enabled") } returns false
        every { firebaseRemoteConfig.getLong("min_supported_version_code") } returns 1_003_000L

        updateListenerSlot.captured.onUpdate(mockConfigUpdate)
        listenerSlot.captured.onComplete(mockActivateTask)

        assertFalse(repository.inAppUpdatesEnabled.value)
        assertEquals(1_003_000L, repository.minSupportedVersionCode.value)
    }

    @Test
    fun `parses valid category_classifier_rules_json from Remote Config on init and fetchConfiguration`() = runTest {
        every {
            firebaseRemoteConfig.getString("category_classifier_rules_json")
        } returns
            """{"version":1,"merge_mode":"merge","common":[{"category":"FOOD",""" +
            """"subcategory":"DELIVERY","keywords":["rappi"]}]}"""

        val initializedRepo = FirebaseAppConfigRepository(firebaseRemoteConfig)
        assertEquals(
            listOf(
                CategoryKeywordRule(
                    category = ExpenseCategory.FOOD,
                    subcategory = ExpenseSubcategory.DELIVERY,
                    keywords = listOf("rappi")
                )
            ),
            initializedRepo.categoryClassifierRules.value.commonRules
        )

        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        val mockTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.fetchAndActivate() } returns mockTask
        coEvery { mockTask.await() } returns true
        setupMockRemoteConfigValues()

        val updated = repository.fetchConfiguration()

        assertTrue(updated)
        assertEquals(CategoryRuleMergeMode.MERGE, repository.categoryClassifierRules.value.mergeMode)
        assertEquals(
            listOf(
                CategoryKeywordRule(
                    category = ExpenseCategory.FOOD,
                    subcategory = ExpenseSubcategory.CAFE_BREAKFAST,
                    keywords = listOf("boulangerie")
                )
            ),
            repository.categoryClassifierRules.value.languageRules["fr"]
        )
    }

    @Test
    fun `falls back to EMPTY CategoryClassifierRuleConfig when category_classifier_rules_json is malformed or blank`() {
        every { firebaseRemoteConfig.getString("category_classifier_rules_json") } returns "{malformed"
        val malformedRepo = FirebaseAppConfigRepository(firebaseRemoteConfig)
        assertEquals(CategoryClassifierRuleConfig.EMPTY, malformedRepo.categoryClassifierRules.value)

        every { firebaseRemoteConfig.getString("category_classifier_rules_json") } returns "   "
        val blankRepo = FirebaseAppConfigRepository(firebaseRemoteConfig)
        assertEquals(CategoryClassifierRuleConfig.EMPTY, blankRepo.categoryClassifierRules.value)
    }

    @Test
    fun `real-time ConfigUpdateListener updates categoryClassifierRules flow`() {
        val mockConfigUpdate = mockk<ConfigUpdate>(relaxed = true)
        val updateListenerSlot = slot<ConfigUpdateListener>()
        verify(exactly = 1) { firebaseRemoteConfig.addOnConfigUpdateListener(capture(updateListenerSlot)) }

        val mockActivateTask = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.activate() } returns mockActivateTask
        val listenerSlot = slot<OnCompleteListener<Boolean>>()
        every { mockActivateTask.addOnCompleteListener(capture(listenerSlot)) } returns mockActivateTask
        every { mockActivateTask.isSuccessful } returns true

        every {
            firebaseRemoteConfig.getString("category_classifier_rules_json")
        } returns """{"merge_mode":"replace","disabled_terms":{"es":["bote"]}}"""

        updateListenerSlot.captured.onUpdate(mockConfigUpdate)
        listenerSlot.captured.onComplete(mockActivateTask)

        assertEquals(CategoryRuleMergeMode.REPLACE, repository.categoryClassifierRules.value.mergeMode)
        assertEquals(setOf("bote"), repository.categoryClassifierRules.value.disabledTerms["es"])
    }

    private fun setupMockRemoteConfigValues() {
        every { firebaseRemoteConfig.getString("default_currency_code") } returns "GBP"
        every { firebaseRemoteConfig.getLong("balance_computation_debounce_ms") } returns 100L
        every { firebaseRemoteConfig.getLong("max_members_per_group") } returns 25L
        every { firebaseRemoteConfig.getString("subscription_gating_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("subscription_gating_enabled") } returns false
        every { firebaseRemoteConfig.getString("billing_simulation_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("billing_simulation_enabled") } returns false
        every { firebaseRemoteConfig.getLong("max_owned_groups_free") } returns 3L
        every { firebaseRemoteConfig.getLong("max_owned_groups_pro") } returns 200L
        every { firebaseRemoteConfig.getLong("max_members_per_group_free") } returns 8L
        every { firebaseRemoteConfig.getLong("max_members_per_group_pro") } returns 50L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_free") } returns 10L
        every { firebaseRemoteConfig.getLong("ai_receipt_monthly_limit_pro") } returns 250L
        every { firebaseRemoteConfig.getLong("manual_receipt_monthly_limit_free") } returns 8L
        every { firebaseRemoteConfig.getLong("extracted_date_max_future_days") } returns 60L
        every { firebaseRemoteConfig.getString("support_email_address") } returns "fetch-support@splittrip.com"
        every { firebaseRemoteConfig.getLong("settlement_nudge_rate_limit_hours") } returns 12L
        every { firebaseRemoteConfig.getString("ocr_safety_false_positives_blacklist") } returns "fuck,dick,pussy,cunt"
        every { firebaseRemoteConfig.getString("developer_allowlist_email_hashes") } returns "ABC123, def456"
        every { firebaseRemoteConfig.getString("pro_allowlist_email_hashes") } returns "PRO123, pro456"
        every { firebaseRemoteConfig.getString("ads_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("ads_enabled") } returns false
        every { firebaseRemoteConfig.getString("admob_test_mode_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("admob_test_mode_enabled") } returns false
        every { firebaseRemoteConfig.getString("admob_banner_ad_unit_id") } returns "banner-new"
        every { firebaseRemoteConfig.getString("admob_interstitial_ad_unit_id") } returns "interstitial-new"
        every { firebaseRemoteConfig.getLong("ad_interstitial_action_frequency") } returns 3L
        every { firebaseRemoteConfig.getLong("ad_interstitial_min_interval_seconds") } returns 60L
        every { firebaseRemoteConfig.getString("in_app_updates_enabled") } returns "false"
        every { firebaseRemoteConfig.getBoolean("in_app_updates_enabled") } returns false
        every { firebaseRemoteConfig.getLong("min_supported_version_code") } returns 1_000_000L
        every { firebaseRemoteConfig.getString("auto_force_patch_updates") } returns "true"
        every { firebaseRemoteConfig.getBoolean("auto_force_patch_updates") } returns true
        every { firebaseRemoteConfig.getString("optional_update_version_codes") } returns "1005000, 1006001"
        every { firebaseRemoteConfig.getLong("optional_update_cooldown_hours") } returns 72L
        every {
            firebaseRemoteConfig.getString("category_classifier_rules_json")
        } returns
            """{"version":1,"merge_mode":"merge","languages":{"fr":[{"category":"FOOD",""" +
            """"subcategory":"CAFE_BREAKFAST","keywords":["boulangerie"]}]}}"""
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
