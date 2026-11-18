package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SplitInputConfigTest {

    private val sampleSplit = SplitUiModel(
        userId = "user-1",
        displayName = "Alice",
        amountCents = 2500L,
        formattedAmount = "25.00 €",
        amountInput = "25.00",
        percentageInput = "50"
    )

    @Test
    fun `resolveSplitInputConfig returns percent configuration when isPercentMode is true`() {
        var amountCallbackCalled = false
        var percentageCallbackValue: String? = null

        val config = resolveSplitInputConfig(
            split = sampleSplit,
            isPercentMode = true,
            currencySymbol = "EUR",
            onAmountChanged = { amountCallbackCalled = true },
            onPercentageChanged = { percentageCallbackValue = it }
        )

        assertEquals("50", config.value)
        assertEquals(R.string.add_expense_split_percentage_label, config.unitRes)
        assertNull(config.unitLiteral)
        assertEquals(R.string.add_expense_split_percentage_label, config.fieldDescriptionRes)

        config.onValueChange("60")
        assertEquals("60", percentageCallbackValue)
        assertEquals(false, amountCallbackCalled)
    }

    @Test
    fun `resolveSplitInputConfig returns exact amount configuration when isPercentMode is false`() {
        var amountCallbackValue: String? = null
        var percentageCallbackCalled = false

        val config = resolveSplitInputConfig(
            split = sampleSplit,
            isPercentMode = false,
            currencySymbol = "EUR",
            onAmountChanged = { amountCallbackValue = it },
            onPercentageChanged = { percentageCallbackCalled = true }
        )

        assertEquals("25.00", config.value)
        assertNull(config.unitRes)
        assertEquals("EUR", config.unitLiteral)
        assertEquals(R.string.add_expense_split_amount_label, config.fieldDescriptionRes)

        config.onValueChange("30.00")
        assertEquals("30.00", amountCallbackValue)
        assertEquals(false, percentageCallbackCalled)
    }

    @Test
    fun `splitLockDescriptionRes returns expected string resource for locked and unlocked states`() {
        assertEquals(R.string.add_expense_split_share_unlock, splitLockDescriptionRes(isLocked = true))
        assertEquals(R.string.add_expense_split_share_lock, splitLockDescriptionRes(isLocked = false))
    }
}
