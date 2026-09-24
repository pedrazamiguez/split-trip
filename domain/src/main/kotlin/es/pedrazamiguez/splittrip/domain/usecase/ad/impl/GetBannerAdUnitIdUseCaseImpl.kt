package es.pedrazamiguez.splittrip.domain.usecase.ad.impl

import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.usecase.ad.GetBannerAdUnitIdUseCase
import kotlinx.coroutines.flow.StateFlow

class GetBannerAdUnitIdUseCaseImpl(
    private val appConfigRepository: AppConfigRepository
) : GetBannerAdUnitIdUseCase {

    override fun invoke(): StateFlow<String> = appConfigRepository.admobBannerAdUnitId
}
