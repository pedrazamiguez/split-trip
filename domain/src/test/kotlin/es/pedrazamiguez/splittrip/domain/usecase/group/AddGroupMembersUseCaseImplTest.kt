package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.AddGroupMembersUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AddGroupMembersUseCaseImplTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: AddGroupMembersUseCase

    private val groupId = "group-123"
    private val registeredUser = User(userId = "user-1", email = "alice@example.com", isPending = false)
    private val pendingUser = User(userId = "pending-uid", email = "pending@example.com", isPending = true)

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        userRepository = mockk()
        useCase = AddGroupMembersUseCaseImpl(
            groupRepository = groupRepository,
            userRepository = userRepository
        )
        coEvery { groupRepository.addMembers(any(), any()) } returns Unit
    }

    @Test
    fun `saves pending users and adds members to group`() = runTest {
        coEvery { userRepository.saveUser(pendingUser) } returns Result.success(Unit)

        val result = useCase(groupId, listOf(registeredUser, pendingUser))

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { userRepository.saveUser(pendingUser) }
        coVerify(exactly = 0) { userRepository.saveUser(registeredUser) }
        coVerify(exactly = 1) { groupRepository.addMembers(groupId, listOf("user-1", "pending-uid")) }
    }

    @Test
    fun `skips saving registered users`() = runTest {
        val result = useCase(groupId, listOf(registeredUser))

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { userRepository.saveUser(any()) }
        coVerify(exactly = 1) { groupRepository.addMembers(groupId, listOf("user-1")) }
    }

    @Test
    fun `propagates failure when saving pending user fails`() = runTest {
        coEvery { userRepository.saveUser(pendingUser) } returns Result.failure(RuntimeException("Save failed"))

        val result = useCase(groupId, listOf(pendingUser))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { groupRepository.addMembers(any(), any()) }
    }

    @Test
    fun `propagates failure when group repository fails`() = runTest {
        coEvery { groupRepository.addMembers(any(), any()) } throws RuntimeException("Add members failed")

        val result = useCase(groupId, listOf(registeredUser))

        assertTrue(result.isFailure)
    }
}
