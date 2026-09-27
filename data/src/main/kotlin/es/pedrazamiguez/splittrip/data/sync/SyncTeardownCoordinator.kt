package es.pedrazamiguez.splittrip.data.sync

import es.pedrazamiguez.splittrip.domain.service.SyncTeardownService

class SyncTeardownCoordinator : SyncTeardownService {
    private val teardownActions = mutableListOf<() -> Unit>()

    fun registerAction(action: () -> Unit) {
        teardownActions.add(action)
    }

    override suspend fun teardownAll() {
        teardownActions.forEach { it.invoke() }
    }
}
