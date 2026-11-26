package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import kotlinx.coroutines.flow.Flow

interface LocalAiModelPackService {
    fun observeStatus(): Flow<ModelPackStatus>
    fun checkDeviceCompatibility(): DeviceCompatibilityResult
    fun isPackInstalled(): Boolean
    fun getInstalledModelPath(): String?
    fun observeWifiOnlyPreference(): Flow<Boolean>
    suspend fun setWifiOnlyPreference(wifiOnly: Boolean)
    suspend fun requestDownload(): Result<Unit>
    suspend fun cancelDownload(): Result<Unit>
    suspend fun deletePack(): Result<Unit>
}
