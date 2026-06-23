package es.pedrazamiguez.splittrip.domain.service

import kotlinx.coroutines.flow.StateFlow

interface AppConfigService {
    val defaultCurrencyCode: StateFlow<String>
    val balanceComputationDebounceMs: StateFlow<Long>
    val maxMembersPerGroup: StateFlow<Int>
}
