package es.pedrazamiguez.splittrip.domain.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class GroupRoleTest {

    @Nested
    inner class FromString {

        @ParameterizedTest
        @CsvSource(
            "ADMIN, ADMIN",
            "MEMBER, MEMBER",
            "GUEST, GUEST",
            "admin, ADMIN",
            "member, MEMBER",
            "Guest, GUEST",
            "ADMIN, ADMIN"
        )
        fun `parses known roles case-insensitively`(input: String, expected: GroupRole) {
            assertEquals(expected, GroupRole.fromString(input))
        }

        @Test
        fun `throws IllegalArgumentException for unknown role string`() {
            assertThrows(IllegalArgumentException::class.java) {
                GroupRole.fromString("SUPERUSER")
            }
        }

        @Test
        fun `throws with descriptive message containing the unknown role`() {
            val ex = assertThrows(IllegalArgumentException::class.java) {
                GroupRole.fromString("OWNER")
            }
            assertEquals("Unknown role: OWNER", ex.message)
        }
    }
}
