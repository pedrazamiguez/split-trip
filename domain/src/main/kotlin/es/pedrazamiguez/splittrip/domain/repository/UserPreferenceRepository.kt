package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import kotlinx.coroutines.flow.Flow

interface UserPreferenceRepository {

    fun getUserDefaultCurrency(): Flow<String>
    suspend fun setUserDefaultCurrency(currencyCode: String)

    fun getActiveAiEngine(): Flow<AiEngineType>
    suspend fun setActiveAiEngine(engineType: AiEngineType)

    suspend fun clearAll()
}
