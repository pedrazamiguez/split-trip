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

    override fun getAppLanguage(): Flow<String?> = userPreferences.appLanguage

    override suspend fun setAppLanguage(languageCode: String) {
        userPreferences.setAppLanguage(languageCode)
    }

    override fun getShouldShowLanguagePill(): Flow<Boolean> = userPreferences.shouldShowLanguagePill

    override suspend fun setShouldShowLanguagePill(show: Boolean) {
        userPreferences.setShouldShowLanguagePill(show)
    }

    override fun getAppTheme(): Flow<String?> = userPreferences.appTheme

    override suspend fun setAppTheme(themeCode: String) {
        userPreferences.setAppTheme(themeCode)
    }

    override fun getHasSignedOut(): Flow<Boolean> = userPreferences.hasSignedOut

    override suspend fun setHasSignedOut(signedOut: Boolean) {
        userPreferences.setHasSignedOut(signedOut)
    }

    override suspend fun clearAll() {
        userPreferences.clearAll()
    }
}
