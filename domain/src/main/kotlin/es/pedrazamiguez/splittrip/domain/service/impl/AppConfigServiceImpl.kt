package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import kotlinx.coroutines.flow.StateFlow

class AppConfigServiceImpl(
    private val appConfigRepository: AppConfigRepository
) : AppConfigService {
    override val defaultCurrencyCode: StateFlow<String> = appConfigRepository.defaultCurrencyCode
    override val balanceComputationDebounceMs: StateFlow<Long> = appConfigRepository.balanceComputationDebounceMs
}
