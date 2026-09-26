package es.pedrazamiguez.splittrip.features.group.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel.Companion.MAX_VISIBLE_AVATARS
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GroupUiMapperImplTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var userUiMapper: UserUiMapper
    private lateinit var mapper: GroupUiMapperImpl

    private val testLocale = Locale.US

    @BeforeEach
    fun setUp() {
        localeProvider = mockk {
            every { getCurrentLocale() } returns testLocale
        }
        resourceProvider = mockk()
        every { resourceProvider.getString(R.string.group_member_role_creator) } returns "Creator"
        every { resourceProvider.getString(R.string.group_member_role_member) } returns "Member"
        userUiMapper = UserUiMapper(mockk(relaxed = true))
        mapper = GroupUiMapperImpl(localeProvider, resourceProvider, userUiMapper)
    }

    @Nested
    inner class MembersCountMapping {

        @Test
        fun `maps group with zero members correctly`() {
            // Given
            val group = createGroup(members = emptyList())
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 0, 0) } returns "0 travelers"

            // When
            val result = mapper.toGroupUiModel(group)

            // Then
            assertEquals("0 travelers", result.membersCountText)
            verify { resourceProvider.getQuantityString(R.plurals.group_members_count, 0, 0) }
        }

        @Test
        fun `maps group with one member using singular form`() {
            // Given
            val group = createGroup(members = listOf("user-1"))
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 1, 1) } returns "1 traveler"

            // When
            val result = mapper.toGroupUiModel(group)

            // Then
            assertEquals("1 traveler", result.membersCountText)
            verify { resourceProvider.getQuantityString(R.plurals.group_members_count, 1, 1) }
        }

        @Test
        fun `maps group with multiple members using plural form`() {
            // Given
            val members = listOf("user-1", "user-2", "user-3")
            val group = createGroup(members = members)
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 3, 3) } returns "3 travelers"

            // When
            val result = mapper.toGroupUiModel(group)

            // Then
            assertEquals("3 travelers", result.membersCountText)
            verify { resourceProvider.getQuantityString(R.plurals.group_members_count, 3, 3) }
        }

        @Test
        fun `maps group with many members correctly`() {
            // Given
            val members = (1..25).map { "user-$it" }
            val group = createGroup(members = members)
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 25, 25) } returns "25 travelers"

            // When
            val result = mapper.toGroupUiModel(group)

            // Then
            assertEquals("25 travelers", result.membersCountText)
            verify { resourceProvider.getQuantityString(R.plurals.group_members_count, 25, 25) }
        }
    }

    @Nested
    inner class MembersCountWithSpanishLocale {

        @BeforeEach
        fun setUpSpanishLocale() {
            every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
        }

        @Test
        fun `maps group with one member using Spanish singular`() {
            // Given
            val group = createGroup(members = listOf("user-1"))
            every {
                resourceProvider.getQuantityString(R.plurals.group_members_count, 1, 1)
            } returns "1 viajero"

            // When
            val result = mapper.toGroupUiModel(group)

            // Then
            assertEquals("1 viajero", result.membersCountText)
        }

        @Test
        fun `maps group with multiple members using Spanish plural`() {
            // Given
            val group = createGroup(members = listOf("user-1", "user-2"))
            every {
                resourceProvider.getQuantityString(R.plurals.group_members_count, 2, 2)
            } returns "2 viajeros"

            // When
            val result = mapper.toGroupUiModel(group)

            // Then
            assertEquals("2 viajeros", result.membersCountText)
        }
    }

    @Nested
    inner class GroupListMapping {

        @Test
        fun `maps list of groups preserving member counts`() {
            // Given
            val groups = listOf(
                createGroup(id = "1", members = listOf("user-1")),
                createGroup(id = "2", members = listOf("user-1", "user-2", "user-3")),
                createGroup(id = "3", members = emptyList())
            )
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 1, 1) } returns "1 traveler"
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 3, 3) } returns "3 travelers"
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 0, 0) } returns "0 travelers"

            // When
            val result = mapper.toGroupUiModelList(groups)

            // Then
            assertEquals(3, result.size)
            assertEquals("1 traveler", result[0].membersCountText)
            assertEquals("3 travelers", result[1].membersCountText)
            assertEquals("0 travelers", result[2].membersCountText)
        }

        @Test
        fun `maps list of groups with member profiles enriching avatar urls`() {
            // Given
            val members = listOf("user-1", "user-2")
            val groups = listOf(
                createGroup(id = "1", members = members),
                createGroup(id = "2", members = listOf("user-1"))
            )
            val profiles = mapOf(
                "user-1" to createUser("user-1", "https://example.com/avatar1.jpg"),
                "user-2" to createUser("user-2", "https://example.com/avatar2.jpg")
            )
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 2, 2) } returns "2 travelers"
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 1, 1) } returns "1 traveler"

            // When — call the 2-arg overload directly (covers the Impl's toGroupUiModelList implementation)
            val result = mapper.toGroupUiModelList(groups, profiles)

            // Then
            assertEquals(2, result.size)
            assertEquals(2, result[0].memberAvatarUrls.size)
            assertEquals("https://example.com/avatar1.jpg", result[0].memberAvatarUrls[0])
            assertEquals("https://example.com/avatar2.jpg", result[0].memberAvatarUrls[1])
            assertEquals(1, result[1].memberAvatarUrls.size)
        }
    }

    @Nested
    inner class SyncStatusMapping {

        @Test
        fun `maps PENDING_SYNC status`() {
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 0, 0) } returns "0 travelers"
            val group = createGroup().copy(syncStatus = SyncStatus.PENDING_SYNC)
            val result = mapper.toGroupUiModel(group)
            assertEquals(SyncStatus.PENDING_SYNC, result.syncStatus)
        }

        @Test
        fun `maps SYNC_FAILED status`() {
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 0, 0) } returns "0 travelers"
            val group = createGroup().copy(syncStatus = SyncStatus.SYNC_FAILED)
            val result = mapper.toGroupUiModel(group)
            assertEquals(SyncStatus.SYNC_FAILED, result.syncStatus)
        }

        @Test
        fun `default maps to SYNCED`() {
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 0, 0) } returns "0 travelers"
            val group = createGroup()
            val result = mapper.toGroupUiModel(group)
            assertEquals(SyncStatus.SYNCED, result.syncStatus)
        }
    }

    @Nested
    inner class CurrencyMapping {

        @Test
        fun `maps known currency with localized name from resourceProvider`() {
            // Given
            val currency = createCurrency(code = "EUR", defaultName = "Euro")
            every { resourceProvider.getString(DesignR.string.currency_name_eur) } returns "Euro"

            // When
            val result = mapper.toCurrencyUiModel(currency)

            // Then
            assertEquals("EUR", result.code)
            assertEquals("Euro", result.defaultName)
            assertEquals("Euro", result.localizedName)
            verify { resourceProvider.getString(DesignR.string.currency_name_eur) }
        }

        @Test
        fun `maps unknown currency falling back to defaultName`() {
            // Given
            val currency = createCurrency(code = "XAF", defaultName = "CFA Franc")

            // When
            val result = mapper.toCurrencyUiModel(currency)

            // Then
            assertEquals("XAF", result.code)
            assertEquals("CFA Franc", result.defaultName)
            assertEquals("CFA Franc", result.localizedName)
        }

        @Test
        fun `maps currency list preserving localized names`() {
            // Given
            val currencies = listOf(
                createCurrency(code = "EUR", defaultName = "Euro"),
                createCurrency(code = "XAF", defaultName = "CFA Franc")
            )
            every { resourceProvider.getString(DesignR.string.currency_name_eur) } returns "Euro"

            // When
            val result = mapper.toCurrencyUiModels(currencies)

            // Then
            assertEquals(2, result.size)
            assertEquals("Euro", result[0].localizedName)
            assertEquals("CFA Franc", result[1].localizedName)
            verify(exactly = 1) { resourceProvider.getString(DesignR.string.currency_name_eur) }
        }
    }

    @Nested
    inner class CurrencyMappingWithSpanishLocale {

        @BeforeEach
        fun setUpSpanishLocale() {
            every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
        }

        @Test
        fun `maps GBP with Spanish localized name`() {
            // Given
            val currency = createCurrency(code = "GBP", defaultName = "British Pound Sterling")
            every { resourceProvider.getString(DesignR.string.currency_name_gbp) } returns "Libra esterlina"

            // When
            val result = mapper.toCurrencyUiModel(currency)

            // Then
            assertEquals("GBP", result.code)
            assertEquals("British Pound Sterling", result.defaultName)
            assertEquals("Libra esterlina", result.localizedName)
            verify { resourceProvider.getString(DesignR.string.currency_name_gbp) }
        }

        @Test
        fun `maps USD with Spanish localized name`() {
            // Given
            val currency = createCurrency(code = "USD", defaultName = "United States Dollar")
            every { resourceProvider.getString(DesignR.string.currency_name_usd) } returns "Dólar estadounidense"

            // When
            val result = mapper.toCurrencyUiModel(currency)

            // Then
            assertEquals("USD", result.code)
            assertEquals("United States Dollar", result.defaultName)
            assertEquals("Dólar estadounidense", result.localizedName)
            verify { resourceProvider.getString(DesignR.string.currency_name_usd) }
        }
    }

    private fun createCurrency(
        code: String = "EUR",
        symbol: String = "€",
        defaultName: String = "Euro",
        decimalDigits: Int = 2
    ) = Currency(code = code, symbol = symbol, defaultName = defaultName, decimalDigits = decimalDigits)

    private fun createGroup(
        id: String = "test-id",
        name: String = "Test Group",
        description: String = "Test Description",
        currency: String = "EUR",
        extraCurrencies: List<String> = emptyList(),
        members: List<String> = emptyList(),
        mainImagePath: String? = null,
        createdAt: LocalDateTime? = LocalDateTime.of(2024, 1, 15, 12, 0)
    ) = Group(
        id = id,
        name = name,
        description = description,
        currency = currency,
        extraCurrencies = extraCurrencies,
        members = members,
        mainImagePath = mainImagePath,
        createdAt = createdAt
    )

    private fun createUser(userId: String, profileImagePath: String?) = User(
        userId = userId,
        email = "$userId@example.com",
        displayName = userId,
        profileImagePath = profileImagePath
    )

    @Nested
    inner class ExtraCurrenciesMapping {

        @Test
        fun `maps empty extraCurrencies to empty list with null overflow`() {
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 0, 0) } returns "0 travelers"
            val group = createGroup(extraCurrencies = emptyList())

            val result = mapper.toGroupUiModel(group)

            assertTrue(result.extraCurrencies.isEmpty())
            assertNull(result.extraCurrenciesOverflowText)
        }

        @Test
        fun `maps up to 3 extraCurrencies without overflow`() {
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 0, 0) } returns "0 travelers"
            val extra = listOf("USD", "GBP", "JPY")
            val group = createGroup(extraCurrencies = extra)

            val result = mapper.toGroupUiModel(group)

            assertEquals(listOf("USD", "GBP", "JPY"), result.extraCurrencies)
            assertNull(result.extraCurrenciesOverflowText)
        }

        @Test
        fun `caps extraCurrencies when exceeding 3 and formats overflow text`() {
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 0, 0) } returns "0 travelers"
            every { resourceProvider.getString(R.string.group_extra_currencies_overflow, 3) } returns "+3"
            val extra = listOf("USD", "GBP", "JPY", "THB", "CAD")
            val group = createGroup(extraCurrencies = extra)

            val result = mapper.toGroupUiModel(group)

            assertEquals(listOf("USD", "GBP"), result.extraCurrencies)
            assertEquals("+3", result.extraCurrenciesOverflowText)
            verify { resourceProvider.getString(R.string.group_extra_currencies_overflow, 3) }
        }
    }

    @Nested
    inner class ImageUrlMapping {

        @Test
        fun `maps mainImagePath to imageUrl`() {
            // Given
            val group = createGroup(mainImagePath = "https://example.com/image.jpg", members = emptyList())
            every {
                resourceProvider.getQuantityString(R.plurals.group_members_count, 0, 0)
            } returns "0 travelers"

            // When
            val result = mapper.toGroupUiModel(group)

            // Then
            assertEquals("https://example.com/image.jpg", result.imageUrl)
        }

        @Test
        fun `maps null mainImagePath to null imageUrl`() {
            // Given
            val group = createGroup(mainImagePath = null, members = emptyList())
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 0, 0) } returns "0 travelers"

            // When
            val result = mapper.toGroupUiModel(group)

            // Then
            assertNull(result.imageUrl)
        }
    }

    @Nested
    inner class MemberAvatarMapping {

        @Test
        fun `extracts avatar URLs for members with profiles`() {
            // Given
            val members = listOf("user-1", "user-2")
            val group = createGroup(members = members)
            val profiles = mapOf(
                "user-1" to createUser("user-1", "https://example.com/avatar1.jpg"),
                "user-2" to createUser("user-2", "https://example.com/avatar2.jpg")
            )
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 2, 2) } returns "2 travelers"

            // When
            val result = mapper.toGroupUiModel(group, profiles)

            // Then
            assertEquals(2, result.memberAvatarUrls.size)
            assertEquals("https://example.com/avatar1.jpg", result.memberAvatarUrls[0])
            assertEquals("https://example.com/avatar2.jpg", result.memberAvatarUrls[1])
            assertEquals(0, result.memberOverflowCount)
        }

        @Test
        fun `skips members without profileImagePath`() {
            // Given
            val members = listOf("user-1", "user-2")
            val group = createGroup(members = members)
            val profiles = mapOf(
                "user-1" to createUser("user-1", "https://example.com/avatar1.jpg"),
                "user-2" to createUser("user-2", null)
            )
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 2, 2) } returns "2 travelers"

            // When
            val result = mapper.toGroupUiModel(group, profiles)

            // Then
            assertEquals(1, result.memberAvatarUrls.size)
            assertEquals("https://example.com/avatar1.jpg", result.memberAvatarUrls[0])
        }

        @Test
        fun `limits avatar URLs to MAX_VISIBLE_AVATARS`() {
            // Given — more members than the avatar limit
            val memberCount = MAX_VISIBLE_AVATARS + 2
            val members = (1..memberCount).map { "user-$it" }
            val group = createGroup(members = members)
            val profiles = members.associate { userId ->
                userId to createUser(userId, "https://example.com/$userId.jpg")
            }
            every {
                resourceProvider.getQuantityString(R.plurals.group_members_count, memberCount, memberCount)
            } returns "$memberCount travelers"

            // When
            val result = mapper.toGroupUiModel(group, profiles)

            // Then
            assertEquals(MAX_VISIBLE_AVATARS, result.memberAvatarUrls.size)
            assertEquals(2, result.memberOverflowCount)
        }

        @Test
        fun `overflow count is zero when members within avatar limit`() {
            // Given
            val members = listOf("user-1", "user-2")
            val group = createGroup(members = members)
            val profiles = members.associate { userId ->
                userId to createUser(userId, "https://example.com/$userId.jpg")
            }
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 2, 2) } returns "2 travelers"

            // When
            val result = mapper.toGroupUiModel(group, profiles)

            // Then
            assertEquals(0, result.memberOverflowCount)
        }

        @Test
        fun `overflow count accounts for total members not just those with avatars`() {
            // Given — 5 members but only 2 have avatar URLs
            val members = (1..5).map { "user-$it" }
            val group = createGroup(members = members)
            val profiles = mapOf(
                "user-1" to createUser("user-1", "https://example.com/1.jpg"),
                "user-2" to createUser("user-2", "https://example.com/2.jpg"),
                "user-3" to createUser("user-3", null),
                "user-4" to createUser("user-4", null),
                "user-5" to createUser("user-5", null)
            )
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 5, 5) } returns "5 travelers"

            // When
            val result = mapper.toGroupUiModel(group, profiles)

            // Then
            assertEquals(2, result.memberAvatarUrls.size)
            assertEquals(3, result.memberOverflowCount) // max(0, memberCount(5) - avatarUrls.size(2)) = 3
        }

        @Test
        fun `empty profiles map results in no avatars and no overflow for small groups`() {
            // Given
            val members = listOf("user-1", "user-2")
            val group = createGroup(members = members)
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 2, 2) } returns "2 travelers"

            // When
            val result = mapper.toGroupUiModel(group, emptyMap())

            // Then
            assertTrue(result.memberAvatarUrls.isEmpty())
            assertEquals(0, result.memberOverflowCount)
        }
    }

    @Nested
    inner class GroupMembersMapping {

        @Test
        fun `maps group members to GroupMemberUiModel with roles and avatars`() {
            // Given
            val members = listOf("creator-id", "member-id")
            val group = createGroup(
                id = "group-1",
                members = members
            ).copy(createdBy = "creator-id")
            val profiles = mapOf(
                "creator-id" to createUser(
                    userId = "creator-id",
                    profileImagePath = "https://example.com/creator.jpg"
                ).copy(displayName = "Creator"),
                "member-id" to createUser("member-id", null).copy(displayName = "Member")
            )
            every { resourceProvider.getQuantityString(R.plurals.group_members_count, 2, 2) } returns "2 travelers"

            // When
            val result = mapper.toGroupUiModel(group, profiles)

            // Then
            assertEquals(2, result.members.size)
            val creatorMember = result.members[0]
            assertEquals("creator-id", creatorMember.userId)
            assertEquals("Creator", creatorMember.displayName)
            assertEquals("https://example.com/creator.jpg", creatorMember.avatarUrl)
            assertTrue(creatorMember.isCreator)
            assertEquals("Creator", creatorMember.roleBadgeText)

            val regularMember = result.members[1]
            assertEquals("member-id", regularMember.userId)
            assertEquals("Member", regularMember.displayName)
            assertNull(regularMember.avatarUrl)
            assertFalse(regularMember.isCreator)
            assertEquals("Member", regularMember.roleBadgeText)
        }
    }
}
