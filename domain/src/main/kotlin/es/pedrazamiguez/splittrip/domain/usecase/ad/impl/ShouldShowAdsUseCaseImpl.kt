package es.pedrazamiguez.splittrip.domain.usecase.ad.impl

import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.usecase.ad.ShouldShowAdsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ShouldShowAdsUseCaseImpl(
    private val appConfigRepository: AppConfigRepository,
    private val featureGateService: FeatureGateService
) : ShouldShowAdsUseCase {

    override fun invoke(): Flow<Boolean> = combine(
        appConfigRepository.adsEnabled,
        featureGateService.isFeatureEnabled(GatedFeature.AD_FREE)
    ) { adsEnabled, isAdFree ->
        adsEnabled && !isAdFree
    }
}
