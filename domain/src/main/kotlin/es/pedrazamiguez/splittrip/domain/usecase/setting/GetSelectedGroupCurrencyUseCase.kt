package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface GetSelectedGroupCurrencyUseCase : UseCase {
    operator fun invoke(): Flow<String?>
}
