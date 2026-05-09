package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.domain.service.split.SubunitAwareSplitService
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseSplitUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SubunitSplitEventHandler].
 *
 * All state mutations are synchronous. [runTest] is used only to supply the
 * [CoroutineScope] required by [bind].
 */
class SubunitSplitEventHandlerTest {

    private lateinit var handler: SubunitSplitEventHandler
    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var actions: MutableSharedFlow<AddExpenseUiAction>

    private val eurCurrency = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)

    private val equalSplitType = SplitTypeUiModel(id = "EQUAL", displayText = "Equal")
    private val exactSplitType = SplitTypeUiModel(id = "EXACT", displayText = "Exact")
    private val percentSplitType = SplitTypeUiModel(id = "PERCENT", displayText = "Percent")

    /** A group with 2 solo members and one subunit (couple). */
    private val subunitCoupleId = "subunit-couple"
    private val soloMember1 = "solo-1"
    private val soloMember2 = "solo-2"
    private val coupleMember1 = "couple-member-1"
    private val coupleMember2 = "couple-member-2"

    private val coupleSubunit = Subunit(
        id = subunitCoupleId,
        groupId = "group-1",
        name = "The Couple",
        memberIds = listOf(coupleMember1, coupleMember2),
        memberShares = emptyMap() // equal shares implied
    )

    private fun makeSplit(
        userId: String,
        displayName: String = userId,
        amountCents: Long = 0L,
        amountInput: String = "",
        percentageInput: String = "",
        isShareLocked: Boolean = false,
        isExcluded: Boolean = false,
        isEntityRow: Boolean = false,
        isExpanded: Boolean = false,
        entityMembers: List<SplitUiModel> = emptyList(),
        entitySplitType: SplitTypeUiModel? = null
    ) = SplitUiModel(
        userId = userId,
        displayName = displayName,
        amountCents = amountCents,
        amountInput = amountInput,
        percentageInput = percentageInput,
        isShareLocked = isShareLocked,
        isExcluded = isExcluded,
        isEntityRow = isEntityRow,
        isExpanded = isExpanded,
        entityMembers = entityMembers.toImmutableList(),
        entitySplitType = entitySplitType
    )

    /** Base state for entity-split tests (100 EUR, 3 entities: 2 solo + 1 subunit). */
    private val baseEntityState = AddExpenseUiState(
        loadedGroupId = "group-1",
        groupCurrency = eurCurrency,
        selectedCurrency = eurCurrency,
        sourceAmount = "100",
        availableSplitTypes = persistentListOf(equalSplitType, exactSplitType, percentSplitType),
        selectedSplitType = equalSplitType,
        isSubunitMode = true,
        hasSubunits = true,
        entitySplits = persistentListOf(
            makeSplit(soloMember1, isEntityRow = true),
            makeSplit(soloMember2, isEntityRow = true),
            makeSplit(
                subunitCoupleId,
                displayName = "The Couple",
                isEntityRow = true,
                entitySplitType = equalSplitType,
                entityMembers = listOf(
                    makeSplit(coupleMember1),
                    makeSplit(coupleMember2)
                )
            )
        )
    )

    @BeforeEach
    fun setUp() {
        val localeProvider = mockk<LocaleProvider>()
        every { localeProvider.getCurrentLocale() } returns Locale.US

        val splitPreviewService = SplitPreviewService()
        val splitCalculatorFactory = ExpenseSplitCalculatorFactory(ExpenseCalculatorService())
        val formattingHelper = FormattingHelper(localeProvider)
        val remainderDistributionService = RemainderDistributionService()

        val intraSubunitSplitDelegate = IntraSubunitSplitDelegate(
            splitCalculatorFactory = splitCalculatorFactory,
            splitPreviewService = splitPreviewService,
            subunitAwareSplitService = SubunitAwareSplitService(splitCalculatorFactory),
            formattingHelper = formattingHelper
        )

        val splitRowMappingDelegate = SplitRowMappingDelegate(
            splitCalculatorFactory = splitCalculatorFactory,
            splitPreviewService = splitPreviewService,
            formattingHelper = formattingHelper
        )

        handler = SubunitSplitEventHandler(
            splitPreviewService = splitPreviewService,
            addExpenseSplitMapper = AddExpenseSplitUiMapper(
                localeProvider,
                formattingHelper,
                splitPreviewService,
                EntitySplitFlattenDelegate(splitPreviewService, remainderDistributionService)
            ),
            intraSubunitSplitDelegate = intraSubunitSplitDelegate,
            splitRowMappingDelegate = splitRowMappingDelegate
        )

        uiState = MutableStateFlow(baseEntityState)
        actions = MutableSharedFlow()
    }

    // ── initEntitySplits ─────────────────────────────────────────────────

    @Nested
    inner class InitEntitySplits {

        @Test
        fun `creates entity rows for solo members and subunit`() = runTest {
            uiState.value = AddExpenseUiState(
                loadedGroupId = "group-1",
                selectedCurrency = eurCurrency,
                availableSplitTypes = persistentListOf(equalSplitType)
            )
            handler.bind(uiState, actions, this)

            handler.initEntitySplits(
                memberIds = listOf(soloMember1, soloMember2, coupleMember1, coupleMember2),
                subunits = listOf(coupleSubunit)
            )

            val state = uiState.value
            assertTrue(state.hasSubunits)
            assertEquals(3, state.entitySplits.size) // 2 solo + 1 subunit entity
        }

        @Test
        fun `subunit entity row contains nested member rows`() = runTest {
            uiState.value = AddExpenseUiState(
                loadedGroupId = "group-1",
                selectedCurrency = eurCurrency,
                availableSplitTypes = persistentListOf(equalSplitType)
            )
            handler.bind(uiState, actions, this)

            handler.initEntitySplits(
                memberIds = listOf(soloMember1, coupleMember1, coupleMember2),
                subunits = listOf(coupleSubunit)
            )

            val subunitRow = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            assertTrue(subunitRow.isEntityRow)
            assertEquals(2, subunitRow.entityMembers.size)
        }

        @Test
        fun `does nothing when subunit list is empty`() = runTest {
            uiState.value = AddExpenseUiState(loadedGroupId = "group-1", selectedCurrency = eurCurrency)
            handler.bind(uiState, actions, this)

            handler.initEntitySplits(
                memberIds = listOf(soloMember1),
                subunits = emptyList()
            )

            // entitySplits should remain empty (no subunits)
            assertEquals(0, uiState.value.entitySplits.size)
            assertFalse(uiState.value.hasSubunits)
        }

        @Test
        fun `solo members are sorted alphabetically`() = runTest {
            uiState.value = AddExpenseUiState(
                loadedGroupId = "group-1",
                selectedCurrency = eurCurrency,
                availableSplitTypes = persistentListOf(equalSplitType)
            )
            handler.bind(uiState, actions, this)

            handler.initEntitySplits(
                memberIds = listOf("zeta", "alpha", coupleMember1, coupleMember2),
                subunits = listOf(coupleSubunit)
            )

            val entitySplits = uiState.value.entitySplits
            val soloRows = entitySplits.filter { it.entityMembers.isEmpty() }
            // Solo members should appear before subunits, sorted alphabetically
            assertEquals("alpha", soloRows.first().userId)
            assertEquals("zeta", soloRows.last().userId)
        }
    }

    // ── clearEntitySplits ────────────────────────────────────────────────

    @Nested
    inner class ClearEntitySplits {

        @Test
        fun `resets hasSubunits isSubunitMode and entitySplits`() = runTest {
            handler.bind(uiState, actions, this)

            handler.clearEntitySplits()

            val state = uiState.value
            assertFalse(state.hasSubunits)
            assertFalse(state.isSubunitMode)
            assertTrue(state.entitySplits.isEmpty())
        }
    }

    // ── handleSubunitModeToggled ─────────────────────────────────────────

    @Nested
    inner class SubunitModeToggled {

        @Test
        fun `toggles isSubunitMode from false to true`() = runTest {
            uiState.value = baseEntityState.copy(isSubunitMode = false, sourceAmount = "0")
            handler.bind(uiState, actions, this)

            handler.handleSubunitModeToggled()

            assertTrue(uiState.value.isSubunitMode)
        }

        @Test
        fun `toggles isSubunitMode from true to false`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleSubunitModeToggled()

            assertFalse(uiState.value.isSubunitMode)
        }

        @Test
        fun `enabling mode triggers entity split recalculation (EQUAL distributes evenly)`() = runTest {
            uiState.value = baseEntityState.copy(isSubunitMode = false)
            handler.bind(uiState, actions, this)

            handler.handleSubunitModeToggled()

            val total = uiState.value.entitySplits.filter { !it.isExcluded }.sumOf { it.amountCents }
            assertEquals(10000L, total)
        }

        @Test
        fun `clears split error on toggle`() = runTest {
            uiState.value = baseEntityState.copy(
                isSubunitMode = false,
                splitError = es.pedrazamiguez.splittrip.core.common.presentation.UiText.DynamicString("err")
            )
            handler.bind(uiState, actions, this)

            handler.handleSubunitModeToggled()

            assertTrue(uiState.value.splitError == null)
        }
    }

    // ── handleAccordionToggled ───────────────────────────────────────────

    @Nested
    inner class AccordionToggled {

        @Test
        fun `expands a collapsed subunit row`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleAccordionToggled(subunitCoupleId)

            val entity = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            assertTrue(entity.isExpanded)
        }

        @Test
        fun `collapses an expanded subunit row`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == subunitCoupleId) entity.copy(isExpanded = true) else entity
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleAccordionToggled(subunitCoupleId)

            val entity = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            assertFalse(entity.isExpanded)
        }

        @Test
        fun `no-op for solo-member entity rows (no entityMembers)`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleAccordionToggled(soloMember1)

            // Solo member row should not be expanded
            val solo = uiState.value.entitySplits.first { it.userId == soloMember1 }
            assertFalse(solo.isExpanded)
        }
    }

    // ── handleEntityExcludedToggled ──────────────────────────────────────

    @Nested
    inner class EntityExcludedToggled {

        @Test
        fun `marks entity as excluded`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleEntityExcludedToggled(soloMember1)

            assertTrue(uiState.value.entitySplits.first { it.userId == soloMember1 }.isExcluded)
        }

        @Test
        fun `re-including an excluded entity restores it`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == soloMember1) entity.copy(isExcluded = true) else entity
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleEntityExcludedToggled(soloMember1)

            assertFalse(uiState.value.entitySplits.first { it.userId == soloMember1 }.isExcluded)
        }

        @Test
        fun `clears all entity locks on toggle`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { it.copy(isShareLocked = true) }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleEntityExcludedToggled(soloMember2)

            uiState.value.entitySplits.forEach { assertFalse(it.isShareLocked) }
        }

        @Test
        fun `excluding one entity redistributes amount to remaining entities`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleEntityExcludedToggled(soloMember1)

            val activeTotal = uiState.value.entitySplits.filter { !it.isExcluded }.sumOf { it.amountCents }
            assertEquals(10000L, activeTotal)
        }
    }

    // ── handleEntityShareLockToggled ─────────────────────────────────────

    @Nested
    inner class EntityShareLockToggled {

        @Test
        fun `toggles lock on for an unlocked entity`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleEntityShareLockToggled(soloMember1)

            assertTrue(uiState.value.entitySplits.first { it.userId == soloMember1 }.isShareLocked)
        }

        @Test
        fun `toggles lock off for a locked entity`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == soloMember1) entity.copy(isShareLocked = true) else entity
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleEntityShareLockToggled(soloMember1)

            assertFalse(uiState.value.entitySplits.first { it.userId == soloMember1 }.isShareLocked)
        }

        @Test
        fun `does not affect other entities`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == soloMember2) entity.copy(isShareLocked = true) else entity
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleEntityShareLockToggled(soloMember1)

            assertTrue(uiState.value.entitySplits.first { it.userId == soloMember2 }.isShareLocked)
        }
    }

    // ── handleEntityAmountChanged (EXACT entity level) ───────────────────

    @Nested
    inner class EntityAmountChanged {

        @Test
        fun `typing an amount for one entity auto-distributes remainder to others`() = runTest {
            uiState.value = baseEntityState.copy(selectedSplitType = exactSplitType)
            handler.bind(uiState, actions, this)

            // soloMember1 claims 40 EUR → remaining 2 entities share 60 EUR
            handler.handleEntityAmountChanged(soloMember1, "40")

            val solo1 = uiState.value.entitySplits.first { it.userId == soloMember1 }
            assertEquals(4000L, solo1.amountCents)
            assertTrue(solo1.isShareLocked)

            val otherTotal = uiState.value.entitySplits.filter { it.userId != soloMember1 }.sumOf { it.amountCents }
            assertEquals(6000L, otherTotal)
        }

        @Test
        fun `entity split total equals source amount after edit`() = runTest {
            uiState.value = baseEntityState.copy(selectedSplitType = exactSplitType)
            handler.bind(uiState, actions, this)

            handler.handleEntityAmountChanged(soloMember2, "35")

            assertEquals(10000L, uiState.value.entitySplits.sumOf { it.amountCents })
        }

        @Test
        fun `locked entities keep their amount when another entity edits`() = runTest {
            uiState.value = baseEntityState.copy(
                selectedSplitType = exactSplitType,
                entitySplits = persistentListOf(
                    makeSplit(soloMember1, isEntityRow = true, amountCents = 3000L, isShareLocked = true),
                    makeSplit(soloMember2, isEntityRow = true, amountCents = 3000L, isShareLocked = true),
                    makeSplit(
                        subunitCoupleId,
                        displayName = "The Couple",
                        isEntityRow = true,
                        amountCents = 4000L,
                        entitySplitType = equalSplitType,
                        entityMembers = listOf(
                            makeSplit(coupleMember1),
                            makeSplit(coupleMember2)
                        )
                    )
                )
            )
            handler.bind(uiState, actions, this)

            handler.handleEntityAmountChanged(subunitCoupleId, "50")

            assertEquals(3000L, uiState.value.entitySplits.first { it.userId == soloMember1 }.amountCents)
            assertEquals(3000L, uiState.value.entitySplits.first { it.userId == soloMember2 }.amountCents)
        }

        @Test
        fun `changing entity amount recalculates intra-subunit member splits`() = runTest {
            uiState.value = baseEntityState.copy(selectedSplitType = exactSplitType)
            handler.bind(uiState, actions, this)

            // Assign 60 EUR to the couple subunit
            handler.handleEntityAmountChanged(subunitCoupleId, "60")

            val subunit = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            val memberTotal = subunit.entityMembers.sumOf { it.amountCents }
            assertEquals(6000L, memberTotal)
        }
    }

    // ── handleEntityPercentageChanged ─────────────────────────────────────

    @Nested
    inner class EntityPercentageChanged {

        @Test
        fun `typing a percentage for one entity redistributes remaining percent`() = runTest {
            uiState.value = baseEntityState.copy(selectedSplitType = percentSplitType)
            handler.bind(uiState, actions, this)

            // soloMember1 claims 50% → other two entities share 50%
            handler.handleEntityPercentageChanged(soloMember1, "50")

            val solo1 = uiState.value.entitySplits.first { it.userId == soloMember1 }
            assertEquals("50", solo1.percentageInput)
            assertTrue(solo1.isShareLocked)
            assertEquals(5000L, solo1.amountCents)
        }

        @Test
        fun `entity cents total equals source amount after percentage edit`() = runTest {
            uiState.value = baseEntityState.copy(selectedSplitType = percentSplitType)
            handler.bind(uiState, actions, this)

            handler.handleEntityPercentageChanged(soloMember2, "40")

            assertEquals(10000L, uiState.value.entitySplits.sumOf { it.amountCents })
        }
    }

    // ── recalculateEntitySplits ───────────────────────────────────────────

    @Nested
    inner class RecalculateEntitySplits {

        @Test
        fun `EQUAL distributes evenly across 3 entities`() = runTest {
            handler.bind(uiState, actions, this)

            handler.recalculateEntitySplits()

            assertEquals(10000L, uiState.value.entitySplits.sumOf { it.amountCents })
        }

        @Test
        fun `no-op when not in subunit mode`() = runTest {
            uiState.value = baseEntityState.copy(isSubunitMode = false)
            handler.bind(uiState, actions, this)

            handler.recalculateEntitySplits()

            uiState.value.entitySplits.forEach { assertEquals(0L, it.amountCents) }
        }

        @Test
        fun `no-op when entitySplits is empty`() = runTest {
            uiState.value = baseEntityState.copy(entitySplits = persistentListOf())
            handler.bind(uiState, actions, this)

            // Should not throw
            handler.recalculateEntitySplits()
        }

        @Test
        fun `EQUAL distributes only to non-excluded entities`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == soloMember1) entity.copy(isExcluded = true) else entity
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.recalculateEntitySplits()

            assertEquals(0L, uiState.value.entitySplits.first { it.userId == soloMember1 }.amountCents)
            val activeTotal = uiState.value.entitySplits.filter { !it.isExcluded }.sumOf { it.amountCents }
            assertEquals(10000L, activeTotal)
        }
    }

    // ── handleIntraSubunitSplitTypeChanged ────────────────────────────────

    @Nested
    inner class IntraSubunitSplitTypeChanged {

        @Test
        fun `changes entitySplitType for the specified subunit`() = runTest {
            // Pre-populate entity amounts so recalculation has data to work with
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId ==
                        subunitCoupleId
                    ) {
                        entity.copy(amountCents = 6000L)
                    } else {
                        entity.copy(amountCents = 2000L)
                    }
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleIntraSubunitSplitTypeChanged(subunitCoupleId, exactSplitType.id)

            val subunit = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            assertEquals(exactSplitType, subunit.entitySplitType)
        }

        @Test
        fun `clears member locks when changing intra split type`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == subunitCoupleId) {
                        entity.copy(
                            amountCents = 6000L,
                            entityMembers = entity.entityMembers.map {
                                it.copy(isShareLocked = true)
                            }.toImmutableList()
                        )
                    } else {
                        entity.copy(amountCents = 2000L)
                    }
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleIntraSubunitSplitTypeChanged(subunitCoupleId, exactSplitType.id)

            val subunit = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            subunit.entityMembers.forEach { assertFalse(it.isShareLocked) }
        }
    }

    // ── handleIntraSubunitAmountChanged ───────────────────────────────────

    @Nested
    inner class IntraSubunitAmountChanged {

        @Test
        fun `editing a member's amount distributes remainder to other member`() = runTest {
            // Subunit has 6000 cents total, 2 members
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == subunitCoupleId) entity.copy(amountCents = 6000L) else entity
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleIntraSubunitAmountChanged(subunitCoupleId, coupleMember1, "40")

            val subunit = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            val member1 = subunit.entityMembers.first { it.userId == coupleMember1 }
            val member2 = subunit.entityMembers.first { it.userId == coupleMember2 }

            assertEquals(4000L, member1.amountCents)
            assertTrue(member1.isShareLocked)
            assertEquals(2000L, member2.amountCents)
        }

        @Test
        fun `member total equals subunit total after edit`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == subunitCoupleId) entity.copy(amountCents = 6000L) else entity
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleIntraSubunitAmountChanged(subunitCoupleId, coupleMember2, "30")

            val subunit = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            val memberTotal = subunit.entityMembers.sumOf { it.amountCents }
            assertEquals(6000L, memberTotal)
        }
    }

    // ── handleIntraSubunitPercentageChanged ───────────────────────────────

    @Nested
    inner class IntraSubunitPercentageChanged {

        @Test
        fun `editing a member percentage distributes remainder to other member`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == subunitCoupleId) entity.copy(amountCents = 6000L) else entity
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleIntraSubunitPercentageChanged(subunitCoupleId, coupleMember1, "60")

            val subunit = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            val member1 = subunit.entityMembers.first { it.userId == coupleMember1 }
            assertEquals("60", member1.percentageInput)
            assertTrue(member1.isShareLocked)
        }

        @Test
        fun `member cents total equals subunit total after percentage edit`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == subunitCoupleId) entity.copy(amountCents = 6000L) else entity
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleIntraSubunitPercentageChanged(subunitCoupleId, coupleMember2, "40")

            val subunit = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            assertEquals(6000L, subunit.entityMembers.sumOf { it.amountCents })
        }
    }

    // ── handleIntraSubunitShareLockToggled ────────────────────────────────

    @Nested
    inner class IntraSubunitShareLockToggled {

        @Test
        fun `toggles lock on for an unlocked member`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleIntraSubunitShareLockToggled(subunitCoupleId, coupleMember1)

            val subunit = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            assertTrue(subunit.entityMembers.first { it.userId == coupleMember1 }.isShareLocked)
        }

        @Test
        fun `toggles lock off for a locked member`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == subunitCoupleId) {
                        entity.copy(
                            entityMembers = entity.entityMembers.map { member ->
                                if (member.userId == coupleMember1) member.copy(isShareLocked = true) else member
                            }.toImmutableList()
                        )
                    } else {
                        entity
                    }
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleIntraSubunitShareLockToggled(subunitCoupleId, coupleMember1)

            val subunit = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            assertFalse(subunit.entityMembers.first { it.userId == coupleMember1 }.isShareLocked)
        }

        @Test
        fun `does not affect the other member`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { entity ->
                    if (entity.userId == subunitCoupleId) {
                        entity.copy(
                            entityMembers = entity.entityMembers.map { member ->
                                if (member.userId == coupleMember2) member.copy(isShareLocked = true) else member
                            }.toImmutableList()
                        )
                    } else {
                        entity
                    }
                }.toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.handleIntraSubunitShareLockToggled(subunitCoupleId, coupleMember1)

            val subunit = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            assertTrue(subunit.entityMembers.first { it.userId == coupleMember2 }.isShareLocked)
        }
    }

    // ── initEntitySplits with weighted memberShares ──────────────────────

    @Nested
    inner class WeightedMemberShares {

        @Test
        fun `intra-subunit EQUAL distribution uses weighted memberShares when provided`() = runTest {
            val weightedSubunit = Subunit(
                id = subunitCoupleId,
                groupId = "group-1",
                name = "The Couple",
                memberIds = listOf(coupleMember1, coupleMember2),
                // 70/30 split
                memberShares = mapOf(
                    coupleMember1 to BigDecimal("0.7"),
                    coupleMember2 to BigDecimal("0.3")
                )
            )

            uiState.value = AddExpenseUiState(
                loadedGroupId = "group-1",
                selectedCurrency = eurCurrency,
                availableSplitTypes = persistentListOf(equalSplitType)
            )
            handler.bind(uiState, actions, this)

            handler.initEntitySplits(
                memberIds = listOf(coupleMember1, coupleMember2),
                subunits = listOf(weightedSubunit)
            )

            // Now recalculate entity splits with 100 EUR
            uiState.value = uiState.value.copy(
                sourceAmount = "100",
                isSubunitMode = true,
                selectedSplitType = equalSplitType
            )
            handler.recalculateEntitySplits()

            val subunit = uiState.value.entitySplits.first { it.userId == subunitCoupleId }
            // Subunit gets 100% since it's the only entity
            assertEquals(10000L, subunit.amountCents)
            // Weighted intra-split: 7000 and 3000
            val member1 = subunit.entityMembers.first { it.userId == coupleMember1 }
            val member2 = subunit.entityMembers.first { it.userId == coupleMember2 }
            assertEquals(7000L, member1.amountCents)
            assertEquals(3000L, member2.amountCents)
        }
    }

    @Nested
    inner class DisableSubunitMode {

        @Test
        fun `disables subunit mode when active`() = runTest {
            uiState.value = baseEntityState.copy(isSubunitMode = true)
            handler.bind(uiState, actions, this)

            handler.disableSubunitMode()

            assertFalse(uiState.value.isSubunitMode)
        }

        @Test
        fun `is no-op when subunit mode is already false`() = runTest {
            uiState.value = baseEntityState.copy(isSubunitMode = false)
            handler.bind(uiState, actions, this)

            handler.disableSubunitMode()

            assertFalse(uiState.value.isSubunitMode)
        }

        @Test
        fun `clears split error when disabling`() = runTest {
            uiState.value = baseEntityState.copy(
                isSubunitMode = true,
                splitError = UiText.DynamicString("Some error")
            )
            handler.bind(uiState, actions, this)

            handler.disableSubunitMode()

            assertEquals(null, uiState.value.splitError)
        }

        @Test
        fun `does not reset entity splits exclusions`() = runTest {
            val excludedEntitySplits = baseEntityState.entitySplits.map { entity ->
                entity.copy(isExcluded = entity.userId != subunitCoupleId)
            }.toImmutableList()
            uiState.value = baseEntityState.copy(
                isSubunitMode = true,
                entitySplits = excludedEntitySplits
            )
            handler.bind(uiState, actions, this)

            handler.disableSubunitMode()

            // Entity exclusions from the prior SUBUNIT pool selection are preserved
            assertTrue(uiState.value.entitySplits.first { it.userId == soloMember1 }.isExcluded)
            assertTrue(uiState.value.entitySplits.first { it.userId == soloMember2 }.isExcluded)
            assertFalse(uiState.value.entitySplits.first { it.userId == subunitCoupleId }.isExcluded)
        }
    }

    @Nested
    inner class ApplySubunitPoolDefault {

        @Test
        fun `enables subunit mode`() = runTest {
            uiState.value = baseEntityState.copy(isSubunitMode = false)
            handler.bind(uiState, actions, this)

            handler.applySubunitPoolDefault(subunitCoupleId)

            assertTrue(uiState.value.isSubunitMode)
        }

        @Test
        fun `excludes all entities except the matching subunit`() = runTest {
            uiState.value = baseEntityState
            handler.bind(uiState, actions, this)

            handler.applySubunitPoolDefault(subunitCoupleId)

            val entitySplits = uiState.value.entitySplits
            assertTrue(entitySplits.first { it.userId == subunitCoupleId }.isExcluded.not())
            assertTrue(entitySplits.first { it.userId == soloMember1 }.isExcluded)
            assertTrue(entitySplits.first { it.userId == soloMember2 }.isExcluded)
        }

        @Test
        fun `clears all share locks`() = runTest {
            uiState.value = baseEntityState.copy(
                entitySplits = baseEntityState.entitySplits.map { it.copy(isShareLocked = true) }
                    .toImmutableList()
            )
            handler.bind(uiState, actions, this)

            handler.applySubunitPoolDefault(subunitCoupleId)

            assertTrue(uiState.value.entitySplits.none { it.isShareLocked })
        }

        @Test
        fun `clears split error`() = runTest {
            uiState.value = baseEntityState.copy(splitError = UiText.DynamicString("Some error"))
            handler.bind(uiState, actions, this)

            handler.applySubunitPoolDefault(subunitCoupleId)

            assertEquals(null, uiState.value.splitError)
        }

        @Test
        fun `is no-op when poolSubunitId is null`() = runTest {
            uiState.value = baseEntityState.copy(isSubunitMode = false)
            handler.bind(uiState, actions, this)

            handler.applySubunitPoolDefault(null)

            assertFalse(uiState.value.isSubunitMode)
        }

        @Test
        fun `is no-op when entitySplits is empty (subunit-less group)`() = runTest {
            uiState.value = baseEntityState.copy(
                isSubunitMode = false,
                entitySplits = persistentListOf()
            )
            handler.bind(uiState, actions, this)

            handler.applySubunitPoolDefault(subunitCoupleId)

            assertFalse(uiState.value.isSubunitMode)
        }
    }
}
