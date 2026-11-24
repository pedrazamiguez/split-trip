package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.CategoryClassifierRuleConfig
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import kotlinx.coroutines.flow.StateFlow

class AppConfigServiceImpl(
    private val appConfigRepository: AppConfigRepository
) : AppConfigService {
    override val defaultCurrencyCode: StateFlow<String> = appConfigRepository.defaultCurrencyCode
    override val balanceComputationDebounceMs: StateFlow<Long> = appConfigRepository.balanceComputationDebounceMs
    override val maxMembersPerGroup: StateFlow<Int> = appConfigRepository.maxMembersPerGroup
    override val subscriptionGatingEnabled: StateFlow<Boolean> = appConfigRepository.subscriptionGatingEnabled
    override val billingSimulationEnabled: StateFlow<Boolean> = appConfigRepository.billingSimulationEnabled
    override val maxOwnedGroupsFree: StateFlow<Int> = appConfigRepository.maxOwnedGroupsFree
    override val maxOwnedGroupsPro: StateFlow<Int> = appConfigRepository.maxOwnedGroupsPro
    override val maxMembersPerGroupFree: StateFlow<Int> = appConfigRepository.maxMembersPerGroupFree
    override val maxMembersPerGroupPro: StateFlow<Int> = appConfigRepository.maxMembersPerGroupPro
    override val aiReceiptMonthlyLimitFree: StateFlow<Int> = appConfigRepository.aiReceiptMonthlyLimitFree
    override val aiReceiptMonthlyLimitPro: StateFlow<Int> = appConfigRepository.aiReceiptMonthlyLimitPro
    override val manualReceiptMonthlyLimitFree: StateFlow<Int> =
        appConfigRepository.manualReceiptMonthlyLimitFree
    override val reportExportMonthlyLimitFree: StateFlow<Int> =
        appConfigRepository.reportExportMonthlyLimitFree
    override val maxSubunitsPerGroupFree: StateFlow<Int> =
        appConfigRepository.maxSubunitsPerGroupFree
    override val maxSubunitsPerGroupPro: StateFlow<Int> =
        appConfigRepository.maxSubunitsPerGroupPro
    override val extractedDateMaxFutureDays: StateFlow<Int> = appConfigRepository.extractedDateMaxFutureDays
    override val supportEmailAddress: StateFlow<String> = appConfigRepository.supportEmailAddress
    override val settlementNudgeRateLimitHours: StateFlow<Long> = appConfigRepository.settlementNudgeRateLimitHours
    override val developerAllowlistEmailHashes: StateFlow<Set<String>> =
        appConfigRepository.developerAllowlistEmailHashes
    override val proAllowlistEmailHashes: StateFlow<Set<String>> =
        appConfigRepository.proAllowlistEmailHashes
    override val inAppUpdatesEnabled: StateFlow<Boolean> =
        appConfigRepository.inAppUpdatesEnabled
    override val minSupportedVersionCode: StateFlow<Long> =
        appConfigRepository.minSupportedVersionCode
    override val autoForcePatchUpdates: StateFlow<Boolean> =
        appConfigRepository.autoForcePatchUpdates
    override val optionalUpdateVersionCodes: StateFlow<Set<Long>> =
        appConfigRepository.optionalUpdateVersionCodes
    override val optionalUpdateCooldownHours: StateFlow<Long> =
        appConfigRepository.optionalUpdateCooldownHours
    override val isInitialFetchSettled: StateFlow<Boolean> =
        appConfigRepository.isInitialFetchSettled
    override val categoryClassifierRules: StateFlow<CategoryClassifierRuleConfig> =
        appConfigRepository.categoryClassifierRules
}
