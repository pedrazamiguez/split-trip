package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.Balance
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetBalancesUseCase

class GetBalancesUseCaseImpl : GetBalancesUseCase {

    override suspend operator fun invoke(): Result<List<Balance>> = try {
        // Placeholder for actual implementation
        Result.success(emptyList())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
