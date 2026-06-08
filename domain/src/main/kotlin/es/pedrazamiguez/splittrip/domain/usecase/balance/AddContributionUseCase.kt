package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface AddContributionUseCase : UseCase {
    suspend operator fun invoke(groupId: String, contribution: Contribution)
}
