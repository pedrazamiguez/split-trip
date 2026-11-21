package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.OptionalUpdateDismissal
import java.time.YearMonth
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

    fun getBiometricLockEnabled(): Flow<Boolean>
    suspend fun setBiometricLockEnabled(enabled: Boolean)

    suspend fun getOptionalUpdateDismissal(): OptionalUpdateDismissal?
    suspend fun recordOptionalUpdateDismissal(versionCode: Long, timestampMs: Long)

    fun getMonthlyAiReceiptScanCount(yearMonth: YearMonth): Flow<Int>
    suspend fun incrementMonthlyAiReceiptScanCount(yearMonth: YearMonth)

    fun getMonthlyManualReceiptAttachmentCount(yearMonth: YearMonth): Flow<Int>
    suspend fun incrementMonthlyManualReceiptAttachmentCount(yearMonth: YearMonth)

    suspend fun clearAll()
}
