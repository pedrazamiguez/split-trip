package es.pedrazamiguez.splittrip.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DisplayNameResolverTest {

    @Nested
    inner class NullOrMissingUser {

        @Test
        fun `returns empty string when userId is null`() {
            val result = DisplayNameResolver.resolve(
                userId = null,
                currentUserId = "user-1",
                youLabel = "You",
                displayName = "Alice"
            )

            assertEquals("", result)
        }

        @Test
        fun `returns userId as last resort when display name and email are both blank`() {
            val result = DisplayNameResolver.resolve(
                userId = "user-unknown",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = null,
                email = ""
            )

            assertEquals("user-unknown", result)
        }

        @Test
        fun `returns userId as last resort when display name is blank and email is blank`() {
            val result = DisplayNameResolver.resolve(
                userId = "user-unknown",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = "   ",
                email = "  "
            )

            assertEquals("user-unknown", result)
        }
    }

    @Nested
    inner class CurrentUser {

        @Test
        fun `returns youLabel when userId matches currentUserId`() {
            val result = DisplayNameResolver.resolve(
                userId = "user-1",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = "Alice",
                email = "alice@example.com"
            )

            assertEquals("You", result)
        }

        @Test
        fun `returns youLabel in Spanish locale form when provided`() {
            val result = DisplayNameResolver.resolve(
                userId = "user-1",
                currentUserId = "user-1",
                youLabel = "tú",
                displayName = "Alice"
            )

            assertEquals("tú", result)
        }
    }

    @Nested
    inner class OtherUser {

        @Test
        fun `returns displayName when present and not current user`() {
            val result = DisplayNameResolver.resolve(
                userId = "user-2",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = "Bob",
                email = "bob@example.com"
            )

            assertEquals("Bob", result)
        }

        @Test
        fun `returns email when displayName is null`() {
            val result = DisplayNameResolver.resolve(
                userId = "user-2",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = null,
                email = "bob@example.com"
            )

            assertEquals("bob@example.com", result)
        }

        @Test
        fun `returns email when displayName is blank`() {
            val result = DisplayNameResolver.resolve(
                userId = "user-2",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = "  ",
                email = "bob@example.com"
            )

            assertEquals("bob@example.com", result)
        }

        @Test
        fun `returns userId when both displayName and email are null or blank`() {
            val result = DisplayNameResolver.resolve(
                userId = "user-2",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = null,
                email = ""
            )

            assertEquals("user-2", result)
        }

        @Test
        fun `does not return youLabel for non-current user even when currentUserId is null`() {
            val result = DisplayNameResolver.resolve(
                userId = "user-2",
                currentUserId = null,
                youLabel = "You",
                displayName = "Charlie"
            )

            assertEquals("Charlie", result)
        }
    }
}
