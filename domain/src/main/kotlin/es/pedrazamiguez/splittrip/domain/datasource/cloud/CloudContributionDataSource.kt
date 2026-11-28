package es.pedrazamiguez.splittrip.domain.datasource.cloud

import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import kotlinx.coroutines.flow.Flow

interface CloudContributionDataSource {

    suspend fun addContribution(groupId: String, contribution: Contribution)

    suspend fun deleteContribution(groupId: String, contributionId: String)

    /**
     * One-shot paginated full-collection fetch used for initial bootstrap or expired-checkpoint fallback.
     */
    suspend fun fetchContributionsFullSnapshot(groupId: String): DeltaSyncBatch<Contribution>

    /**
     * One-shot incremental delta fetch for changes after [sinceWatermark].
     */
    suspend fun fetchContributionsDelta(
        groupId: String,
        sinceWatermark: SyncWatermark
    ): DeltaSyncBatch<Contribution>

    /**
     * Reactive delta stream for real-time observers starting after [sinceWatermark].
     */
    fun observeContributionsDeltaFlow(
        groupId: String,
        sinceWatermark: SyncWatermark?
    ): Flow<DeltaSyncBatch<Contribution>>

    /**
     * Verifies that a contribution document exists on the Firestore server (not just local cache).
     * Forces a server round-trip — throws if the device is offline.
     *
     * Used by repositories to confirm that a locally-created contribution has been
     * successfully persisted to the server, enabling the PENDING_SYNC → SYNCED transition.
     *
     * @param groupId The ID of the group containing the contribution.
     * @param contributionId The ID of the contribution to verify.
     * @return true if the contribution exists on the server.
     * @throws Exception if the server is unreachable (e.g., airplane mode).
     */
    suspend fun verifyContributionOnServer(groupId: String, contributionId: String): Boolean
}
