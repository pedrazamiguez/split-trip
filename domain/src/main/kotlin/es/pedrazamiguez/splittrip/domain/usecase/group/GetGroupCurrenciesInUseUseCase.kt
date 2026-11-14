package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetGroupCurrenciesInUseUseCase : UseCase {
    suspend operator fun invoke(groupId: String): Set<String>
}
