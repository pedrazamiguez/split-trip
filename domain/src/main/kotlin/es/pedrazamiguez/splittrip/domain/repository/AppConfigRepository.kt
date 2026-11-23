package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.model.DeveloperInfo
import kotlinx.coroutines.flow.StateFlow

interface AppConfigRepository {
    val defaultCurrencyCode: StateFlow<String>
    val balanceComputationDebounceMs: StateFlow<Long>
    val maxMembersPerGroup: StateFlow<Int>
    val subscriptionGatingEnabled: StateFlow<Boolean>
    val billingSimulationEnabled: StateFlow<Boolean>
    val maxOwnedGroupsFree: StateFlow<Int>
    val maxOwnedGroupsPro: StateFlow<Int>
    val maxMembersPerGroupFree: StateFlow<Int>
    val maxMembersPerGroupPro: StateFlow<Int>
    val aiReceiptMonthlyLimitFree: StateFlow<Int>
    val aiReceiptMonthlyLimitPro: StateFlow<Int>
    val manualReceiptMonthlyLimitFree: StateFlow<Int>
    val reportExportMonthlyLimitFree: StateFlow<Int>
    val maxSubunitsPerGroupFree: StateFlow<Int>
    val maxSubunitsPerGroupPro: StateFlow<Int>
    val extractedDateMaxFutureDays: StateFlow<Int>
    val supportEmailAddress: StateFlow<String>
    val settlementNudgeRateLimitHours: StateFlow<Long>
    val ocrSafetyFalsePositivesBlacklist: StateFlow<List<String>>
    val developerInfo: StateFlow<DeveloperInfo>
    val adsEnabled: StateFlow<Boolean>
    val admobTestModeEnabled: StateFlow<Boolean>
    val admobBannerAdUnitId: StateFlow<String>
    val admobInterstitialAdUnitId: StateFlow<String>
    val adInterstitialActionFrequency: StateFlow<Int>
    val adInterstitialMinIntervalSeconds: StateFlow<Long>
    val developerAllowlistEmailHashes: StateFlow<Set<String>>
    val proAllowlistEmailHashes: StateFlow<Set<String>>
    val inAppUpdatesEnabled: StateFlow<Boolean>
    val minSupportedVersionCode: StateFlow<Long>
    val autoForcePatchUpdates: StateFlow<Boolean>
    val optionalUpdateVersionCodes: StateFlow<Set<Long>>
    val optionalUpdateCooldownHours: StateFlow<Long>
    val isInitialFetchSettled: StateFlow<Boolean>

    suspend fun fetchConfiguration(): Boolean
}
