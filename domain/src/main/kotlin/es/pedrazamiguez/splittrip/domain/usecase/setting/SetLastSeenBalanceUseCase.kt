package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SetLastSeenBalanceUseCase : UseCase {
    suspend operator fun invoke(groupId: String, formattedBalance: String)
}
