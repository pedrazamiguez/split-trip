package es.pedrazamiguez.splittrip.features.group.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.group.R
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GroupUiMapperImplMemberOrderingTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var userUiMapper: UserUiMapper
    private lateinit var mapper: GroupUiMapperImpl

    @BeforeEach
    fun setUp() {
        localeProvider = mockk {
            every { getCurrentLocale() } returns Locale.US
        }
        resourceProvider = mockk(relaxed = true) {
            every { getString(DesignR.string.self_identification_nominative) } returns "You"
            every { getString(R.string.group_member_role_creator) } returns "Creator"
            every { getString(R.string.group_member_role_member) } returns "Member"
            every { getQuantityString(R.plurals.group_members_count, any(), any()) } returns "members"
        }
        userUiMapper = UserUiMapper(resourceProvider, localeProvider)
        mapper = GroupUiMapperImpl(localeProvider, resourceProvider, userUiMapper)
    }

    @Nested
    inner class MemberOrdering {

        @Test
        fun `toGroupUiModel pins current user at index 0 of members with nominative displayName`() {
            val group = Group(
                id = "group-1",
                name = "Test Group",
                members = listOf("user-1", "user-2", "user-3"),
                createdBy = "user-1"
            )
            val memberProfiles = mapOf(
                "user-1" to User(
                    userId = "user-1",
                    email = "alice@test.com",
                    displayName = "Alice",
                    profileImagePath = "https://example.com/alice.jpg"
                ),
                "user-2" to User(
                    userId = "user-2",
                    email = "bob@test.com",
                    displayName = "Bob",
                    profileImagePath = "https://example.com/bob.jpg"
                ),
                "user-3" to User(
                    userId = "user-3",
                    email = "charlie@test.com",
                    displayName = "Charlie",
                    profileImagePath = "https://example.com/charlie.jpg"
                )
            )

            val result = mapper.toGroupUiModel(group, memberProfiles, currentUserId = "user-2")

            assertEquals(3, result.members.size)

            // Current user pinned to index 0
            assertEquals("user-2", result.members[0].userId)
            assertEquals("You", result.members[0].displayName)

            // Remaining members sorted alphabetically: Alice, Charlie
            assertEquals("user-1", result.members[1].userId)
            assertEquals("Alice", result.members[1].displayName)

            assertEquals("user-3", result.members[2].userId)
            assertEquals("Charlie", result.members[2].displayName)

            // Avatar URLs start with current user's avatar
            assertEquals(3, result.memberAvatarUrls.size)
            assertEquals("https://example.com/bob.jpg", result.memberAvatarUrls[0])
            assertEquals("https://example.com/alice.jpg", result.memberAvatarUrls[1])
            assertEquals("https://example.com/charlie.jpg", result.memberAvatarUrls[2])
        }

        @Test
        fun `toGroupUiModel sorts members alphabetically when currentUserId is null`() {
            val group = Group(
                id = "group-1",
                name = "Test Group",
                members = listOf("user-3", "user-1", "user-2"),
                createdBy = "user-1"
            )
            val memberProfiles = mapOf(
                "user-1" to User(userId = "user-1", email = "alice@test.com", displayName = "Alice"),
                "user-2" to User(userId = "user-2", email = "bob@test.com", displayName = "Bob"),
                "user-3" to User(userId = "user-3", email = "charlie@test.com", displayName = "Charlie")
            )

            val result = mapper.toGroupUiModel(group, memberProfiles, currentUserId = null)

            assertEquals(3, result.members.size)
            assertEquals("user-1", result.members[0].userId)
            assertEquals("Alice", result.members[0].displayName)
            assertEquals("user-2", result.members[1].userId)
            assertEquals("Bob", result.members[1].displayName)
            assertEquals("user-3", result.members[2].userId)
            assertEquals("Charlie", result.members[2].displayName)
        }
    }
}
