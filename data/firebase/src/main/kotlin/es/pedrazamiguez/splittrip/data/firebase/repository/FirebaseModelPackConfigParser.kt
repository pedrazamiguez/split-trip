package es.pedrazamiguez.splittrip.data.firebase.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import timber.log.Timber

internal object FirebaseModelPackConfigParser {

    fun parse(
        gson: Gson,
        jsonStr: String,
        defaultConfig: ModelPackConfig
    ): ModelPackConfig {
        if (jsonStr.isBlank()) return defaultConfig
        return try {
            val dto = gson.fromJson(jsonStr, ModelPackConfigDto::class.java) ?: return defaultConfig
            ModelPackConfig(
                packId = dto.packId?.takeIf { it.isNotBlank() } ?: defaultConfig.packId,
                version = dto.version?.takeIf { it.isNotBlank() } ?: defaultConfig.version,
                downloadUrl = dto.downloadUrl?.trim().orEmpty(),
                sha256Checksum = dto.sha256Checksum?.trim().orEmpty(),
                sizeBytes = (dto.sizeBytes ?: defaultConfig.sizeBytes).coerceAtLeast(0L),
                minRamMb = (dto.minRamMb ?: defaultConfig.minRamMb).coerceAtLeast(0),
                minFreeStorageBytes = (dto.minFreeStorageBytes ?: defaultConfig.minFreeStorageBytes).coerceAtLeast(0L),
                isEnabled = dto.isEnabled ?: defaultConfig.isEnabled
            )
        } catch (e: Exception) {
            Timber.w(e, "Firebase Remote Config: Failed to parse local_ai_model_pack_json.")
            defaultConfig
        }
    }

    private data class ModelPackConfigDto(
        @SerializedName("pack_id") val packId: String? = null,
        @SerializedName("version") val version: String? = null,
        @SerializedName("download_url") val downloadUrl: String? = null,
        @SerializedName("sha256_checksum") val sha256Checksum: String? = null,
        @SerializedName("size_bytes") val sizeBytes: Long? = null,
        @SerializedName("min_ram_mb") val minRamMb: Int? = null,
        @SerializedName("min_free_storage_bytes") val minFreeStorageBytes: Long? = null,
        @SerializedName("is_enabled") val isEnabled: Boolean? = null
    )
}
