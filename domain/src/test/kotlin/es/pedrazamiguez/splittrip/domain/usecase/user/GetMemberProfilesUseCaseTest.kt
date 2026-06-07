package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.GetMemberProfilesUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetMemberProfilesUseCaseTest {

    private val repository: UserRepository = mockk()
    private val useCase = GetMemberProfilesUseCaseImpl(repository)

    @Test
    fun `invoke delegates to repository`() = runTest {
        val userIds = listOf("user_1", "user_2")
        val expectedMap: Map<String, User> = emptyMap()
        coEvery { repository.getUsersByIds(userIds) } returns expectedMap

        val result = useCase(userIds)

        assertEquals(expectedMap, result)
        coVerify(exactly = 1) { repository.getUsersByIds(userIds) }
    }
}
