package es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.domain.model.User
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UserUiMapperTest {

    private val resourceProvider = mockk<ResourceProvider> {
        every { getString(R.string.user_pending_fallback) } returns "Pending member"
    }
    private val mapper = UserUiMapper(resourceProvider)

    @Nested
    inner class MapToDisplayName {

        @Test
        fun `returns displayName when present and non-blank`() {
            val user = User(userId = "user-1", email = "test@example.com", displayName = "Alice")
            val result = mapper.mapToDisplayName(user)
            assertEquals("Alice", result)
        }

        @Test
        fun `falls back to email when displayName is null`() {
            val user = User(userId = "user-1", email = "test@example.com", displayName = null)
            val result = mapper.mapToDisplayName(user)
            assertEquals("test@example.com", result)
        }

        @Test
        fun `falls back to email when displayName is blank`() {
            val user = User(userId = "user-1", email = "test@example.com", displayName = "   ")
            val result = mapper.mapToDisplayName(user)
            assertEquals("test@example.com", result)
        }

        @Test
        fun `falls back to userId when user profile is null`() {
            val result = mapper.mapToDisplayName(user = null, fallbackUserId = "user-1")
            assertEquals("user-1", result)
        }

        @Test
        fun `returns localized fallback string when user profile is null and userId starts with pending_`() {
            val result = mapper.mapToDisplayName(user = null, fallbackUserId = "pending_user-1")
            assertEquals("Pending member", result)
        }

        @Test
        fun `falls back to userId when user profile is null and youLabel is blank`() {
            val result = mapper.mapToDisplayName(
                user = null,
                fallbackUserId = "user-1",
                currentUserId = "user-1",
                youLabel = ""
            )
            assertEquals("user-1", result)
        }

        @Test
        fun `returns youLabel when user is current user`() {
            val user = User(userId = "user-1", email = "test@example.com", displayName = "Alice")
            val result = mapper.mapToDisplayName(user, currentUserId = "user-1", youLabel = "You")
            assertEquals("You", result)
        }

        @Test
        fun `returns youLabel when user is null but fallbackUserId is current user`() {
            val result = mapper.mapToDisplayName(
                user = null,
                fallbackUserId = "user-1",
                currentUserId = "user-1",
                youLabel = "You"
            )
            assertEquals("You", result)
        }
    }

    @Nested
    inner class ToMemberOptions {

        @Test
        fun `maps list of member IDs and profiles correctly`() {
            val profiles = mapOf(
                "user-1" to User(userId = "user-1", email = "alice@example.com", displayName = "Alice"),
                "user-2" to User(userId = "user-2", email = "bob@example.com", displayName = ""),
                "user-3" to User(userId = "user-3", email = "charlie@example.com", displayName = null)
            )

            val result = mapper.toMemberOptions(
                memberIds = listOf("user-1", "user-2", "user-3", "user-unknown"),
                memberProfiles = profiles,
                currentUserId = "user-1"
            )

            assertEquals(4, result.size)

            // user-1
            assertEquals("user-1", result[0].userId)
            assertEquals("Alice", result[0].displayName)
            assertTrue(result[0].isCurrentUser)

            // user-2 (blank display name, falls back to email)
            assertEquals("user-2", result[1].userId)
            assertEquals("bob@example.com", result[1].displayName)
            assertFalse(result[1].isCurrentUser)

            // user-3 (null display name, falls back to email)
            assertEquals("user-3", result[2].userId)
            assertEquals("charlie@example.com", result[2].displayName)
            assertFalse(result[2].isCurrentUser)

            // user-unknown (null profile, falls back to userId)
            assertEquals("user-unknown", result[3].userId)
            assertEquals("user-unknown", result[3].displayName)
            assertFalse(result[3].isCurrentUser)
        }
    }
}
