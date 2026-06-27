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

    @Nested
    inner class PendingFallback {

        @Test
        fun `returns pendingLabel when userId starts with pending_ and display name and email are blank`() {
            val result = DisplayNameResolver.resolve(
                userId = "pending_user-123",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = null,
                email = "",
                pendingLabel = "Pending Member"
            )

            assertEquals("Pending Member", result)
        }

        @Test
        fun `returns userId when userId starts with pending_ but pendingLabel is null or blank`() {
            val resultNull = DisplayNameResolver.resolve(
                userId = "pending_user-123",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = null,
                email = "",
                pendingLabel = null
            )
            val resultBlank = DisplayNameResolver.resolve(
                userId = "pending_user-123",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = null,
                email = "",
                pendingLabel = "   "
            )

            assertEquals("pending_user-123", resultNull)
            assertEquals("pending_user-123", resultBlank)
        }

        @Test
        fun `returns displayName or email instead of pendingLabel if present even if pending user`() {
            val resultDisplayName = DisplayNameResolver.resolve(
                userId = "pending_user-123",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = "Alice",
                email = "",
                pendingLabel = "Pending Member"
            )
            val resultEmail = DisplayNameResolver.resolve(
                userId = "pending_user-123",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = "",
                email = "alice@example.com",
                pendingLabel = "Pending Member"
            )

            assertEquals("Alice", resultDisplayName)
            assertEquals("alice@example.com", resultEmail)
        }

        @Test
        fun `returns userId when userId does not start with pending_ even if pendingLabel is provided`() {
            val result = DisplayNameResolver.resolve(
                userId = "user-123",
                currentUserId = "user-1",
                youLabel = "You",
                displayName = null,
                email = "",
                pendingLabel = "Pending Member"
            )

            assertEquals("user-123", result)
        }
    }
}
