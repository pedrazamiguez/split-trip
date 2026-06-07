package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface DeleteContributionUseCase : UseCase {
    suspend operator fun invoke(groupId: String, contributionId: String)
}
