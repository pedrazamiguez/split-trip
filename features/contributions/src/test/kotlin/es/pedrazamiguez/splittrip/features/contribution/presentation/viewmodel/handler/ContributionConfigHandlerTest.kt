package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.contribution.presentation.mapper.AddContributionUiMapper
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.action.AddContributionUiAction
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ContributionConfigHandler")
class ContributionConfigHandlerTest {

    private lateinit var handler: ContributionConfigHandler
    private lateinit var getGroupByIdUseCase: GetGroupByIdUseCase
    private lateinit var getGroupSubunitsUseCase: GetGroupSubunitsUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var addContributionUiMapper: AddContributionUiMapper
    private lateinit var appConfigService: AppConfigService

    private lateinit var uiState: MutableStateFlow<AddContributionUiState>
    private lateinit var actions: MutableSharedFlow<AddContributionUiAction>

    private val testGroup = Group(
        id = "group-1",
        name = "Trip to Paris",
        currency = "EUR",
        members = listOf("user-1", "user-2")
    )

    private val testMemberProfiles = mapOf(
        "user-1" to User(userId = "user-1", email = "andres@test.com", displayName = "Andrés"),
        "user-2" to User(userId = "user-2", email = "ana@test.com", displayName = "Ana")
    )

    private val testMemberOptions = persistentListOf(
        MemberOptionUiModel(userId = "user-1", displayName = "Andrés", isCurrentUser = true),
        MemberOptionUiModel(userId = "user-2", displayName = "Ana", isCurrentUser = false)
    )

    @BeforeEach
    fun setUp() {
        getGroupByIdUseCase = mockk()
        getGroupSubunitsUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        authenticationService = mockk()
        addContributionUiMapper = mockk(relaxed = true)
        appConfigService = mockk()

        uiState = MutableStateFlow(AddContributionUiState())
        actions = MutableSharedFlow(extraBufferCapacity = 1)

        val defaultCurrencyFlow = MutableStateFlow("EUR")
        every { appConfigService.defaultCurrencyCode } returns defaultCurrencyFlow

        handler = ContributionConfigHandler(
            getGroupByIdUseCase = getGroupByIdUseCase,
            getGroupSubunitsUseCase = getGroupSubunitsUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            authenticationService = authenticationService,
            addContributionUiMapper = addContributionUiMapper,
            appConfigService = appConfigService
        )

        // Default stubs
        coEvery { getGroupSubunitsUseCase(any()) } returns emptyList()
        coEvery { getMemberProfilesUseCase(any()) } returns testMemberProfiles
        every { authenticationService.currentUserId() } returns "user-1"
        every { addContributionUiMapper.resolveCurrencySymbol(any()) } returns "€"
        every {
            addContributionUiMapper.toMemberOptions(any(), any(), any())
        } returns testMemberOptions
        every {
            addContributionUiMapper.resolveDisplayName(any(), any())
        } answers {
            val userId = firstArg<String?>()
            testMemberOptions.firstOrNull { it.userId == userId }?.displayName ?: ""
        }
    }

    @Nested
    @DisplayName("setGroupCurrency")
    inner class SetGroupCurrency {

        @Test
        fun `updates groupCurrency and UI state synchronously`() = runTest {
            handler.bind(uiState, actions, this)

            handler.setGroupCurrency("USD")

            assertEquals("USD", handler.groupCurrency)
            assertEquals("USD", uiState.value.groupCurrencyCode)
        }

        @Test
        fun `resolves currency symbol via mapper`() = runTest {
            every { addContributionUiMapper.resolveCurrencySymbol("JPY") } returns "¥"
            handler.bind(uiState, actions, this)

            handler.setGroupCurrency("JPY")

            assertEquals("¥", uiState.value.groupCurrencySymbol)
        }

        @Test
        fun `null currency falls back to default`() = runTest {
            handler.bind(uiState, actions, this)

            handler.setGroupCurrency(null)

            assertEquals("EUR", handler.groupCurrency)
            assertEquals("EUR", uiState.value.groupCurrencyCode)
        }
    }

