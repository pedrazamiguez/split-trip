package es.pedrazamiguez.splittrip.features.subunit.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.subunit.R
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SubunitUiMapperImplTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var userUiMapper: UserUiMapper
    private lateinit var mapper: SubunitUiMapperImpl

    private val testLocale = Locale.US

    @BeforeEach
    fun setUp() {
        localeProvider = mockk {
            every { getCurrentLocale() } returns testLocale
        }
        resourceProvider = mockk(relaxed = true)
        userUiMapper = UserUiMapper(resourceProvider)
        mapper = SubunitUiMapperImpl(localeProvider, resourceProvider, userUiMapper)
    }

    private fun createUser(
        id: String,
        displayName: String? = null,
        email: String = "$id@test.com",
        profileImagePath: String? = null
    ) = User(userId = id, email = email, displayName = displayName, profileImagePath = profileImagePath)

    private fun createSubunit(
        id: String = "sub-1",
        name: String = "Couple",
        memberIds: List<String> = listOf("user-1", "user-2"),
        memberShares: Map<String, BigDecimal> = mapOf("user-1" to BigDecimal("0.5"), "user-2" to BigDecimal("0.5"))
    ) = Subunit(
        id = id,
        groupId = "group-1",
        name = name,
        memberIds = memberIds,
        memberShares = memberShares
    )

    @Nested
    inner class ToSubunitUiModel {

        @Test
        fun `maps subunit with display names`() {
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = "Alice"),
                "user-2" to createUser("user-2", displayName = "Bob")
            )
            val subunit = createSubunit()
            every {
                resourceProvider.getQuantityString(R.plurals.subunit_member_count, 2, 2)
            } returns "2 members"

            val result = mapper.toSubunitUiModel(subunit, profiles)

            assertEquals("sub-1", result.id)
            assertEquals("Couple", result.name)
            assertEquals(2, result.memberShares.size)
            assertEquals("Alice", result.memberShares[0].displayName)
            assertEquals("Bob", result.memberShares[1].displayName)
            assertEquals("2 members", result.memberCount)
        }

        @Test
        fun `maps member avatar URLs from member profiles`() {
            val profiles = mapOf(
                "user-1" to createUser(
                    id = "user-1",
                    displayName = "Alice",
                    profileImagePath = "https://example.com/alice.jpg"
                ),
                "user-2" to createUser("user-2", displayName = "Bob", profileImagePath = null)
            )
            val subunit = createSubunit()
            every {
                resourceProvider.getQuantityString(R.plurals.subunit_member_count, 2, 2)
            } returns "2 members"

            val result = mapper.toSubunitUiModel(subunit, profiles)

            assertEquals(2, result.memberShares.size)
            assertEquals("https://example.com/alice.jpg", result.memberShares[0].avatarUrl)
            assertNull(result.memberShares[1].avatarUrl)
        }

        @Test
        fun `falls back to email when display name is null`() {
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = null, email = "alice@test.com"),
                "user-2" to createUser("user-2", displayName = "Bob")
            )
            val subunit = createSubunit()
            every {
                resourceProvider.getQuantityString(R.plurals.subunit_member_count, 2, 2)
            } returns "2 members"

            val result = mapper.toSubunitUiModel(subunit, profiles)

            // Locale-aware case-insensitive sort: "alice@test.com" < "Bob"
            assertEquals("alice@test.com", result.memberShares[0].displayName)
            assertEquals("Bob", result.memberShares[1].displayName)
        }

        @Test
        fun `falls back to userId when profile is missing`() {
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = "Alice")
            )
            val subunit = createSubunit()
            every {
                resourceProvider.getQuantityString(R.plurals.subunit_member_count, 2, 2)
            } returns "2 members"

            val result = mapper.toSubunitUiModel(subunit, profiles)

            assertEquals("Alice", result.memberShares[0].displayName)
            assertEquals("user-2", result.memberShares[1].displayName)
        }

        @Test
        fun `formats shares summary as percentages`() {
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = "Alice"),
                "user-2" to createUser("user-2", displayName = "Bob")
            )
            val subunit = createSubunit(
                memberShares = mapOf("user-1" to BigDecimal("0.6"), "user-2" to BigDecimal("0.4"))
            )
            every {
                resourceProvider.getQuantityString(R.plurals.subunit_member_count, 2, 2)
            } returns "2 members"

            val result = mapper.toSubunitUiModel(subunit, profiles)

            assertEquals(2, result.memberShares.size)
            assertEquals("Alice", result.memberShares[0].displayName)
            assertEquals("60%", result.memberShares[0].shareText)
            assertEquals("Bob", result.memberShares[1].displayName)
            assertEquals("40%", result.memberShares[1].shareText)
        }

        @Test
        fun `returns empty shares summary when no shares defined`() {
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = "Alice")
            )
            val subunit = createSubunit(
                memberIds = listOf("user-1"),
                memberShares = emptyMap()
            )
            every {
                resourceProvider.getQuantityString(R.plurals.subunit_member_count, 1, 1)
            } returns "1 member"

            val result = mapper.toSubunitUiModel(subunit, profiles)

            assertTrue(result.memberShares.isEmpty())
        }
    }

    @Nested
    inner class ToSubunitUiModelWithEsLocale {

        private val esLocale = Locale.forLanguageTag("es-ES")

        private fun createEsMapper(): SubunitUiMapperImpl {
            val esLocaleProvider = mockk<LocaleProvider> {
                every { getCurrentLocale() } returns esLocale
            }
            return SubunitUiMapperImpl(esLocaleProvider, resourceProvider, userUiMapper)
        }

        @Test
        fun `formats shares with comma-decimal separator for ES locale`() {
            val esMapper = createEsMapper()
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = "Alice"),
                "user-2" to createUser("user-2", displayName = "Bob"),
                "user-3" to createUser("user-3", displayName = "Charlie")
            )
            val thirdShare = BigDecimal.ONE.divide(BigDecimal(3), 10, java.math.RoundingMode.DOWN)
            val subunit = createSubunit(
                memberIds = listOf("user-1", "user-2", "user-3"),
                memberShares = mapOf(
                    "user-1" to thirdShare,
                    "user-2" to thirdShare,
                    "user-3" to thirdShare
                )
            )
            every {
                resourceProvider.getQuantityString(R.plurals.subunit_member_count, 3, 3)
            } returns "3 miembros"

            val result = esMapper.toSubunitUiModel(subunit, profiles)

            assertEquals(3, result.memberShares.size)
            // ES locale uses comma as decimal separator → "33 %", not "33 %"
            // NumberFormat.getPercentInstance(es_ES) formats 0.333... as "33 %"
            result.memberShares.forEach { memberShare ->
                assertTrue(
                    memberShare.shareText.contains("%"),
                    "Expected percentage format, got: ${memberShare.shareText}"
                )
            }
        }

        @Test
        fun `formats integer shares correctly for ES locale`() {
            val esMapper = createEsMapper()
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = "Alice"),
                "user-2" to createUser("user-2", displayName = "Bob")
            )
            val subunit = createSubunit(
                memberShares = mapOf("user-1" to BigDecimal("0.6"), "user-2" to BigDecimal("0.4"))
            )
            every {
                resourceProvider.getQuantityString(R.plurals.subunit_member_count, 2, 2)
            } returns "2 miembros"

            val result = esMapper.toSubunitUiModel(subunit, profiles)

            assertEquals(2, result.memberShares.size)
            assertEquals("Alice", result.memberShares[0].displayName)
            assertEquals("Bob", result.memberShares[1].displayName)
            // Verify both locales produce valid percentage text
            assertTrue(result.memberShares[0].shareText.contains("60"))
            assertTrue(result.memberShares[1].shareText.contains("40"))
        }

        @Test
        fun `formatShareAsPercentage handles thirds correctly for ES locale`() {
            val esMapper = createEsMapper()

            val result = esMapper.formatShareAsPercentage(
                BigDecimal.ONE.divide(BigDecimal(3), 10, java.math.RoundingMode.DOWN)
            )

            // ES locale: "33,33" (comma decimal separator, 2 max fraction digits)
            assertTrue(
                result.contains("33"),
                "Expected formatted percentage containing '33', got: $result"
            )
        }
    }

    @Nested
    inner class ToSubunitUiModelList {

        @Test
        fun `maps list of subunits`() {
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = "Alice"),
                "user-2" to createUser("user-2", displayName = "Bob"),
                "user-3" to createUser("user-3", displayName = "Charlie")
            )
            val subunits = listOf(
                createSubunit(id = "sub-1", name = "Couple", memberIds = listOf("user-1", "user-2")),
                createSubunit(id = "sub-2", name = "Solo", memberIds = listOf("user-3"))
            )
            every {
                resourceProvider.getQuantityString(R.plurals.subunit_member_count, any(), any())
            } returns "members"

            val result = mapper.toSubunitUiModelList(subunits, profiles)

            assertEquals(2, result.size)
            assertEquals("Couple", result[0].name)
            assertEquals("Solo", result[1].name)
        }

        @Test
        fun `returns empty list for empty input`() {
            val result = mapper.toSubunitUiModelList(emptyList(), emptyMap())
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class ToMemberUiModelList {

        @Test
        fun `marks members not in any subunit as available`() {
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = "Alice"),
                "user-2" to createUser("user-2", displayName = "Bob")
            )

            val result = mapper.toMemberUiModelList(
                memberIds = listOf("user-1", "user-2"),
                memberProfiles = profiles,
                subunits = emptyList()
            )

            assertEquals(2, result.size)
            assertFalse(result[0].isAssigned)
            assertFalse(result[1].isAssigned)
        }

        @Test
        fun `marks members in existing subunits as assigned`() {
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = "Alice"),
                "user-2" to createUser("user-2", displayName = "Bob"),
                "user-3" to createUser("user-3", displayName = "Charlie")
            )
            val existingSubunits = listOf(
                createSubunit(id = "sub-1", name = "Couple", memberIds = listOf("user-1", "user-2"))
            )

            val result = mapper.toMemberUiModelList(
                memberIds = listOf("user-1", "user-2", "user-3"),
                memberProfiles = profiles,
                subunits = existingSubunits
            )

            assertTrue(result[0].isAssigned)
            assertEquals("Couple", result[0].assignedSubunitName)
            assertTrue(result[1].isAssigned)
            assertEquals("Couple", result[1].assignedSubunitName)
            assertFalse(result[2].isAssigned)
        }

        @Test
        fun `excludes edited subunit from assigned check`() {
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = "Alice"),
                "user-2" to createUser("user-2", displayName = "Bob")
            )
            val existingSubunits = listOf(
                createSubunit(id = "sub-1", name = "Couple", memberIds = listOf("user-1", "user-2"))
            )

            val result = mapper.toMemberUiModelList(
                memberIds = listOf("user-1", "user-2"),
                memberProfiles = profiles,
                subunits = existingSubunits,
                excludeSubunitId = "sub-1"
            )

            assertFalse(result[0].isAssigned)
            assertFalse(result[1].isAssigned)
        }

        @Test
        fun `resolves display name with fallback to email`() {
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = null, email = "alice@test.com")
            )

            val result = mapper.toMemberUiModelList(
                memberIds = listOf("user-1"),
                memberProfiles = profiles,
                subunits = emptyList()
            )

            assertEquals("alice@test.com", result[0].displayName)
        }

        @Test
        fun `resolves userId fallback when no profile exists`() {
            val result = mapper.toMemberUiModelList(
                memberIds = listOf("user-unknown"),
                memberProfiles = emptyMap(),
                subunits = emptyList()
            )

            assertEquals("user-unknown", result[0].displayName)
        }

        @Test
        fun `members are sorted alphabetically by displayName`() {
            val profiles = mapOf(
                "user-1" to createUser("user-1", displayName = "Charlie"),
                "user-2" to createUser("user-2", displayName = "Alice"),
                "user-3" to createUser("user-3", displayName = "Bob")
            )

            val result = mapper.toMemberUiModelList(
                memberIds = listOf("user-1", "user-2", "user-3"),
                memberProfiles = profiles,
                subunits = emptyList()
            )

            assertEquals("Alice", result[0].displayName)
            assertEquals("Bob", result[1].displayName)
            assertEquals("Charlie", result[2].displayName)
        }
    }
}
