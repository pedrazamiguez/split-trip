package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface AddCashWithdrawalUseCase : UseCase {
    suspend operator fun invoke(groupId: String?, withdrawal: CashWithdrawal): Result<Unit>
}