    @Nested
    @DisplayName("loadGroupConfig")
    inner class LoadGroupConfig {

        @Test
        fun `does nothing when groupId is null`() = runTest {
            handler.bind(uiState, actions, this)
            handler.loadGroupConfig(null)
            advanceUntilIdle()

            assertTrue(uiState.value.groupMembers.isEmpty())
            coVerify(exactly = 0) { getGroupByIdUseCase(any()) }
        }

        @Test
        fun `loads members and subunits on success`() = runTest {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertEquals(testMemberOptions.size, uiState.value.groupMembers.size)
            assertEquals("user-1", uiState.value.selectedMemberId)
            assertEquals("Andrés", uiState.value.selectedMemberDisplayName)
        }

        @Test
        fun `updates groupCurrency from loaded group`() = runTest {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertEquals("EUR", handler.groupCurrency)
            assertEquals("EUR", uiState.value.groupCurrencyCode)
        }

        @Test
        fun `resets form fields on successful load`() = runTest {
            uiState.value = AddContributionUiState(
                amountInput = "999",
                amountError = true,
                contributionScope = PayerType.SUBUNIT,
                selectedSubunitId = "old-subunit"
            )
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertEquals("", uiState.value.amountInput)
            assertEquals(false, uiState.value.amountError)
            assertEquals(PayerType.USER, uiState.value.contributionScope)
            assertNull(uiState.value.selectedSubunitId)
        }

        @Test
        fun `skips load when same groupId already loaded`() = runTest {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            coVerify(exactly = 1) { getGroupByIdUseCase("group-1") }
        }

        @Test
        fun `reloads when group ID changes`() = runTest {
            val group2 = testGroup.copy(id = "group-2", name = "Second Trip")
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { getGroupByIdUseCase("group-2") } returns group2

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            handler.loadGroupConfig("group-2")
            advanceUntilIdle()

            coVerify(exactly = 1) { getGroupByIdUseCase("group-1") }
            coVerify(exactly = 1) { getGroupByIdUseCase("group-2") }
        }

        @Test
        fun `resets state and emits ShowError on failure`() = runTest {
            coEvery { getGroupByIdUseCase("group-1") } throws RuntimeException("Network error")

            handler.bind(uiState, actions, this)

            val emitted = mutableListOf<AddContributionUiAction>()
            val collectJob = launch { actions.collect { emitted.add(it) } }

            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            // State is reset to safe defaults
            assertEquals(PayerType.USER, uiState.value.contributionScope)
            assertNull(uiState.value.selectedSubunitId)
            // ShowError action was emitted
            assertTrue(emitted.any { it is AddContributionUiAction.ShowError })

            collectJob.cancel()
        }

        @Test
        fun `calls getMemberProfilesUseCase with group members`() = runTest {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            coVerify { getMemberProfilesUseCase(listOf("user-1", "user-2")) }
        }

        @Test
        fun `filters subunits to only those containing current user`() = runTest {
            val memberSubunit = Subunit(
                id = "sub-1",
                groupId = "group-1",
                name = "Couple A",
                memberIds = listOf("user-1", "user-2")
            )
            val otherSubunit = Subunit(
                id = "sub-2",
                groupId = "group-1",
                name = "Other Team",
                memberIds = listOf("user-3", "user-4")
            )
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { getGroupSubunitsUseCase("group-1") } returns listOf(
                memberSubunit,
                otherSubunit
            )

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertEquals(1, uiState.value.subunitOptions.size)
            assertEquals("sub-1", uiState.value.subunitOptions[0].id)
        }

        @Test
        fun `loads no subunit options when user is in no subunits`() = runTest {
            val otherSubunit = Subunit(
                id = "sub-1",
                groupId = "group-1",
                name = "Other Team",
                memberIds = listOf("user-99")
            )
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { getGroupSubunitsUseCase("group-1") } returns listOf(otherSubunit)

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertTrue(uiState.value.subunitOptions.isEmpty())
        }

        @Test
        fun `defaults selectedMemberId to current user`() = runTest {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            assertEquals("user-1", uiState.value.selectedMemberId)
        }
    }

