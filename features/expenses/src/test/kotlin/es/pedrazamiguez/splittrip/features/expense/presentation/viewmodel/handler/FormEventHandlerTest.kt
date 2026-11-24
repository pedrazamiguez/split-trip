package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChefHat
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewType
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.ManualReceiptQuota
import es.pedrazamiguez.splittrip.domain.usecase.expense.AttachReceiptUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ManualReceiptQuotaUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ManualReceiptQuotaUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentStatusUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubcategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormEventHandlerTest {

    private lateinit var handler: FormEventHandler
    private lateinit var addExpenseUiMapper: AddExpenseUiMapper
    private lateinit var addExpenseOptionsUiMapper: AddExpenseOptionsUiMapper
    private lateinit var attachReceiptUseCase: AttachReceiptUseCase
    private lateinit var featureGateService: FeatureGateService
    private lateinit var manualReceiptQuotaUiMapper: ManualReceiptQuotaUiMapper
    private lateinit var telemetryTracker: TelemetryTracker
    private lateinit var manualQuotaFlow: MutableStateFlow<ManualReceiptQuota>
    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var actions: MutableSharedFlow<AddExpenseUiAction>
    private val capturedPostActions = mutableListOf<FormPostAction>()

    private val cashMethod = PaymentMethodUiModel(id = PaymentMethod.CASH.name, displayText = "Cash")
    private val cardMethod = PaymentMethodUiModel(id = PaymentMethod.CREDIT_CARD.name, displayText = "Credit Card")
    private val groupSource = FundingSourceUiModel(id = PayerType.GROUP.name, displayText = "Group Pocket")
    private val userSource = FundingSourceUiModel(id = PayerType.USER.name, displayText = "Your Money")
    private val finishedStatus = PaymentStatusUiModel(id = PaymentStatus.FINISHED.name, displayText = "Finished")
    private val scheduledStatus = PaymentStatusUiModel(id = PaymentStatus.SCHEDULED.name, displayText = "Scheduled")
    private val refundableStatus = PaymentStatusUiModel(id = PaymentStatus.REFUNDABLE.name, displayText = "Refundable")
    private val cancelledStatus = PaymentStatusUiModel(id = PaymentStatus.CANCELLED.name, displayText = "Cancelled")
    private val foodCategory = CategoryUiModel(id = "FOOD", displayText = "Food")
    private val otherCategory = CategoryUiModel(id = "OTHER", displayText = "Other")

    @BeforeEach
    fun setUp() {
        addExpenseUiMapper = mockk(relaxed = true)
        addExpenseOptionsUiMapper = mockk(relaxed = true)
        attachReceiptUseCase = mockk(relaxed = true)
        manualQuotaFlow = MutableStateFlow(
            ManualReceiptQuota(usedAttachments = 0, monthlyFreeLimit = 5, isProTier = false)
        )
        featureGateService = mockk(relaxed = true) {
            every { observeManualReceiptQuota() } returns manualQuotaFlow
        }
        manualReceiptQuotaUiMapper = mockk(relaxed = true) {
            every { mapQuotaToUiModel(any()) } answers {
                val q = firstArg<ManualReceiptQuota>()
                ManualReceiptQuotaUiModel(
                    isManualReceiptGated = !q.canAttach,
                    isManualReceiptProFeature = q.requiresUpgrade,
                    remainingAttachmentsLabel = q.remainingAttachments?.let { "$it left" }
                )
            }
        }
        telemetryTracker = mockk(relaxed = true)
        capturedPostActions.clear()

        every { addExpenseOptionsUiMapper.mapPaymentStatuses(any(), any(), any()) } answers {
            val method = secondArg<PaymentMethod?>()
            val isEdit = thirdArg<Boolean>()
            val statuses = if (method == PaymentMethod.CASH) {
                mutableListOf(finishedStatus, refundableStatus)
            } else {
                mutableListOf(finishedStatus, scheduledStatus, refundableStatus)
            }
            if (isEdit) {
                statuses.add(cancelledStatus)
            }
            statuses.toImmutableList()
        }

        handler = FormEventHandler(
            addExpenseUiMapper = addExpenseUiMapper,
            addExpenseOptionsUiMapper = addExpenseOptionsUiMapper,
            attachReceiptUseCase = attachReceiptUseCase,
            featureGateService = featureGateService,
            manualReceiptQuotaUiMapper = manualReceiptQuotaUiMapper,
            telemetryTracker = telemetryTracker
        )
        handler.setFormPostCallback { capturedPostActions.add(it) }

        uiState = MutableStateFlow(
            AddExpenseUiState(
                paymentMethods = persistentListOf(cashMethod, cardMethod),
                fundingSources = persistentListOf(groupSource, userSource),
                availablePaymentStatuses = persistentListOf(finishedStatus, scheduledStatus, refundableStatus),
                availableCategories = persistentListOf(foodCategory, otherCategory),
                selectedFundingSource = groupSource
            )
        )
        actions = MutableSharedFlow()
        // UnconfinedTestDispatcher runs coroutines eagerly so no advanceUntilIdle() is needed
        handler.bind(uiState, actions, TestScope(UnconfinedTestDispatcher()))
    }

    @Nested
    inner class TitleChanged {

        @Test
        fun `updates title and clears validation error`() = runTest {
            uiState.value = uiState.value.copy(isTitleValid = false, error = UiText.DynamicString("err"))

            handler.handleTitleChanged("Dinner")

            assertEquals("Dinner", uiState.value.expenseTitle)
            assertTrue(uiState.value.isTitleValid)
            assertNull(uiState.value.error)
        }
    }

    @Nested
    inner class SourceAmountChanged {

        @Test
        fun `updates amount and clears validation error`() = runTest {
            uiState.value = uiState.value.copy(isAmountValid = false, error = UiText.DynamicString("err"))

            handler.handleSourceAmountChanged("42.50")

            assertEquals("42.50", uiState.value.sourceAmount)
            assertTrue(uiState.value.isAmountValid)
            assertNull(uiState.value.error)
        }

        @Test
        fun `blank amount is considered valid (user still typing)`() = runTest {
            handler.handleSourceAmountChanged("")
            assertTrue(uiState.value.isAmountValid)
        }

        @Test
        fun `non-numeric text sets isAmountValid false`() = runTest {
            handler.handleSourceAmountChanged("abc")
            assertFalse(uiState.value.isAmountValid)
        }

        @Test
        fun `zero amount sets isAmountValid false`() = runTest {
            handler.handleSourceAmountChanged("0")
            assertFalse(uiState.value.isAmountValid)
        }

        @Test
        fun `negative amount sets isAmountValid false`() = runTest {
            handler.handleSourceAmountChanged("-10")
            assertFalse(uiState.value.isAmountValid)
        }

        @Test
        fun `emits RecalculateAfterAmount with locked rate`() = runTest {
            uiState.value = uiState.value.copy(isExchangeRateLocked = true)

            handler.handleSourceAmountChanged("100")

            val action = capturedPostActions.single()
            assertTrue(action is FormPostAction.RecalculateAfterAmount)
            assertTrue((action as FormPostAction.RecalculateAfterAmount).isExchangeRateLocked)
        }

        @Test
        fun `emits RecalculateAfterAmount with unlocked rate`() = runTest {
            uiState.value = uiState.value.copy(isExchangeRateLocked = false)

            handler.handleSourceAmountChanged("100")

            val action = capturedPostActions.single()
            assertTrue(action is FormPostAction.RecalculateAfterAmount)
            assertFalse((action as FormPostAction.RecalculateAfterAmount).isExchangeRateLocked)
        }

        @Test
        fun `emits RecalculateAfterAmount with isCash true when CASH is selected`() = runTest {
            uiState.value = uiState.value.copy(selectedPaymentMethod = cashMethod)

            handler.handleSourceAmountChanged("50")

            val action = capturedPostActions.single() as FormPostAction.RecalculateAfterAmount
            assertTrue(action.isCash)
        }

        @Test
        fun `emits RecalculateAfterAmount with isCash false when non-CASH is selected`() = runTest {
            uiState.value = uiState.value.copy(selectedPaymentMethod = cardMethod)

            handler.handleSourceAmountChanged("50")

            val action = capturedPostActions.single() as FormPostAction.RecalculateAfterAmount
            assertFalse(action.isCash)
        }
    }

    @Nested
    inner class PaymentMethodSelected {

        @Test
        fun `selects payment method and emits callback`() = runTest {
            handler.handlePaymentMethodSelected(cardMethod.id)

            assertEquals(cardMethod, uiState.value.selectedPaymentMethod)
            val action = capturedPostActions.single() as FormPostAction.PaymentMethodChanged
            assertFalse(action.isCash)
            assertTrue(action.isGroupPocket) // default selectedFundingSource is GROUP
        }

        @Test
        fun `detects CASH payment method`() = runTest {
            handler.handlePaymentMethodSelected(cashMethod.id)

            assertEquals(cashMethod, uiState.value.selectedPaymentMethod)
            val action = capturedPostActions.single() as FormPostAction.PaymentMethodChanged
            assertTrue(action.isCash)
        }

        @Test
        fun `changing payment method preserves CANCELLED in edit mode`() = runTest {
            uiState.value = uiState.value.copy(
                isEditMode = true,
                selectedPaymentMethod = cardMethod,
                selectedPaymentStatus = cancelledStatus,
                availablePaymentStatuses = persistentListOf(
                    finishedStatus,
                    scheduledStatus,
                    refundableStatus,
                    cancelledStatus
                )
            )

            handler.handlePaymentMethodSelected(cashMethod.id)

            assertEquals(cancelledStatus, uiState.value.selectedPaymentStatus)
            assertTrue(uiState.value.showRefundDateSection)
        }

        @Test
        fun `ignores unknown method ID`() = runTest {
            val before = uiState.value.selectedPaymentMethod

            handler.handlePaymentMethodSelected("NONEXISTENT")

            assertEquals(before, uiState.value.selectedPaymentMethod)
            assertTrue(capturedPostActions.isEmpty())
        }

        @Test
        fun `detects non-group funding source`() = runTest {
            uiState.value = uiState.value.copy(selectedFundingSource = userSource)

            handler.handlePaymentMethodSelected(cashMethod.id)

            val action = capturedPostActions.single() as FormPostAction.PaymentMethodChanged
            assertFalse(action.isGroupPocket)
        }

        @Test
        fun `switching to CASH when SCHEDULED is selected resets status to FINISHED and hides due date section`() =
            runTest {
                uiState.value = uiState.value.copy(
                    selectedPaymentStatus = scheduledStatus,
                    showDueDateSection = true,
                    dueDateMillis = 12345L,
                    formattedDueDate = "1 Jan 2026"
                )

                handler.handlePaymentMethodSelected(cashMethod.id)

                assertEquals(finishedStatus, uiState.value.selectedPaymentStatus)
                assertFalse(uiState.value.showDueDateSection)
                assertNull(uiState.value.dueDateMillis)
                assertEquals("", uiState.value.formattedDueDate)
            }

        @Test
        fun `switching to CASH updates availablePaymentStatuses excluding SCHEDULED`() = runTest {
            handler.handlePaymentMethodSelected(cashMethod.id)

            val statuses = uiState.value.availablePaymentStatuses
            assertTrue(finishedStatus in statuses)
            assertTrue(refundableStatus in statuses)
            assertTrue(scheduledStatus !in statuses)
        }

        @Test
        fun `switching from CASH to non-cash restores SCHEDULED in availablePaymentStatuses`() = runTest {
            uiState.value = uiState.value.copy(
                selectedPaymentMethod = cashMethod,
                availablePaymentStatuses = persistentListOf(finishedStatus, refundableStatus)
            )

            handler.handlePaymentMethodSelected(cardMethod.id)

            val statuses = uiState.value.availablePaymentStatuses
            assertTrue(scheduledStatus in statuses)
            assertTrue(finishedStatus in statuses)
            assertTrue(refundableStatus in statuses)
        }

        @Test
        fun `switching to CASH when REFUNDABLE is selected preserves REFUNDABLE and keeps due date section visible`() =
            runTest {
                uiState.value = uiState.value.copy(
                    selectedPaymentStatus = refundableStatus,
                    showDueDateSection = true,
                    dueDateMillis = 12345L,
                    formattedDueDate = "1 Jan 2026"
                )

                handler.handlePaymentMethodSelected(cashMethod.id)

                assertEquals(refundableStatus, uiState.value.selectedPaymentStatus)
                assertTrue(uiState.value.showDueDateSection)
                assertEquals(12345L, uiState.value.dueDateMillis)
                assertEquals("1 Jan 2026", uiState.value.formattedDueDate)
            }
    }

    @Nested
    inner class FundingSourceSelected {

        @Test
        fun `selects GROUP funding source`() = runTest {
            uiState.value = uiState.value.copy(selectedFundingSource = userSource)

            handler.handleFundingSourceSelected(PayerType.GROUP.name)

            assertEquals(groupSource, uiState.value.selectedFundingSource)
            val action = capturedPostActions.single() as FormPostAction.FundingSourceChanged
            assertTrue(action.isGroupPocket)
        }

        @Test
        fun `selects USER funding source`() = runTest {
            handler.handleFundingSourceSelected(PayerType.USER.name)

            assertEquals(userSource, uiState.value.selectedFundingSource)
            val action = capturedPostActions.single() as FormPostAction.FundingSourceChanged
            assertFalse(action.isGroupPocket)
        }

        @Test
        fun `resets contribution scope when switching away from Your Money`() = runTest {
            uiState.value = uiState.value.copy(
                selectedFundingSource = userSource,
                contributionScope = PayerType.GROUP,
                selectedContributionSubunitId = "sub-1"
            )

            handler.handleFundingSourceSelected(PayerType.GROUP.name)

            assertEquals(PayerType.USER, uiState.value.contributionScope)
            assertNull(uiState.value.selectedContributionSubunitId)
        }

        @Test
        fun `preserves contribution scope when staying on Your Money`() = runTest {
            uiState.value = uiState.value.copy(
                selectedFundingSource = userSource,
                contributionScope = PayerType.SUBUNIT,
                selectedContributionSubunitId = "sub-1"
            )

            handler.handleFundingSourceSelected(PayerType.USER.name)

            assertEquals(PayerType.SUBUNIT, uiState.value.contributionScope)
            assertEquals("sub-1", uiState.value.selectedContributionSubunitId)
        }

        @Test
        fun `ignores unknown funding source ID`() = runTest {
            val before = uiState.value.selectedFundingSource

            handler.handleFundingSourceSelected("NONEXISTENT")

            assertEquals(before, uiState.value.selectedFundingSource)
            assertTrue(capturedPostActions.isEmpty())
        }
    }

    @Nested
    inner class ContributionScopeSelected {

        @Test
        fun `updates contribution scope and subunit ID`() = runTest {
            handler.handleContributionScopeSelected(PayerType.SUBUNIT, "sub-1")

            assertEquals(PayerType.SUBUNIT, uiState.value.contributionScope)
            assertEquals("sub-1", uiState.value.selectedContributionSubunitId)
        }
    }

    @Nested
    inner class CategorySelected {

        @Test
        fun `selects category, resets subcategory, and maps available subcategories`() = runTest {
            val subcategoryUiModel = SubcategoryUiModel(
                subcategory = ExpenseSubcategory.RESTAURANT,
                name = UiText.DynamicString("Restaurant"),
                icon = TablerIcons.Outline.ChefHat
            )
            every {
                addExpenseOptionsUiMapper.mapCategoriesForContext(
                    context = GroupContext.TRAVEL,
                    showAll = false,
                    alwaysInclude = ExpenseCategory.FOOD
                )
            } returns persistentListOf(foodCategory, otherCategory)
            every {
                addExpenseOptionsUiMapper.mapSubcategories(
                    category = ExpenseCategory.FOOD,
                    context = GroupContext.TRAVEL,
                    showAll = false,
                    alwaysInclude = null
                )
            } returns persistentListOf(subcategoryUiModel)

            uiState.value = uiState.value.copy(selectedSubcategory = subcategoryUiModel)

            handler.handleCategorySelected("FOOD")

            assertEquals(foodCategory, uiState.value.selectedCategory)
            assertNull(uiState.value.selectedSubcategory)
            assertEquals(1, uiState.value.availableSubcategories.size)
            assertEquals(subcategoryUiModel, uiState.value.availableSubcategories.first())
        }

        @Test
        fun `handleCategorySelected prunes out-of-context categories when selecting an in-context category`() =
            runTest {
                val healthCategory = CategoryUiModel(
                    id = ExpenseCategory.HEALTH_PERSONAL_CARE.name,
                    displayText = "Health & Care"
                )
                uiState.value = uiState.value.copy(
                    groupContext = GroupContext.TRAVEL,
                    showAllCategories = false,
                    availableCategories = persistentListOf(foodCategory, otherCategory, healthCategory),
                    selectedCategory = healthCategory
                )
                every {
                    addExpenseOptionsUiMapper.mapCategoriesForContext(
                        context = GroupContext.TRAVEL,
                        showAll = false,
                        alwaysInclude = ExpenseCategory.FOOD
                    )
                } returns persistentListOf(foodCategory, otherCategory)
                every {
                    addExpenseOptionsUiMapper.mapSubcategories(
                        category = ExpenseCategory.FOOD,
                        context = GroupContext.TRAVEL,
                        showAll = false,
                        alwaysInclude = null
                    )
                } returns persistentListOf()

                handler.handleCategorySelected(ExpenseCategory.FOOD.name)

                assertEquals(listOf(foodCategory, otherCategory), uiState.value.availableCategories)
                assertEquals(foodCategory, uiState.value.selectedCategory)
                assertFalse(uiState.value.showAllCategories)
            }

        @Test
        fun `ignores unknown category ID`() = runTest {
            val before = uiState.value.selectedCategory

            handler.handleCategorySelected("NONEXISTENT")

            assertEquals(before, uiState.value.selectedCategory)
        }

        @Test
        fun `handleCategorySelected auto-expands showAllCategories when outside availableCategories`() =
            runTest {
                val homeCategory = CategoryUiModel(id = "HOME", displayText = "Home")
                val rentSubcategory = SubcategoryUiModel(
                    subcategory = ExpenseSubcategory.RENT_MORTGAGE,
                    name = UiText.DynamicString("Rent"),
                    icon = TablerIcons.Outline.ChefHat
                )
                every {
                    addExpenseOptionsUiMapper.mapCategoriesForContext(
                        context = GroupContext.TRAVEL,
                        showAll = true,
                        alwaysInclude = null
                    )
                } returns persistentListOf(foodCategory, otherCategory, homeCategory)
                every {
                    addExpenseOptionsUiMapper.mapSubcategories(
                        category = ExpenseCategory.HOME,
                        context = GroupContext.TRAVEL,
                        showAll = true,
                        alwaysInclude = null
                    )
                } returns persistentListOf(rentSubcategory)

                handler.handleCategorySelected(ExpenseCategory.HOME.name)

                assertTrue(uiState.value.showAllCategories)
                assertEquals(homeCategory, uiState.value.selectedCategory)
                assertEquals(listOf(rentSubcategory), uiState.value.availableSubcategories)
            }

        @Test
        fun `handleToggleShowAllCategories toggles showAllCategories and refreshes categories and subcategories`() =
            runTest {
                val homeCategory = CategoryUiModel(id = "HOME", displayText = "Home")
                val restaurantSubcategory = SubcategoryUiModel(
                    subcategory = ExpenseSubcategory.RESTAURANT,
                    name = UiText.DynamicString("Restaurant"),
                    icon = TablerIcons.Outline.ChefHat
                )
                val streetFoodSubcategory = SubcategoryUiModel(
                    subcategory = ExpenseSubcategory.STREET_FOOD,
                    name = UiText.DynamicString("Street Food"),
                    icon = TablerIcons.Outline.ChefHat
                )
                uiState.value = uiState.value.copy(
                    groupContext = GroupContext.EVERYDAY,
                    selectedCategory = foodCategory,
                    selectedSubcategory = restaurantSubcategory,
                    showAllCategories = false
                )
                every {
                    addExpenseOptionsUiMapper.mapCategoriesForContext(
                        context = GroupContext.EVERYDAY,
                        showAll = true,
                        alwaysInclude = ExpenseCategory.FOOD
                    )
                } returns persistentListOf(foodCategory, otherCategory, homeCategory)
                every {
                    addExpenseOptionsUiMapper.mapSubcategories(
                        category = ExpenseCategory.FOOD,
                        context = GroupContext.EVERYDAY,
                        showAll = true,
                        alwaysInclude = ExpenseSubcategory.RESTAURANT
                    )
                } returns persistentListOf(restaurantSubcategory, streetFoodSubcategory)

                handler.handleToggleShowAllCategories()

                assertTrue(uiState.value.showAllCategories)
                assertEquals(listOf(foodCategory, otherCategory, homeCategory), uiState.value.availableCategories)
                assertEquals(
                    listOf(restaurantSubcategory, streetFoodSubcategory),
                    uiState.value.availableSubcategories
                )
            }

        @Test
        fun `handleToggleShowAllCategories off resets out-of-context selection to OTHER`() =
            runTest {
                val healthCategory = CategoryUiModel(
                    id = ExpenseCategory.HEALTH_PERSONAL_CARE.name,
                    displayText = "Health & Care"
                )
                val doctorSubcategory = SubcategoryUiModel(
                    subcategory = ExpenseSubcategory.DOCTOR_DENTIST,
                    name = UiText.DynamicString("Doctor"),
                    icon = TablerIcons.Outline.ChefHat
                )
                uiState.value = uiState.value.copy(
                    groupContext = GroupContext.TRAVEL,
                    showAllCategories = true,
                    availableCategories = persistentListOf(foodCategory, otherCategory, healthCategory),
                    selectedCategory = healthCategory,
                    selectedSubcategory = doctorSubcategory
                )
                every {
                    addExpenseOptionsUiMapper.mapCategoriesForContext(
                        context = GroupContext.TRAVEL,
                        showAll = false,
                        alwaysInclude = null
                    )
                } returns persistentListOf(foodCategory, otherCategory)
                every {
                    addExpenseOptionsUiMapper.mapSubcategories(
                        category = ExpenseCategory.OTHER,
                        context = GroupContext.TRAVEL,
                        showAll = false,
                        alwaysInclude = null
                    )
                } returns persistentListOf()

                handler.handleToggleShowAllCategories()

                assertFalse(uiState.value.showAllCategories)
                assertEquals(listOf(foodCategory, otherCategory), uiState.value.availableCategories)
                assertEquals(otherCategory, uiState.value.selectedCategory)
                assertNull(uiState.value.selectedSubcategory)
            }

        @Test
        fun `handleToggleShowAllCategories off clears out-of-context subcategory while keeping in-context category`() =
            runTest {
                val shoppingCategory = CategoryUiModel(
                    id = ExpenseCategory.SHOPPING.name,
                    displayText = "Shopping"
                )
                val giftsSubcategory = SubcategoryUiModel(
                    subcategory = ExpenseSubcategory.GIFTS,
                    name = UiText.DynamicString("Gifts"),
                    icon = TablerIcons.Outline.ChefHat
                )
                val pharmacySubcategory = SubcategoryUiModel(
                    subcategory = ExpenseSubcategory.PHARMACY_HEALTH,
                    name = UiText.DynamicString("Pharmacy & Health"),
                    icon = TablerIcons.Outline.ChefHat
                )
                uiState.value = uiState.value.copy(
                    groupContext = GroupContext.TRAVEL,
                    showAllCategories = true,
                    availableCategories = persistentListOf(foodCategory, shoppingCategory, otherCategory),
                    selectedCategory = shoppingCategory,
                    selectedSubcategory = giftsSubcategory
                )
                every {
                    addExpenseOptionsUiMapper.mapCategoriesForContext(
                        context = GroupContext.TRAVEL,
                        showAll = false,
                        alwaysInclude = null
                    )
                } returns persistentListOf(foodCategory, shoppingCategory, otherCategory)
                every {
                    addExpenseOptionsUiMapper.mapSubcategories(
                        category = ExpenseCategory.SHOPPING,
                        context = GroupContext.TRAVEL,
                        showAll = false,
                        alwaysInclude = null
                    )
                } returns persistentListOf(pharmacySubcategory)

                handler.handleToggleShowAllCategories()

                assertFalse(uiState.value.showAllCategories)
                assertEquals(shoppingCategory, uiState.value.selectedCategory)
                assertNull(uiState.value.selectedSubcategory)
                assertEquals(listOf(pharmacySubcategory), uiState.value.availableSubcategories)
            }
    }

    @Nested
    inner class SubcategorySelected {

        @Test
        fun `selects subcategory from available list`() = runTest {
            val subcategoryUiModel = SubcategoryUiModel(
                subcategory = ExpenseSubcategory.RESTAURANT,
                name = UiText.DynamicString("Restaurant"),
                icon = TablerIcons.Outline.ChefHat
            )
            uiState.value = uiState.value.copy(
                availableSubcategories = persistentListOf(subcategoryUiModel)
            )

            handler.handleSubcategorySelected(ExpenseSubcategory.RESTAURANT.name)

            assertEquals(subcategoryUiModel, uiState.value.selectedSubcategory)
        }

        @Test
        fun `clears subcategory when null passed`() = runTest {
            val subcategoryUiModel = SubcategoryUiModel(
                subcategory = ExpenseSubcategory.RESTAURANT,
                name = UiText.DynamicString("Restaurant"),
                icon = TablerIcons.Outline.ChefHat
            )
            uiState.value = uiState.value.copy(
                availableSubcategories = persistentListOf(subcategoryUiModel),
                selectedSubcategory = subcategoryUiModel
            )

            handler.handleSubcategorySelected(null)

            assertNull(uiState.value.selectedSubcategory)
        }
    }

    @Nested
    inner class SimpleFieldUpdates {

        @Test
        fun `updates vendor`() = runTest {
            handler.handleVendorChanged("Starbucks")
            assertEquals("Starbucks", uiState.value.vendor)
        }

        @Test
        fun `updates notes`() = runTest {
            handler.handleNotesChanged("Important note")
            assertEquals("Important note", uiState.value.notes)
        }

        @Test
        fun `sets receipt URI and attachment when use case succeeds`() = runTest {
            val attachment = ReceiptAttachment(
                localUri = "/data/user/0/receipts/abc.webp",
                mimeType = "image/webp",
                capturedAtMillis = 1716000000000L
            )
            coEvery { attachReceiptUseCase("content://image/1") } returns Result.success(attachment)

            handler.handleReceiptImageChanged("content://image/1")

            assertEquals("/data/user/0/receipts/abc.webp", uiState.value.receiptUri)
            assertEquals(attachment, uiState.value.receiptAttachment)
        }

        @Test
        fun `clears receipt URI when uri is null`() = runTest {
            uiState.value = uiState.value.copy(receiptUri = "content://image/1")

            handler.handleReceiptImageChanged(null)

            assertNull(uiState.value.receiptUri)
            assertNull(uiState.value.receiptAttachment)
        }
    }

    @Nested
    inner class PaymentStatusSelected {

        @Test
        fun `shows due date section for SCHEDULED`() = runTest {
            handler.handlePaymentStatusSelected(PaymentStatus.SCHEDULED.name)

            assertEquals(scheduledStatus, uiState.value.selectedPaymentStatus)
            assertTrue(uiState.value.showDueDateSection)
            assertTrue(uiState.value.isDueDateValid)
        }

        @Test
        fun `hides due date section for FINISHED`() = runTest {
            uiState.value = uiState.value.copy(
                showDueDateSection = true,
                dueDateMillis = 12345L,
                formattedDueDate = "2025-01-01"
            )

            handler.handlePaymentStatusSelected(PaymentStatus.FINISHED.name)

            assertEquals(finishedStatus, uiState.value.selectedPaymentStatus)
            assertFalse(uiState.value.showDueDateSection)
            assertNull(uiState.value.dueDateMillis)
            assertEquals("", uiState.value.formattedDueDate)
        }

        @Test
        fun `ignores unknown status ID`() = runTest {
            val before = uiState.value.selectedPaymentStatus

            handler.handlePaymentStatusSelected("NONEXISTENT")

            assertEquals(before, uiState.value.selectedPaymentStatus)
        }

        @Test
        fun `selecting CANCELLED status enables refund date section and sets default`() = runTest {
            uiState.value = uiState.value.copy(
                availablePaymentStatuses = persistentListOf(
                    finishedStatus,
                    scheduledStatus,
                    refundableStatus,
                    cancelledStatus
                )
            )

            handler.handlePaymentStatusSelected(PaymentStatus.CANCELLED.name)

            assertEquals(cancelledStatus, uiState.value.selectedPaymentStatus)
            assertTrue(uiState.value.showRefundDateSection)
            assertTrue(uiState.value.refundDateMillis != null)
        }

        @Test
        fun `selecting CANCELLED status hides dueDateSection`() = runTest {
            uiState.value = uiState.value.copy(
                availablePaymentStatuses = persistentListOf(
                    finishedStatus,
                    scheduledStatus,
                    refundableStatus,
                    cancelledStatus
                ),
                showDueDateSection = true,
                dueDateMillis = 12345L,
                formattedDueDate = "2026-04-01"
            )

            handler.handlePaymentStatusSelected(PaymentStatus.CANCELLED.name)

            assertFalse(uiState.value.showDueDateSection)
            assertNull(uiState.value.dueDateMillis)
            assertEquals("", uiState.value.formattedDueDate)
        }
    }

    @Nested
    inner class DueDateSelected {

        @Test
        fun `sets due date millis and formatted date`() = runTest {
            every { addExpenseUiMapper.formatDueDateForDisplay(12345L) } returns "Jan 1, 2025"

            handler.handleDueDateSelected(12345L)

            assertEquals(12345L, uiState.value.dueDateMillis)
            assertEquals("Jan 1, 2025", uiState.value.formattedDueDate)
            assertTrue(uiState.value.isDueDateValid)
        }
    }

    @Nested
    inner class RefundDateSelected {

        @Test
        fun `handleRefundDateSelected updates refundDateMillis and formattedRefundDate`() = runTest {
            every { addExpenseUiMapper.formatRefundDateForDisplay(1700000000000L) } returns "Nov 14, 2023"

            handler.handleRefundDateSelected(1700000000000L)

            assertEquals(1700000000000L, uiState.value.refundDateMillis)
            assertEquals("Nov 14, 2023", uiState.value.formattedRefundDate)
        }
    }

    @Nested
    inner class ManualReceiptQuotaGating {

        @Test
        fun `startObservingManualReceiptQuota updates state when quota changes`() = runTest {
            handler.startObservingManualReceiptQuota()
            advanceUntilIdle()

            assertFalse(uiState.value.isManualReceiptGated)
            assertFalse(uiState.value.isManualReceiptProFeature)
            assertEquals("5 left", uiState.value.manualReceiptRemainingLabel)

            manualQuotaFlow.value = ManualReceiptQuota(usedAttachments = 5, monthlyFreeLimit = 5, isProTier = false)
            advanceUntilIdle()

            assertTrue(uiState.value.isManualReceiptGated)
            assertTrue(uiState.value.isManualReceiptProFeature)
            assertEquals("0 left", uiState.value.manualReceiptRemainingLabel)
        }

        @Test
        fun `handleRequestReceiptPicker emits OpenReceiptPickerSource when Free user has remaining quota`() = runTest {
            val emitted = mutableListOf<AddExpenseUiAction>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                actions.collect { emitted.add(it) }
            }

            handler.handleRequestReceiptPicker()
            advanceUntilIdle()

            assertEquals(listOf(AddExpenseUiAction.OpenReceiptPickerSource), emitted)
            job.cancel()
        }

        @Test
        fun `handleRequestReceiptPicker in manual mode emits ShowProFeaturePreview and telemetry when exhausted`() =
            runTest {
                manualQuotaFlow.value = ManualReceiptQuota(
                    usedAttachments = 5,
                    monthlyFreeLimit = 5,
                    isProTier = false
                )
                val emitted = mutableListOf<AddExpenseUiAction>()
                val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                    actions.collect { emitted.add(it) }
                }

                handler.handleRequestReceiptPicker()
                advanceUntilIdle()

                assertEquals(
                    listOf(
                        AddExpenseUiAction.ShowProFeaturePreview(
                            ProFeaturePreviewPrompt(
                                previewType = ProFeaturePreviewType.UNLIMITED_RECEIPTS,
                                entrySource = MonetizationTelemetry.EntrySource.MANUAL_RECEIPT_ATTACHMENT
                            )
                        )
                    ),
                    emitted
                )
                verify(exactly = 1) {
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                        mapOf(
                            MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.LIMIT,
                            MonetizationTelemetry.Param.GATE_NAME to
                                GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS.name.lowercase(),
                            MonetizationTelemetry.Param.ENTRY_POINT to
                                MonetizationTelemetry.GateEntryPoint.MANUAL_RECEIPT_ATTACH,
                            MonetizationTelemetry.Param.UPGRADE_PROMPTED to true
                        )
                    )
                }
                job.cancel()
            }

        @Test
        fun `handleRequestReceiptPicker in AI mode bypasses manual quota check`() = runTest {
            manualQuotaFlow.value = ManualReceiptQuota(usedAttachments = 5, monthlyFreeLimit = 5, isProTier = false)
            uiState.value = uiState.value.copy(isAiModeActive = true)
            val emitted = mutableListOf<AddExpenseUiAction>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                actions.collect { emitted.add(it) }
            }

            handler.handleRequestReceiptPicker()
            advanceUntilIdle()

            assertEquals(listOf(AddExpenseUiAction.OpenReceiptPickerSource), emitted)
            job.cancel()
        }

        @Test
        fun `handleReceiptImageChanged in manual mode blocks when quota exhausted without attaching`() = runTest {
            manualQuotaFlow.value = ManualReceiptQuota(usedAttachments = 5, monthlyFreeLimit = 5, isProTier = false)
            val emitted = mutableListOf<AddExpenseUiAction>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                actions.collect { emitted.add(it) }
            }

            handler.handleReceiptImageChanged("content://receipt/1")
            advanceUntilIdle()

            coVerify(exactly = 0) { attachReceiptUseCase(any()) }
            assertTrue(emitted.single() is AddExpenseUiAction.ShowProFeaturePreview)
            verify(exactly = 1) {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                    mapOf(
                        MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.LIMIT,
                        MonetizationTelemetry.Param.GATE_NAME to
                            GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS.name.lowercase(),
                        MonetizationTelemetry.Param.ENTRY_POINT to
                            MonetizationTelemetry.GateEntryPoint.MANUAL_RECEIPT_ATTACH,
                        MonetizationTelemetry.Param.UPGRADE_PROMPTED to true
                    )
                )
            }
            job.cancel()
        }
    }
}
