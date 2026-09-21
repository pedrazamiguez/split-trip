package es.pedrazamiguez.splittrip.domain.usecase.ad

import kotlinx.coroutines.flow.StateFlow

interface GetBannerAdUnitIdUseCase {
    operator fun invoke(): StateFlow<String>
}
