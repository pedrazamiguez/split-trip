package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ContributionValidationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddContributionUseCase

class AddContributionUseCaseImpl(
    private val contributionRepository: ContributionRepository,
    private val groupMembershipService: GroupMembershipService,
    private val contributionValidationService: ContributionValidationService,
    private val subunitRepository: SubunitRepository,
    private val authenticationService: AuthenticationService,
    private val groupRepository: GroupRepository
) : AddContributionUseCase {

    override suspend operator fun invoke(groupId: String, contribution: Contribution) {
        val group = groupRepository.getGroupById(groupId)
            ?: throw IllegalArgumentException("Group not found with id: $groupId")
        if (group.status == GroupStatus.ARCHIVED) {
            throw GroupArchivedException(groupId)
        }
        groupMembershipService.requireMembership(groupId)

        // Validate amount
        val amountResult = contributionValidationService.validateAmount(contribution.amount)
        require(amountResult !is ContributionValidationService.ValidationResult.Invalid) {
            val invalid = amountResult as ContributionValidationService.ValidationResult.Invalid
            "Invalid contribution amount: ${invalid.error}"
        }

        // Validate contribution scope
        when (contribution.contributionScope) {
            PayerType.SUBUNIT -> {
                // Resolve target: impersonated member or self (fallback)
                val targetUserId =
                    contribution.userId.ifBlank { authenticationService.requireUserId() }

                // Validate target is a group member (prevents impersonating non-members)
                if (contribution.userId.isNotBlank()) {
                    groupMembershipService.requireUserInGroup(groupId, targetUserId)
                }

                // SUBUNIT requires a valid subunit + target membership — fetch subunits
                val groupSubunits = subunitRepository.getGroupSubunits(groupId)
                val scopeResult = contributionValidationService.validateContributionScope(
                    contributionScope = contribution.contributionScope,
                    subunitId = contribution.subunitId,
                    userId = targetUserId,
                    groupSubunits = groupSubunits
                )
                require(scopeResult !is ContributionValidationService.ValidationResult.Invalid) {
                    val invalid =
                        scopeResult as ContributionValidationService.ValidationResult.Invalid
                    "Invalid contribution scope: ${invalid.error}"
                }
            }

            else -> {
                // GROUP / USER must not have a subunitId — no I/O needed
                val error =
                    ContributionValidationService.ValidationError.INVALID_SUBUNIT_FOR_SCOPE
                require(contribution.subunitId == null) {
                    "Invalid contribution scope: $error"
                }
            }
        }

        contributionRepository.addContribution(groupId, contribution)
    }
}
