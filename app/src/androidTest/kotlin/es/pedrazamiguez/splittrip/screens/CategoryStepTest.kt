package es.pedrazamiguez.splittrip.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.CategoryStep
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubcategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [CategoryStep].
 */
@RunWith(AndroidJUnit4::class)
class CategoryStepTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun buildCategories(
        categories: List<ExpenseCategory> = ExpenseCategory.forContext(GroupContext.TRAVEL)
    ): ImmutableList<CategoryUiModel> = categories.map { category ->
        CategoryUiModel(
            id = category.name,
            displayText = context.getString(category.toStringRes()),
            icon = category.toIconVector()
        )
    }.toImmutableList()

    private fun buildSubcategories(category: ExpenseCategory): ImmutableList<SubcategoryUiModel> =
        ExpenseSubcategory.forCategory(category, GroupContext.TRAVEL).map { subcategory ->
            SubcategoryUiModel(
                subcategory = subcategory,
                name = UiText.StringResource(subcategory.toStringRes()),
                icon = subcategory.toIconVector()
            )
        }.toImmutableList()

    @Test
    fun rendersCarouselAndContextualSubcategoryTray_forSelectedCategory() {
        val categories = buildCategories()
        val foodUiModel = categories.first { it.id == ExpenseCategory.FOOD.name }
        val foodSubcategories = buildSubcategories(ExpenseCategory.FOOD)
        val foodLabel = context.getString(ExpenseCategory.FOOD.toStringRes())
        val restaurantLabel = context.getString(ExpenseSubcategory.RESTAURANT.toStringRes())

        val uiState = AddExpenseUiState(
            availableCategories = categories,
            selectedCategory = foodUiModel,
            availableSubcategories = foodSubcategories,
            selectedSubcategory = null
        )

        composeRule.setContent {
            SplitTripTheme {
                CategoryStep(uiState = uiState, onEvent = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasText(foodLabel).and(isSelected())).assertIsDisplayed()
        composeRule.onAllNodesWithText(foodLabel).assertCountEquals(2)
        composeRule.onNodeWithText(restaurantLabel).assertIsDisplayed()
    }

    @Test
    fun selectingDifferentCategory_emitsCategorySelectedEvent_andUpdatesTray() {
        val categories = buildCategories()
        val foodUiModel = categories.first { it.id == ExpenseCategory.FOOD.name }
        val transportLabel = context.getString(ExpenseCategory.TRANSPORT.toStringRes())
        val emittedEvents = mutableListOf<AddExpenseUiEvent>()

        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    AddExpenseUiState(
                        availableCategories = categories,
                        selectedCategory = foodUiModel,
                        availableSubcategories = buildSubcategories(ExpenseCategory.FOOD)
                    )
                )
            }
            SplitTripTheme {
                CategoryStep(
                    uiState = uiState,
                    onEvent = { event ->
                        emittedEvents.add(event)
                        if (event is AddExpenseUiEvent.CategorySelected) {
                            val selectedCategory = ExpenseCategory.fromString(event.categoryId)
                            uiState = uiState.copy(
                                selectedCategory = categories.first { it.id == event.categoryId },
                                availableSubcategories = buildSubcategories(selectedCategory),
                                selectedSubcategory = null
                            )
                        }
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(transportLabel).performClick()
        composeRule.waitForIdle()

        assertTrue(emittedEvents.contains(AddExpenseUiEvent.CategorySelected(ExpenseCategory.TRANSPORT.name)))
        composeRule.onAllNodesWithText(transportLabel).assertCountEquals(2)
    }

    @Test
    fun tappingSelectedSubcategoryStamp_emitsNullToDeselect() {
        val categories = buildCategories()
        val foodUiModel = categories.first { it.id == ExpenseCategory.FOOD.name }
        val foodSubcategories = buildSubcategories(ExpenseCategory.FOOD)
        val restaurantUiModel = foodSubcategories.first { it.subcategory == ExpenseSubcategory.RESTAURANT }
        val restaurantLabel = context.getString(ExpenseSubcategory.RESTAURANT.toStringRes())
        val emittedEvents = mutableListOf<AddExpenseUiEvent>()

        val uiState = AddExpenseUiState(
            availableCategories = categories,
            selectedCategory = foodUiModel,
            availableSubcategories = foodSubcategories,
            selectedSubcategory = restaurantUiModel
        )

        composeRule.setContent {
            SplitTripTheme {
                CategoryStep(
                    uiState = uiState,
                    onEvent = { emittedEvents.add(it) }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(restaurantLabel).performClick()
        composeRule.waitForIdle()

        assertTrue(emittedEvents.contains(AddExpenseUiEvent.SubcategorySelected(null)))
    }

    @Test
    fun togglingShowAllCategoriesChip_emitsToggleEvent() {
        val categories = buildCategories()
        val showAllLabel = context.getString(R.string.category_show_all_toggle)
        val emittedEvents = mutableListOf<AddExpenseUiEvent>()

        val uiState = AddExpenseUiState(
            availableCategories = categories,
            selectedCategory = categories.first(),
            availableSubcategories = buildSubcategories(ExpenseCategory.TRANSPORT)
        )

        composeRule.setContent {
            SplitTripTheme {
                CategoryStep(
                    uiState = uiState,
                    onEvent = { emittedEvents.add(it) }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(showAllLabel).performClick()
        composeRule.waitForIdle()

        assertTrue(emittedEvents.contains(AddExpenseUiEvent.ToggleShowAllCategories))
    }

    @Test
    fun togglingShowAllCategories_preservesHorizontalScrollPosition() {
        val travelCategories = buildCategories(ExpenseCategory.forContext(GroupContext.TRAVEL))
        val allCategories = buildCategories(ExpenseCategory.entries)
        val showAllLabel = context.getString(R.string.category_show_all_toggle)
        val otherLabel = context.getString(ExpenseCategory.OTHER.toStringRes())

        composeRule.setContent {
            var showAll by remember { mutableStateOf(false) }
            val uiState = AddExpenseUiState(
                availableCategories = if (showAll) allCategories else travelCategories,
                selectedCategory = travelCategories.first(),
                availableSubcategories = buildSubcategories(ExpenseCategory.TRANSPORT),
                showAllCategories = showAll
            )
            SplitTripTheme {
                CategoryStep(
                    uiState = uiState,
                    onEvent = { event ->
                        if (event is AddExpenseUiEvent.ToggleShowAllCategories) {
                            showAll = !showAll
                        }
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(otherLabel).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(showAllLabel).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(otherLabel).assertIsDisplayed()
    }

    @Test
    fun rendersCleanlyOnNarrow320dpScreen_withoutClippingOrCrash() {
        val categories = buildCategories(ExpenseCategory.entries)
        val healthCategory = categories.first { it.id == ExpenseCategory.HEALTH_PERSONAL_CARE.name }
        val healthSubcategories = ExpenseSubcategory.forCategory(ExpenseCategory.HEALTH_PERSONAL_CARE).map {
            SubcategoryUiModel(
                subcategory = it,
                name = UiText.StringResource(it.toStringRes()),
                icon = it.toIconVector()
            )
        }.toImmutableList()
        val headerTitle = context.getString(R.string.add_expense_category_title)
        val healthLabel = context.getString(ExpenseCategory.HEALTH_PERSONAL_CARE.toStringRes())

        val uiState = AddExpenseUiState(
            availableCategories = categories,
            selectedCategory = healthCategory,
            availableSubcategories = healthSubcategories,
            showAllCategories = true
        )

        composeRule.setContent {
            SplitTripTheme {
                Box(modifier = Modifier.width(320.dp)) {
                    CategoryStep(uiState = uiState, onEvent = {})
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(headerTitle).assertIsDisplayed()
        composeRule.onAllNodesWithText(healthLabel).assertCountEquals(2)
    }
}
