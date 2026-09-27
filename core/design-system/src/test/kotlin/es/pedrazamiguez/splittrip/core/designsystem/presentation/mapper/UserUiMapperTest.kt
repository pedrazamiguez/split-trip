package es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.enums.GrammaticalGenderEnum
import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.domain.model.User
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UserUiMapperTest {

    private val resourceProvider = mockk<ResourceProvider> {
        every { getString(R.string.user_pending_fallback) } returns "Pending member"
        every { getString(R.string.self_identification_nominative) } returns "You"
        every { getString(R.string.self_identification_possessive_pronoun_feminine) } returns "tuya"
        every { getString(R.string.self_identification_possessive_pronoun_masculine) } returns "tuyo"
    }
    private val localeProvider = mockk<LocaleProvider> {
        every { getCurrentLocale() } returns Locale.ENGLISH
    }
    private val mapper = UserUiMapper(resourceProvider, localeProvider)

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
    inner class MapToSelfIdentification {

        @Test
        fun `returns correct possessive pronoun for feminine gender`() {
            val result = mapper.mapToSelfIdentification(
                context = SelfIdentificationContextEnum.POSSESSIVE_PRONOUN,
                gender = GrammaticalGenderEnum.FEMININE
            )
            assertEquals("tuya", result)
        }

        @Test
        fun `returns correct possessive pronoun for masculine gender`() {
            val result = mapper.mapToSelfIdentification(
                context = SelfIdentificationContextEnum.POSSESSIVE_PRONOUN,
                gender = GrammaticalGenderEnum.MASCULINE
            )
            assertEquals("tuyo", result)
        }
    }

    @Nested
    inner class ToMemberOptions {

        @Test
        fun `resolves nominative pronoun for current user and places them at index 0`() {
            val profiles = mapOf(
                "user-1" to User(userId = "user-1", email = "alice@example.com", displayName = "Alice"),
                "user-2" to User(userId = "user-2", email = "bob@example.com", displayName = "Bob"),
                "user-3" to User(userId = "user-3", email = "charlie@example.com", displayName = "Charlie")
            )

            val result = mapper.toMemberOptions(
                memberIds = listOf("user-1", "user-2", "user-3"),
                memberProfiles = profiles,
                currentUserId = "user-3"
            )

            assertEquals(3, result.size)

            // user-3 is current user -> pinned to index 0 with "You"
            assertEquals("user-3", result[0].userId)
            assertEquals("You", result[0].displayName)
            assertTrue(result[0].isCurrentUser)

            // remaining sorted alphabetically
            assertEquals("user-1", result[1].userId)
            assertEquals("Alice", result[1].displayName)
            assertFalse(result[1].isCurrentUser)

            assertEquals("user-2", result[2].userId)
            assertEquals("Bob", result[2].displayName)
            assertFalse(result[2].isCurrentUser)
        }

        @Test
        fun `sorts purely alphabetically when currentUserId is null or not in member list`() {
            val profiles = mapOf(
                "user-1" to User(userId = "user-1", email = "alice@example.com", displayName = "Alice"),
                "user-2" to User(userId = "user-2", email = "bob@example.com", displayName = ""),
                "user-3" to User(userId = "user-3", email = "charlie@example.com", displayName = null)
            )

            val result = mapper.toMemberOptions(
                memberIds = listOf("user-3", "user-1", "user-2", "user-unknown"),
                memberProfiles = profiles,
                currentUserId = null
            )

            assertEquals(4, result.size)

            // Sorted alphabetically by resolved display name: Alice, bob@example.com, charlie@example.com, user-unknown
            assertEquals("user-1", result[0].userId)
            assertEquals("Alice", result[0].displayName)
            assertFalse(result[0].isCurrentUser)

            assertEquals("user-2", result[1].userId)
            assertEquals("bob@example.com", result[1].displayName)
            assertFalse(result[1].isCurrentUser)

            assertEquals("user-3", result[2].userId)
            assertEquals("charlie@example.com", result[2].displayName)
            assertFalse(result[2].isCurrentUser)

            assertEquals("user-unknown", result[3].userId)
            assertEquals("user-unknown", result[3].displayName)
            assertFalse(result[3].isCurrentUser)
        }
    }
}
