package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.model.DeveloperInfo
import kotlinx.coroutines.flow.StateFlow

interface AppConfigRepository {
    val defaultCurrencyCode: StateFlow<String>
    val balanceComputationDebounceMs: StateFlow<Long>
    val maxMembersPerGroup: StateFlow<Int>
    val subscriptionGatingEnabled: StateFlow<Boolean>
    val maxOwnedGroupsFree: StateFlow<Int>
    val maxOwnedGroupsPro: StateFlow<Int>
    val maxMembersPerGroupFree: StateFlow<Int>
    val maxMembersPerGroupPro: StateFlow<Int>
    val aiReceiptMonthlyLimitFree: StateFlow<Int>
    val aiReceiptMonthlyLimitPro: StateFlow<Int>
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

    suspend fun fetchConfiguration(): Boolean
}
