package es.pedrazamiguez.splittrip.domain.service

interface SyncTeardownService {
    suspend fun teardownAll()
}
