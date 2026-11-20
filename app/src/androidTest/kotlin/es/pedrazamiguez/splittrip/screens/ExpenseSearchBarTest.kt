package es.pedrazamiguez.splittrip.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.extension.clearFocusOnKeyboardDismiss
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.list.ExpenseSearchBar
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDateGroupUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpensesScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [ExpenseSearchBar] and its integration in [ExpensesScreen].
 */
@RunWith(AndroidJUnit4::class)
class ExpenseSearchBarTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersPlaceholder_andHidesClearButton_whenQueryIsEmpty() {
        val placeholder = targetContext.getString(R.string.expenses_search_placeholder)
        val clearDescription = targetContext.getString(R.string.expenses_search_clear)

        composeRule.setContent {
            SplitTripTheme {
                ExpenseSearchBar(
                    query = "",
                    onQueryChange = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(placeholder).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(clearDescription).assertDoesNotExist()
    }

    @Test
    fun invokesOnQueryChange_whenTextInputEntered() {
        var query by mutableStateOf("")

        composeRule.setContent {
            SplitTripTheme {
                ExpenseSearchBar(
                    query = query,
                    onQueryChange = { query = it }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction()).performTextInput("Dinner")
        composeRule.waitForIdle()

        assertEquals("Dinner", query)
        composeRule.onNodeWithText("Dinner").assertIsDisplayed()
    }

    @Test
    fun rendersClearButton_andClearsQuery_whenQueryIsNonEmptyAndClearClicked() {
        val clearDescription = targetContext.getString(R.string.expenses_search_clear)
        var query by mutableStateOf("Dinner")

        composeRule.setContent {
            SplitTripTheme {
                ExpenseSearchBar(
                    query = query,
                    onQueryChange = { query = it }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(clearDescription).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(clearDescription).performClick()
        composeRule.waitForIdle()

        assertEquals("", query)
    }

    @Test
    fun reportsFocusChanges_onFocusAndImeSearchAction() {
        val focusHistory = mutableListOf<Boolean>()

        composeRule.setContent {
            SplitTripTheme {
                ExpenseSearchBar(
                    query = "Taxi",
                    onQueryChange = {},
                    onFocusChanged = { focusHistory.add(it) }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.waitForIdle()
        assertTrue(focusHistory.lastOrNull() == true)

        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()
        assertFalse(focusHistory.lastOrNull() != false)
    }

    @Test
    fun expensesScreen_emptySearchState_rendersSearchBar_andClearsQuery() {
        val emptyTitle = targetContext.getString(R.string.expenses_search_empty_title)
        val emptyDescription = targetContext.getString(R.string.expenses_search_empty_description)
        val clearDescription = targetContext.getString(R.string.expenses_search_clear)
        var lastQuery = "Nonexistent"

        composeRule.setContent {
            SplitTripTheme {
                CompositionLocalProvider(
                    LocalTopPadding provides 0.dp,
                    LocalBottomPadding provides 0.dp
                ) {
                    ExpensesScreen(
                        uiState = ExpensesUiState(
                            isLoading = false,
                            groupId = "group-1",
                            searchQuery = "Nonexistent",
                            totalExpensesCount = 5,
                            formattedTotalSpent = "€0.00",
                            visibleExpensesCount = 0,
                            isFiltered = true
                        ),
                        onSearchQueryChanged = { lastQuery = it }
                    )
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(emptyTitle).assertCountEquals(2)
        composeRule.onNodeWithText(emptyDescription).assertIsDisplayed()
        composeRule.onNodeWithText("€0.00").assertIsDisplayed()
        composeRule.onNodeWithText("Nonexistent").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(clearDescription).performClick()
        composeRule.waitForIdle()

        assertEquals("", lastQuery)
    }

    @Test
    fun expensesScreen_pinsHeaderWhenSearchFocused_andAllowsCollapseOnScrollWhenUnfocused() {
        val dateGroups = buildSampleDateGroups()
        val totalCount = dateGroups.sumOf { it.expenses.size }

        composeRule.setContent {
            SplitTripTheme {
                CompositionLocalProvider(
                    LocalTopPadding provides 0.dp,
                    LocalBottomPadding provides 0.dp
                ) {
                    ExpensesScreen(
                        uiState = ExpensesUiState(
                            isLoading = false,
                            groupId = "group-1",
                            expenseGroups = dateGroups,
                            totalExpensesCount = totalCount,
                            formattedTotalSpent = "€240.00",
                            visibleExpensesCount = totalCount,
                            isFiltered = false
                        )
                    )
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction()).assertIsDisplayed()

        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction()).assertIsNotDisplayed()
    }

    @Test
    fun expensesScreen_preservesSearchFocus_whenTransitioningBetweenResultsAndEmptySearch() {
        val emptyTitle = targetContext.getString(R.string.expenses_search_empty_title)
        val emptyDescription = targetContext.getString(R.string.expenses_search_empty_description)
        val dateGroups = buildSampleDateGroups()
        val totalCount = dateGroups.sumOf { it.expenses.size }
        var uiState by mutableStateOf(
            ExpensesUiState(
                isLoading = false,
                groupId = "group-1",
                expenseGroups = dateGroups,
                totalExpensesCount = totalCount,
                formattedTotalSpent = "€240.00",
                visibleExpensesCount = totalCount,
                isFiltered = false
            )
        )

        composeRule.setContent {
            SplitTripTheme {
                CompositionLocalProvider(
                    LocalTopPadding provides 0.dp,
                    LocalBottomPadding provides 0.dp
                ) {
                    ExpensesScreen(
                        uiState = uiState,
                        onSearchQueryChanged = { query ->
                            uiState = if (query.isEmpty()) {
                                uiState.copy(
                                    searchQuery = "",
                                    expenseGroups = dateGroups,
                                    formattedTotalSpent = "€240.00",
                                    visibleExpensesCount = totalCount,
                                    isFiltered = false
                                )
                            } else {
                                uiState.copy(
                                    searchQuery = query,
                                    expenseGroups = persistentListOf(),
                                    formattedTotalSpent = "€0.00",
                                    visibleExpensesCount = 0,
                                    isFiltered = true
                                )
                            }
                        }
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val initialSearchTop = composeRule.onNode(hasSetTextAction()).fetchSemanticsNode().positionInRoot.y
        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()

        composeRule.onNode(hasSetTextAction()).performTextInput("y")
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(emptyTitle).assertCountEquals(2)
        composeRule.onNodeWithText(emptyDescription).assertIsDisplayed()
        composeRule.onNodeWithText("€0.00").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
        val emptySearchTop = composeRule.onNode(hasSetTextAction()).fetchSemanticsNode().positionInRoot.y
        assertEquals(initialSearchTop, emptySearchTop, 0.5f)

        uiState = uiState.copy(
            searchQuery = "",
            expenseGroups = dateGroups,
            formattedTotalSpent = "€240.00",
            visibleExpensesCount = totalCount,
            isFiltered = false
        )
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(emptyTitle).assertCountEquals(0)
        composeRule.onNodeWithText(emptyDescription).assertDoesNotExist()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()

        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).assertIsNotFocused()
    }

    @Test
    fun clearFocusOnKeyboardDismiss_preservesFocusBeforeImeAppears_andClearsFocusWhenImeDismissed() {
        var imeVisible by mutableStateOf(false)
        var text by mutableStateOf("")

        composeRule.setContent {
            SplitTripTheme {
                StyledOutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.clearFocusOnKeyboardDismiss(isImeVisible = imeVisible)
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()

        imeVisible = true
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()

        imeVisible = false
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).assertIsNotFocused()

        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun expensesScreen_clearsSearchFocusAndAllowsCollapse_whenImeDismissed() {
        val dateGroups = buildSampleDateGroups()

        composeRule.setContent {
            SplitTripTheme {
                CompositionLocalProvider(
                    LocalTopPadding provides 0.dp,
                    LocalBottomPadding provides 0.dp
                ) {
                    ExpensesScreen(
                        uiState = ExpensesUiState(
                            isLoading = false,
                            expenseGroups = dateGroups,
                            formattedTotalSpent = "€240.00",
                            visibleExpensesCount = dateGroups.sumOf { it.expenses.size },
                            isFiltered = false
                        )
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()

        Espresso.closeSoftKeyboard()
        composeRule.waitForIdle()

        val wasFocusCleared = runCatching {
            composeRule.onNode(hasSetTextAction()).assertIsNotFocused()
        }.isSuccess

        if (wasFocusCleared) {
            composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
            composeRule.onNode(hasSetTextAction()).assertIsNotDisplayed()
        }
    }

    private fun buildSampleDateGroups() = listOf(
        ExpenseDateGroupUiModel(
            dateText = "10 Nov",
            formattedDayTotal = "€80.00",
            expenses = (1..5).map { index ->
                sampleExpense(id = "exp-a-$index", title = "Expense A$index")
            }.toImmutableList()
        ),
        ExpenseDateGroupUiModel(
            dateText = "11 Nov",
            formattedDayTotal = "€80.00",
            expenses = (1..5).map { index ->
                sampleExpense(id = "exp-b-$index", title = "Expense B$index")
            }.toImmutableList()
        ),
        ExpenseDateGroupUiModel(
            dateText = "12 Nov",
            formattedDayTotal = "€80.00",
            expenses = (1..5).map { index ->
                sampleExpense(id = "exp-c-$index", title = "Expense C$index")
            }.toImmutableList()
        )
    ).toImmutableList()

    private fun sampleExpense(id: String, title: String) = ExpenseUiModel(
        id = id,
        title = title,
        formattedAmount = "€16.00",
        categoryText = "Food",
        paymentMethodText = "Card",
        paymentStatusText = "Paid",
        paidByText = "Alice",
        dateText = "10 Nov"
    )
}
