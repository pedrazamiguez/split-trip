package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteCashWithdrawalUseCase

class DeleteCashWithdrawalUseCaseImpl(
    private val cashWithdrawalRepository: CashWithdrawalRepository,
    private val groupMembershipService: GroupMembershipService
) : DeleteCashWithdrawalUseCase {

    override suspend operator fun invoke(groupId: String, withdrawalId: String) {
        groupMembershipService.requireMembership(groupId)
        cashWithdrawalRepository.deleteWithdrawal(groupId, withdrawalId)
    }
}
