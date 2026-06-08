package es.pedrazamiguez.splittrip.domain.usecase.subunit.impl

import es.pedrazamiguez.splittrip.domain.exception.ValidationException
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.SubunitValidationService
import es.pedrazamiguez.splittrip.domain.usecase.subunit.CreateSubunitUseCase

class CreateSubunitUseCaseImpl(
    private val subunitRepository: SubunitRepository,
    private val groupRepository: GroupRepository,
    private val groupMembershipService: GroupMembershipService,
    private val subunitValidationService: SubunitValidationService
) : CreateSubunitUseCase {

    /**
     * Creates a subunit in the specified group.
     *
     * @param groupId The group to create the subunit in.
     * @param subunit The subunit data to create.
     * @return [Result.success] with the generated subunit ID, or [Result.failure] on error.
     */
    override suspend operator fun invoke(groupId: String, subunit: Subunit): Result<String> = runCatching {
        groupMembershipService.requireMembership(groupId)

        val existingSubunits = subunitRepository.getGroupSubunits(groupId)
        val group = requireNotNull(groupRepository.getGroupById(groupId)) {
            "Group $groupId not found after membership check"
        }
        val groupMemberIds = group.members

        val validationResult = subunitValidationService.validate(
            subunit = subunit,
            groupMemberIds = groupMemberIds,
            existingSubunits = existingSubunits
        )

        when (validationResult) {
            is SubunitValidationService.ValidationResult.Invalid -> {
                throw ValidationException("Validation failed: ${validationResult.error.name}")
            }

            is SubunitValidationService.ValidationResult.Valid -> {
                subunitRepository.createSubunit(groupId, validationResult.subunit)
            }
        }
    }
}
