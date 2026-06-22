package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class FeatureGateServiceImpl(
    private val authenticationService: AuthenticationService
) : FeatureGateService {

    override fun isFeatureEnabled(feature: GatedFeature): Flow<Boolean> = flow {
        val isAnon = authenticationService.isAnonymous()
        when (feature) {
            GatedFeature.GROUP_COVER_UPLOAD -> emit(!isAnon)
            GatedFeature.SUBUNIT_CREATION -> emit(!isAnon)
            GatedFeature.AI_RECEIPT_SCANNING -> emit(!isAnon)
        }
    }

    override fun checkLimit(limit: GatedLimit, currentCount: Int): Flow<LimitResult> = flow {
        val isAnon = authenticationService.isAnonymous()
        val maxAllowed = when (limit) {
            GatedLimit.MAX_GROUPS_COUNT -> if (isAnon) 1 else Int.MAX_VALUE
            GatedLimit.MAX_MEMBERS_PER_GROUP -> if (isAnon) 3 else Int.MAX_VALUE
        }
        if (currentCount >= maxAllowed) {
            emit(LimitResult.Blocked(limit, upgradeRequired = isAnon))
        } else {
            emit(LimitResult.Allowed)
        }
    }
}
