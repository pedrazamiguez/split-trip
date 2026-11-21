package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import java.time.YearMonth

class CreateGroupUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val yearMonthProvider: () -> YearMonth = { YearMonth.now() }
) : CreateGroupUseCase {

    override suspend operator fun invoke(group: Group, members: List<User>): Result<String> = runCatching {
        members.filter { it.isPending }.forEach { pendingUser ->
            userRepository.saveUser(pendingUser).getOrThrow()
        }
        val groupId = groupRepository.createGroup(group)
        if (!group.mainImagePath.isNullOrBlank()) {
            userPreferenceRepository.incrementMonthlyManualReceiptAttachmentCount(yearMonthProvider())
        }
        groupId
    }
}
