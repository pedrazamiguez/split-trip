package es.pedrazamiguez.splittrip.domain.usecase.subunit.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.exception.ValidationException
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.SubunitValidationService
import es.pedrazamiguez.splittrip.domain.usecase.subunit.UpdateSubunitUseCase

class UpdateSubunitUseCaseImpl(
    private val subunitRepository: SubunitRepository,
    private val groupRepository: GroupRepository,
    private val groupMembershipService: GroupMembershipService,
    private val subunitValidationService: SubunitValidationService
) : UpdateSubunitUseCase {

    /**
     * Updates a subunit in the specified group.
     *
     * @param groupId The group the subunit belongs to.
     * @param subunit The subunit data with updated fields.
     * @return [Result.success] on success, or [Result.failure] on validation/permission error.
     */
    override suspend operator fun invoke(groupId: String, subunit: Subunit): Result<Unit> = runCatching {
        require(subunit.id.isNotBlank()) { "Subunit ID must not be blank for update" }

        val group = groupRepository.getGroupById(groupId)
            ?: throw IllegalArgumentException("Group not found with id: $groupId")
        if (group.status == GroupStatus.ARCHIVED) {
            throw GroupArchivedException(groupId)
        }

        groupMembershipService.requireMembership(groupId)

        val existingSubunits = subunitRepository.getGroupSubunits(groupId)
        val groupMemberIds = group.members

        val validationResult = subunitValidationService.validate(
            subunit = subunit,
            groupMemberIds = groupMemberIds,
            existingSubunits = existingSubunits,
            excludeSubunitId = subunit.id
        )

        when (validationResult) {
            is SubunitValidationService.ValidationResult.Invalid -> {
                throw ValidationException("Validation failed: ${validationResult.error.name}")
            }

            is SubunitValidationService.ValidationResult.Valid -> {
                subunitRepository.updateSubunit(groupId, validationResult.subunit)
            }
        }
    }
}
