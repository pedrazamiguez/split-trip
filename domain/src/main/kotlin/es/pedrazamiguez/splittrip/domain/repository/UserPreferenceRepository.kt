package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface UserPreferenceRepository {

    fun getUserDefaultCurrency(): Flow<String>
    suspend fun setUserDefaultCurrency(currencyCode: String)

    fun getActiveAiEngine(): Flow<AiEngineType>
    suspend fun setActiveAiEngine(engineType: AiEngineType)

    fun getAppLanguage(): Flow<String?>
    suspend fun setAppLanguage(languageCode: String)

    fun getShouldShowLanguagePill(): Flow<Boolean>
    suspend fun setShouldShowLanguagePill(show: Boolean)

    fun getAppTheme(): Flow<String?>
    suspend fun setAppTheme(themeCode: String)

    fun getHasSignedOut(): Flow<Boolean>
    suspend fun setHasSignedOut(signedOut: Boolean)

    fun getIsReconciled(): Flow<Boolean>
    suspend fun setIsReconciled(reconciled: Boolean)

    suspend fun clearAll()
}
