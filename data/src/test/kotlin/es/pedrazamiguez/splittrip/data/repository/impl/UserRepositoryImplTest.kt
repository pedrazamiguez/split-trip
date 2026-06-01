package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalUserDataSource
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UserRepositoryImpl")
class UserRepositoryImplTest {

    private lateinit var cloudUserDataSource: CloudUserDataSource
    private lateinit var localUserDataSource: LocalUserDataSource
    private lateinit var authenticationService: AuthenticationService
    private lateinit var repository: UserRepositoryImpl

    private val testUser = User(
        userId = "user-1",
        email = "alice@example.com",
        displayName = "Alice",
        profileImagePath = null,
        createdAt = LocalDateTime.of(2026, 3, 15, 10, 30)
    )

    @BeforeEach
    fun setUp() {
        cloudUserDataSource = mockk(relaxed = true)
        localUserDataSource = mockk(relaxed = true)
        authenticationService = mockk()

        repository = UserRepositoryImpl(
            cloudUserDataSource = cloudUserDataSource,
            localUserDataSource = localUserDataSource,
            authenticationService = authenticationService
        )
    }

    @Nested
    inner class SaveUser {

        @Test
        fun `saves user to cloud and caches locally`() = runTest {
            coEvery { cloudUserDataSource.saveUser(testUser) } just Runs
            coEvery { localUserDataSource.saveUsers(listOf(testUser)) } just Runs

            val result = repository.saveUser(testUser)

            assertTrue(result.isSuccess)
            coVerify { cloudUserDataSource.saveUser(testUser) }
            coVerify { localUserDataSource.saveUsers(listOf(testUser)) }
        }

        @Test
        fun `returns failure when cloud save throws`() = runTest {
            coEvery { cloudUserDataSource.saveUser(testUser) } throws RuntimeException("Cloud error")

            val result = repository.saveUser(testUser)

            assertTrue(result.isFailure)
        }
    }

    @Nested
    inner class SaveGoogleUser {

        @Test
        fun `saves user to cloud and caches locally`() = runTest {
            coEvery { cloudUserDataSource.saveUser(testUser) } just Runs
            coEvery { localUserDataSource.saveUsers(listOf(testUser)) } just Runs

            val result = repository.saveGoogleUser(testUser)

            assertTrue(result.isSuccess)
            coVerify { cloudUserDataSource.saveUser(testUser) }
            coVerify { localUserDataSource.saveUsers(listOf(testUser)) }
        }

        @Test
        fun `returns failure when cloud save throws`() = runTest {
            coEvery { cloudUserDataSource.saveUser(testUser) } throws RuntimeException("Cloud error")

            val result = repository.saveGoogleUser(testUser)

            assertTrue(result.isFailure)
        }
    }

    @Nested
    inner class GetCurrentUserProfile {

        @Test
        fun `returns null when no user is authenticated`() = runTest {
            coEvery { authenticationService.currentUserId() } returns null

            val result = repository.getCurrentUserProfile()

            assertNull(result)
        }

        @Test
        fun `returns local user when fully populated`() = runTest {
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery { localUserDataSource.getUsersByIds(listOf("user-1")) } returns listOf(testUser)

            val result = repository.getCurrentUserProfile()

            assertNotNull(result)
            assertEquals("user-1", result?.userId)
            coVerify(exactly = 0) { cloudUserDataSource.getUsersByIds(any()) }
        }

        @Test
        fun `refreshes from cloud when local user has null createdAt`() = runTest {
            val incompleteUser = testUser.copy(createdAt = null)
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery { localUserDataSource.getUsersByIds(listOf("user-1")) } returns listOf(incompleteUser)
            coEvery { cloudUserDataSource.getUsersByIds(listOf("user-1")) } returns listOf(testUser)
            coEvery { localUserDataSource.saveUsers(listOf(testUser)) } just Runs

            val result = repository.getCurrentUserProfile()

            assertNotNull(result)
            assertEquals(testUser.createdAt, result?.createdAt)
            coVerify { cloudUserDataSource.getUsersByIds(listOf("user-1")) }
            coVerify { localUserDataSource.saveUsers(listOf(testUser)) }
        }

        @Test
        fun `returns local user when cloud refresh returns empty`() = runTest {
            val incompleteUser = testUser.copy(createdAt = null)
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery { localUserDataSource.getUsersByIds(listOf("user-1")) } returns listOf(incompleteUser)
            coEvery { cloudUserDataSource.getUsersByIds(listOf("user-1")) } returns emptyList()

            val result = repository.getCurrentUserProfile()

            assertEquals(incompleteUser, result)
        }

        @Test
        fun `falls back to local user when cloud throws exception`() = runTest {
            val incompleteUser = testUser.copy(createdAt = null)
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery { localUserDataSource.getUsersByIds(listOf("user-1")) } returns listOf(incompleteUser)
            coEvery { cloudUserDataSource.getUsersByIds(any()) } throws RuntimeException("Network error")

            val result = repository.getCurrentUserProfile()

            assertEquals(incompleteUser, result)
        }

        @Test
        fun `refreshes from cloud when user not found locally`() = runTest {
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery { localUserDataSource.getUsersByIds(listOf("user-1")) } returns emptyList()
            coEvery { cloudUserDataSource.getUsersByIds(listOf("user-1")) } returns listOf(testUser)
            coEvery { localUserDataSource.saveUsers(listOf(testUser)) } just Runs

            val result = repository.getCurrentUserProfile()

            assertNotNull(result)
            assertEquals("user-1", result?.userId)
        }
    }

