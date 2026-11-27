package es.pedrazamiguez.splittrip.data.firebase.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalog
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import timber.log.Timber

internal object FirebaseModelPackConfigParser {

    private const val DEFAULT_PRIORITY_MULTIPLIER = 10
    private const val DEFAULT_MIN_RAM_MB = 4096
    private const val DEFAULT_MIN_STORAGE_BYTES = 1_073_741_824L

    fun parseCatalog(
        gson: Gson,
        jsonStr: String,
        defaultCatalog: ModelPackCatalog
    ): ModelPackCatalog {
        if (jsonStr.isBlank()) return defaultCatalog
        return try {
            val catalogDto = gson.fromJson(jsonStr, ModelPackCatalogDto::class.java) ?: return defaultCatalog
            val rawPacks = catalogDto.packs
            if (rawPacks.isNullOrEmpty()) return defaultCatalog

            val parsedPacks = rawPacks
                .mapNotNull { dto -> mapDtoToConfig(dto) }
                .distinctBy { it.packId }

            if (parsedPacks.isNotEmpty()) ModelPackCatalog(parsedPacks) else defaultCatalog
        } catch (e: Exception) {
            Timber.w(e, "Firebase Remote Config: Failed to parse local_ai_model_pack_json.")
            defaultCatalog
        }
    }

    private fun mapDtoToConfig(dto: ModelPackConfigDto): ModelPackConfig? {
        val packId = dto.packId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val tier = ModelPackTier.fromCode(dto.tier)
        val displayName = dto.displayName?.trim().orEmpty().ifBlank { packId }
        val priority = dto.priority ?: (tier.rank * DEFAULT_PRIORITY_MULTIPLIER)
        return ModelPackConfig(
            packId = packId,
            displayName = displayName,
            tier = tier,
            priority = priority,
            version = dto.version?.trim().orEmpty(),
            downloadUrl = dto.downloadUrl?.trim().orEmpty(),
            sha256Checksum = dto.sha256Checksum?.trim().orEmpty(),
            sizeBytes = (dto.sizeBytes ?: 0L).coerceAtLeast(0L),
            minRamMb = (dto.minRamMb ?: DEFAULT_MIN_RAM_MB).coerceAtLeast(0),
            minFreeStorageBytes = (dto.minFreeStorageBytes ?: DEFAULT_MIN_STORAGE_BYTES).coerceAtLeast(0L),
            isEnabled = dto.isEnabled ?: false
        )
    }

    private data class ModelPackCatalogDto(
        @SerializedName("packs") val packs: List<ModelPackConfigDto>? = null
    )

    private data class ModelPackConfigDto(
        @SerializedName("pack_id") val packId: String? = null,
        @SerializedName("display_name") val displayName: String? = null,
        @SerializedName("tier") val tier: String? = null,
        @SerializedName("priority") val priority: Int? = null,
        @SerializedName("version") val version: String? = null,
        @SerializedName("download_url") val downloadUrl: String? = null,
        @SerializedName("sha256_checksum") val sha256Checksum: String? = null,
        @SerializedName("size_bytes") val sizeBytes: Long? = null,
        @SerializedName("min_ram_mb") val minRamMb: Int? = null,
        @SerializedName("min_free_storage_bytes") val minFreeStorageBytes: Long? = null,
        @SerializedName("is_enabled") val isEnabled: Boolean? = null
    )
}
