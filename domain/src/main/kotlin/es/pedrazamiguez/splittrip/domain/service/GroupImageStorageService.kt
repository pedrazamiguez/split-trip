package es.pedrazamiguez.splittrip.domain.service

interface GroupImageStorageService {
    /** Copies and compresses [sourceUri] to a temporary local WebP file. Returns temp file URI. */
    suspend fun saveTempGroupImage(sourceUri: String): String

    /** Moves temp [tempUri] to a permanent file named [groupId].webp. Returns permanent local URI. */
    suspend fun commitGroupImage(groupId: String, tempUri: String): String

    /** Deletes the permanent local group image for [groupId]. */
    suspend fun deleteLocalGroupImage(groupId: String)

    /** Cleans up leftover temporary files in cache. */
    suspend fun cleanTempGroupImages()
}