    @Nested
    inner class GetUsersByIds {

        @Test
        fun `returns empty map for empty input`() = runTest {
            val result = repository.getUsersByIds(emptyList())
            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns local users when all are found locally`() = runTest {
            val user2 = testUser.copy(userId = "user-2", email = "bob@example.com")
            coEvery { localUserDataSource.getUsersByIds(listOf("user-1", "user-2")) } returns listOf(testUser, user2)

            val result = repository.getUsersByIds(listOf("user-1", "user-2"))

            assertEquals(2, result.size)
            assertEquals(testUser, result["user-1"])
            coVerify(exactly = 0) { cloudUserDataSource.getUsersByIds(any()) }
        }

        @Test
        fun `fetches missing users from cloud and caches locally`() = runTest {
            val user2 = testUser.copy(userId = "user-2", email = "bob@example.com")
            coEvery { localUserDataSource.getUsersByIds(listOf("user-1", "user-2")) } returns listOf(testUser)
            coEvery { cloudUserDataSource.getUsersByIds(listOf("user-2")) } returns listOf(user2)
            coEvery { localUserDataSource.saveUsers(listOf(user2)) } just Runs

            val result = repository.getUsersByIds(listOf("user-1", "user-2"))

            assertEquals(2, result.size)
            coVerify { cloudUserDataSource.getUsersByIds(listOf("user-2")) }
            coVerify { localUserDataSource.saveUsers(listOf(user2)) }
        }

        @Test
        fun `returns only local users when cloud fetch fails`() = runTest {
            coEvery { localUserDataSource.getUsersByIds(listOf("user-1", "user-2")) } returns listOf(testUser)
            coEvery { cloudUserDataSource.getUsersByIds(any()) } throws RuntimeException("Network error")

            val result = repository.getUsersByIds(listOf("user-1", "user-2"))

            assertEquals(1, result.size)
            assertEquals(testUser, result["user-1"])
        }
    }

    @Nested
    inner class SearchUsersByEmail {

        @Test
        fun `delegates to cloud with current user excluded`() = runTest {
            val foundUser = testUser.copy(userId = "user-2", email = "bob@example.com")
            coEvery { authenticationService.currentUserId() } returns "user-1"
            coEvery {
                cloudUserDataSource.searchUsersByEmail("bob@example.com", excludeUserId = "user-1")
            } returns listOf(foundUser)

            val result = repository.searchUsersByEmail("bob@example.com")

            assertEquals(1, result.size)
            assertEquals("user-2", result[0].userId)
        }

        @Test
        fun `excludes null when not authenticated`() = runTest {
            coEvery { authenticationService.currentUserId() } returns null
            coEvery {
                cloudUserDataSource.searchUsersByEmail("bob@example.com", excludeUserId = null)
            } returns emptyList()

            val result = repository.searchUsersByEmail("bob@example.com")

            assertTrue(result.isEmpty())
        }
    }
}