    @Nested
    @DisplayName("handleMemberSelected")
    inner class HandleMemberSelected {

        @Test
        fun `updates selectedMemberId and display name`() = runTest {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            handler.handleMemberSelected("user-2")

            assertEquals("user-2", uiState.value.selectedMemberId)
            assertEquals("Ana", uiState.value.selectedMemberDisplayName)
        }

        @Test
        fun `resets contributionScope and selectedSubunitId`() = runTest {
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            uiState.value = uiState.value.copy(
                groupMembers = testMemberOptions,
                contributionScope = PayerType.SUBUNIT,
                selectedSubunitId = "sub-1"
            )

            handler.bind(uiState, actions, this)
            handler.handleMemberSelected("user-2")

            assertEquals(PayerType.USER, uiState.value.contributionScope)
            assertNull(uiState.value.selectedSubunitId)
        }

        @Test
        fun `re-filters subunits for the new member`() = runTest {
            val subunitForBoth = Subunit(
                id = "sub-1",
                groupId = "group-1",
                name = "Couple",
                memberIds = listOf("user-1", "user-2")
            )
            val subunitForUser2Only = Subunit(
                id = "sub-2",
                groupId = "group-1",
                name = "Couple B",
                memberIds = listOf("user-2", "user-3")
            )
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { getGroupSubunitsUseCase("group-1") } returns listOf(
                subunitForBoth,
                subunitForUser2Only
            )

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            // user-1 is only in sub-1
            assertEquals(1, uiState.value.subunitOptions.size)

            // Select user-2 → should see both subunits
            handler.handleMemberSelected("user-2")
            assertEquals(2, uiState.value.subunitOptions.size)
        }
    }

    @Nested
    @DisplayName("filterSubunitsForMember")
    inner class FilterSubunitsForMember {

        @Test
        fun `returns subunits for the specified member`() = runTest {
            val subunits = listOf(
                Subunit(
                    id = "sub-1",
                    groupId = "group-1",
                    name = "Couple",
                    memberIds = listOf("user-1", "user-2")
                ),
                Subunit(
                    id = "sub-2",
                    groupId = "group-1",
                    name = "Other",
                    memberIds = listOf("user-3")
                )
            )
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { getGroupSubunitsUseCase("group-1") } returns subunits

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            val filtered = handler.filterSubunitsForMember("user-1")
            assertEquals(1, filtered.size)
            assertEquals("sub-1", filtered[0].id)
        }

        @Test
        fun `returns empty list for member in no subunits`() = runTest {
            val subunits = listOf(
                Subunit(
                    id = "sub-1",
                    groupId = "group-1",
                    name = "Couple",
                    memberIds = listOf("user-1")
                )
            )
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { getGroupSubunitsUseCase("group-1") } returns subunits

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            val filtered = handler.filterSubunitsForMember("user-99")
            assertTrue(filtered.isEmpty())
        }

        @Test
        fun `returns empty list for null memberId`() = runTest {
            val subunits = listOf(
                Subunit(
                    id = "sub-1",
                    groupId = "group-1",
                    name = "Couple",
                    memberIds = listOf("user-1")
                )
            )
            coEvery { getGroupByIdUseCase("group-1") } returns testGroup
            coEvery { getGroupSubunitsUseCase("group-1") } returns subunits

            handler.bind(uiState, actions, this)
            handler.loadGroupConfig("group-1")
            advanceUntilIdle()

            val filtered = handler.filterSubunitsForMember(null)
            assertTrue(filtered.isEmpty())
        }
    }
}
