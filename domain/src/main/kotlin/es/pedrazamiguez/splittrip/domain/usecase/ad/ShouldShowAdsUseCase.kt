package es.pedrazamiguez.splittrip.domain.usecase.ad

import kotlinx.coroutines.flow.Flow

interface ShouldShowAdsUseCase {
    operator fun invoke(): Flow<Boolean>
}
