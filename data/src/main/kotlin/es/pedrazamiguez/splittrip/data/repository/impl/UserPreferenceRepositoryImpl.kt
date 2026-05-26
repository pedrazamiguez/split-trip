package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.data.local.datastore.UserPreferences
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import kotlinx.coroutines.flow.Flow

class UserPreferenceRepositoryImpl(
    private val userPreferences: UserPreferences
) : UserPreferenceRepository {

    override fun getUserDefaultCurrency(): Flow<String> = userPreferences.defaultCurrency

    override suspend fun setUserDefaultCurrency(currencyCode: String) {
        userPreferences.setDefaultCurrency(currencyCode)
    }

    override fun getActiveAiEngine(): Flow<AiEngineType> = userPreferences.activeAiEngine

    override suspend fun setActiveAiEngine(engineType: AiEngineType) {
        userPreferences.setActiveAiEngine(engineType)
    }

    override suspend fun clearAll() {
        userPreferences.clearAll()
    }
}
