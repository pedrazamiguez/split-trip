package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.CropRect

interface ProfileImageStorageService {
    suspend fun saveAndCompressAvatar(userId: String, sourceUri: String, cropRect: CropRect?): String
    suspend fun deleteLocalAvatar(userId: String)
}
