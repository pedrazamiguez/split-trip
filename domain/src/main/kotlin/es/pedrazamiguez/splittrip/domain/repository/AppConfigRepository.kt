package es.pedrazamiguez.splittrip.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface AppConfigRepository {
    val defaultCurrencyCode: StateFlow<String>
    val balanceComputationDebounceMs: StateFlow<Long>

    suspend fun fetchConfiguration(): Boolean
}
