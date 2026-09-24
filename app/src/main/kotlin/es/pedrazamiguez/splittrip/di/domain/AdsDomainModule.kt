package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.usecase.ad.GetBannerAdUnitIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.ad.ShouldShowAdsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.ad.impl.GetBannerAdUnitIdUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.ad.impl.ShouldShowAdsUseCaseImpl
import org.koin.dsl.module

val adsDomainModule = module {
    factory<ShouldShowAdsUseCase> {
        val appConfigRepository = get<AppConfigRepository>()
        val featureGateService = get<FeatureGateService>()
        ShouldShowAdsUseCaseImpl(
            appConfigRepository = appConfigRepository,
            featureGateService = featureGateService
        )
    }

    factory<GetBannerAdUnitIdUseCase> {
        val appConfigRepository = get<AppConfigRepository>()
        GetBannerAdUnitIdUseCaseImpl(
            appConfigRepository = appConfigRepository
        )
    }
}
