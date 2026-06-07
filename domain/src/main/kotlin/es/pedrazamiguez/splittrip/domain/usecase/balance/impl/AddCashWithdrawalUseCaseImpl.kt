package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.CashWithdrawalValidationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddCashWithdrawalUseCase

class AddCashWithdrawalUseCaseImpl(
    private val cashWithdrawalRepository: CashWithdrawalRepository,
    private val validationService: CashWithdrawalValidationService,
    private val groupMembershipService: GroupMembershipService,
    private val subunitRepository: SubunitRepository,
    private val authenticationService: AuthenticationService
) : AddCashWithdrawalUseCase {

    override suspend operator fun invoke(groupId: String?, withdrawal: CashWithdrawal): Result<Unit> = runCatching {
        require(!groupId.isNullOrBlank()) { "Group ID cannot be null or blank" }

        groupMembershipService.requireMembership(groupId)

        val amountResult = validationService.validateAmountWithdrawn(withdrawal.amountWithdrawn)
        check(amountResult is CashWithdrawalValidationService.ValidationResult.Valid) {
            "Amount withdrawn must be greater than zero"
        }

        val deductedResult = validationService.validateDeductedBaseAmount(withdrawal.deductedBaseAmount)
        check(deductedResult is CashWithdrawalValidationService.ValidationResult.Valid) {
            "Deducted base amount must be greater than zero"
        }

        val currencyResult = validationService.validateCurrency(withdrawal.currency)
        check(currencyResult is CashWithdrawalValidationService.ValidationResult.Valid) {
            "Currency is required"
        }

        val rateResult = validationService.validateExchangeRate(withdrawal.exchangeRate)
        check(rateResult is CashWithdrawalValidationService.ValidationResult.Valid) {
            "Exchange rate must be greater than zero"
        }

        // Validate withdrawal scope and subunit assignment
        if (withdrawal.withdrawalScope == PayerType.SUBUNIT || withdrawal.subunitId != null) {
            // Resolve target: impersonated member or self (fallback)
            val targetUserId =
                withdrawal.withdrawnBy.ifBlank { authenticationService.requireUserId() }

            // Validate target is a group member (prevents impersonating non-members)
            if (withdrawal.withdrawnBy.isNotBlank()) {
                groupMembershipService.requireUserInGroup(groupId, targetUserId)
            }

            val groupSubunits = subunitRepository.getGroupSubunits(groupId)
            val scopeResult = validationService.validateWithdrawalScope(
                withdrawalScope = withdrawal.withdrawalScope,
                subunitId = withdrawal.subunitId,
                userId = targetUserId,
                groupSubunits = groupSubunits
            )
            check(scopeResult is CashWithdrawalValidationService.ValidationResult.Valid) {
                val error =
                    (scopeResult as? CashWithdrawalValidationService.ValidationResult.Invalid)?.error
                "Invalid withdrawal scope: $error"
            }
        }

        cashWithdrawalRepository.addWithdrawal(groupId, withdrawal)
    }
}
