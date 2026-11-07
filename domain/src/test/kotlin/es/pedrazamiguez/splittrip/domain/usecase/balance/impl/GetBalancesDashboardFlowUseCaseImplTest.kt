package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.datasource.GroupDashboardDataSource
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.GroupDashboardReadModel
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupPocketBalanceFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetBalancesDashboardFlowUseCaseImplTest {

    private lateinit var useCase: GetBalancesDashboardFlowUseCaseImpl
    private val groupDashboardDataSource: GroupDashboardDataSource = mockk()
    private val getGroupPocketBalanceFlowUseCase: GetGroupPocketBalanceFlowUseCase = mockk()
    private val getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase = mockk()
    private val getSettlementSuggestionsUseCase: GetSettlementSuggestionsUseCase = mockk()
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase = mockk()
    private val appConfigService: AppConfigService = mockk()

    @BeforeEach
    fun setup() {
        every { appConfigService.balanceComputationDebounceMs } returns MutableStateFlow(0L)

        useCase = GetBalancesDashboardFlowUseCaseImpl(
            groupDashboardDataSource,
            getGroupPocketBalanceFlowUseCase,
            getMemberBalancesFlowUseCase,
            getSettlementSuggestionsUseCase,
            getMemberProfilesUseCase,
            appConfigService
        )
    }

    @Test
    fun `invoke emits BalancesDashboardDomainModel with computed member balances`() = runTest {
        // Setup mock responses
        val group = Group(id = "1", name = "Test", currency = "EUR", status = GroupStatus.ACTIVE)
        val snapshot = GroupDashboardReadModel(
            group = group,
            contributions = listOf(
                Contribution(id = "c1", groupId = "1", userId = "u1", amount = 10L, currency = "EUR", createdAt = null)
            ),
            withdrawals = emptyList(),
            subunits = emptyList(),
            expenses = emptyList(),
            settlements = emptyList()
        )
        val memberBalances = listOf(MemberBalance(userId = "u1", pocketBalance = 10L))
        val settlements = emptyList<SettlementRecord>()
        val settlementSuggestions = emptyList<Settlement>()
        val profiles = mapOf("u1" to User(userId = "u1", email = "test@test.com", displayName = "Test User"))

        every { groupDashboardDataSource.getDashboardSnapshotFlow("1") } returns flowOf(snapshot)
        every { getGroupPocketBalanceFlowUseCase.calculate(any(), any(), any(), "EUR") } returns
            GroupPocketBalance(0L, 0L, 0L, "EUR", emptyMap(), emptyMap(), 0L, 0L, 0L, 0L)
        every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns
            memberBalances
        coEvery { getMemberProfilesUseCase(any()) } returns profiles
        every { getSettlementSuggestionsUseCase(any()) } returns settlementSuggestions

        val result = useCase("1", "EUR", listOf("u1")).first()

        assertEquals(1, result.memberBalances.size)
        assertEquals("u1", result.memberBalances[0].userId)
    }

    @Test
    fun `invoke calls getMemberProfilesUseCase with union of groupMemberIds and contribution userIds`() = runTest {
        val group = Group(id = "1", name = "Test", currency = "EUR", status = GroupStatus.ACTIVE)
        val snapshot = GroupDashboardReadModel(
            group = group,
            contributions = listOf(
                Contribution(id = "c1", groupId = "1", userId = "u2", amount = 10L, currency = "EUR", createdAt = null)
            ),
            withdrawals = emptyList(),
            subunits = emptyList(),
            expenses = emptyList(),
            settlements = emptyList()
        )
        val memberBalances = emptyList<MemberBalance>()
        val settlements = emptyList<SettlementRecord>()
        val settlementSuggestions = emptyList<Settlement>()
        val profiles = mapOf(
            "u1" to User(userId = "u1", email = "", displayName = "User 1"),
            "u2" to User(userId = "u2", email = "", displayName = "User 2")
        )

        every { groupDashboardDataSource.getDashboardSnapshotFlow("1") } returns flowOf(snapshot)
        every { getGroupPocketBalanceFlowUseCase.calculate(any(), any(), any(), "EUR") } returns
            GroupPocketBalance(0L, 0L, 0L, "EUR", emptyMap(), emptyMap(), 0L, 0L, 0L, 0L)
        every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns
            memberBalances
        coEvery { getMemberProfilesUseCase(any()) } returns profiles
        every { getSettlementSuggestionsUseCase(any()) } returns settlementSuggestions

        useCase("1", "EUR", listOf("u1")).first()

        coVerify { getMemberProfilesUseCase(match { it.containsAll(listOf("u1", "u2")) }) }
    }

    @Test
    fun `invoke delegates settlement suggestions to getSettlementSuggestionsUseCase`() = runTest {
        val group = Group(id = "1", name = "Test", currency = "EUR", status = GroupStatus.ACTIVE)
        val snapshot = GroupDashboardReadModel(
            group = group,
            contributions = emptyList(),
            withdrawals = emptyList(),
            subunits = emptyList(),
            expenses = emptyList(),
            settlements = emptyList()
        )
        val memberBalances = listOf(MemberBalance(userId = "u1", pocketBalance = 10L))
        val settlements = listOf(Settlement(fromUserId = "u1", toUserId = "u2", amount = 10L, currency = "EUR"))
        val profiles = emptyMap<String, User>()

        every { groupDashboardDataSource.getDashboardSnapshotFlow("1") } returns flowOf(snapshot)
        every { getGroupPocketBalanceFlowUseCase.calculate(any(), any(), any(), "EUR") } returns
            GroupPocketBalance(0L, 0L, 0L, "EUR", emptyMap(), emptyMap(), 0L, 0L, 0L, 0L)
        every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns
            memberBalances
        coEvery { getMemberProfilesUseCase(any()) } returns profiles
        every { getSettlementSuggestionsUseCase(memberBalances) } returns settlements

        val result = useCase("1", "EUR", emptyList()).first()

        assertEquals(1, result.settlementSuggestions.size)
        assertEquals("u1", result.settlementSuggestions[0].fromUserId)
    }

    @Test
    fun `invoke emits updated model when upstream snapshot changes`() = runTest {
        val group = Group(id = "1", name = "Test", currency = "EUR", status = GroupStatus.ACTIVE)
        val snapshot1 = GroupDashboardReadModel(
            group = group,
            contributions = emptyList(),
            withdrawals = emptyList(),
            subunits = emptyList(),
            expenses = emptyList(),
            settlements = emptyList()
        )
        val snapshot2 = snapshot1.copy(
            contributions = listOf(
                Contribution(id = "c1", groupId = "1", userId = "u1", amount = 10L, currency = "EUR", createdAt = null)
            )
        )

        every { groupDashboardDataSource.getDashboardSnapshotFlow("1") } returns flowOf(snapshot1, snapshot2)
        every { getGroupPocketBalanceFlowUseCase.calculate(any(), any(), any(), "EUR") } returns
            GroupPocketBalance(0L, 0L, 0L, "EUR", emptyMap(), emptyMap(), 0L, 0L, 0L, 0L)
        every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns
            emptyList()
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()
        every { getSettlementSuggestionsUseCase(any()) } returns emptyList()

        val results = useCase("1", "EUR", emptyList()).toList()

        assertEquals(2, results.size)
        assertEquals(0, results[0].contributions.size)
        assertEquals(1, results[1].contributions.size)
    }

    @Test
    fun `invoke calculates pocket balance directly from snapshot without invoking secondary flow`() = runTest {
        val group = Group(id = "1", name = "Test", currency = "EUR", status = GroupStatus.ACTIVE)
        val snapshot = GroupDashboardReadModel(
            group = group,
            contributions = emptyList(),
            withdrawals = emptyList(),
            subunits = emptyList(),
            expenses = emptyList(),
            settlements = emptyList()
        )

        every { groupDashboardDataSource.getDashboardSnapshotFlow("1") } returns flowOf(snapshot)
        every { getGroupPocketBalanceFlowUseCase.calculate(any(), any(), any(), "EUR") } returns
            GroupPocketBalance(0L, 0L, 0L, "EUR", emptyMap(), emptyMap(), 0L, 0L, 0L, 0L)
        every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns emptyList()
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()
        every { getSettlementSuggestionsUseCase(any()) } returns emptyList()

        useCase("1", "EUR", emptyList()).first()

        verify(exactly = 1) {
            getGroupPocketBalanceFlowUseCase.calculate(
                contributions = snapshot.contributions,
                expenses = snapshot.expenses,
                withdrawals = snapshot.withdrawals,
                currency = "EUR"
            )
        }
        verify(exactly = 0) { getGroupPocketBalanceFlowUseCase(any(), any()) }
    }
}
