package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalogEntry
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import kotlinx.coroutines.flow.Flow

interface LocalAiModelPackService {
    fun observeStatus(): Flow<ModelPackStatus>
    fun observeCatalogEntries(): Flow<List<ModelPackCatalogEntry>>
    fun checkDeviceCompatibility(): DeviceCompatibilityResult
    fun isPackInstalled(): Boolean
    fun getInstalledModelPath(): String?
    fun observeWifiOnlyPreference(): Flow<Boolean>
    suspend fun setWifiOnlyPreference(wifiOnly: Boolean)
    suspend fun requestDownload(packId: String? = null): Result<Unit>
    suspend fun cancelDownload(packId: String? = null): Result<Unit>
    suspend fun deletePack(): Result<Unit>
}
