package es.pedrazamiguez.splittrip.domain.service

import kotlinx.coroutines.flow.StateFlow

interface AppConfigService {
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
    val extractedDateMaxFutureDays: StateFlow<Int>
    val supportEmailAddress: StateFlow<String>
    val settlementNudgeRateLimitHours: StateFlow<Long>
    val developerAllowlistEmailHashes: StateFlow<Set<String>>
    val proAllowlistEmailHashes: StateFlow<Set<String>>
    val inAppUpdatesEnabled: StateFlow<Boolean>
    val minSupportedVersionCode: StateFlow<Long>
    val autoForcePatchUpdates: StateFlow<Boolean>
    val optionalUpdateVersionCodes: StateFlow<Set<Long>>
    val optionalUpdateCooldownHours: StateFlow<Long>
    val isInitialFetchSettled: StateFlow<Boolean>
}
