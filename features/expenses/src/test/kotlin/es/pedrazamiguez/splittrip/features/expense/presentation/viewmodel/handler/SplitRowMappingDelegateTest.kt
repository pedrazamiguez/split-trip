package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import es.pedrazamiguez.splittrip.domain.service.split.impl.SplitPreviewServiceImpl
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SplitRowMappingDelegate].
 *
 * Uses real domain services for calculation correctness; only
 * [LocaleProvider] is mocked (fixed to US locale).
 */
class SplitRowMappingDelegateTest {

    private lateinit var delegate: SplitRowMappingDelegate

    private fun makeSplit(
        userId: String,
        amountCents: Long = 0L,
        amountInput: String = "",
        percentageInput: String = "",
        isShareLocked: Boolean = false,
        isExcluded: Boolean = false
    ) = SplitUiModel(
        userId = userId,
        displayName = userId,
        amountCents = amountCents,
        amountInput = amountInput,
        percentageInput = percentageInput,
        isShareLocked = isShareLocked,
        isExcluded = isExcluded
    )

    @BeforeEach
    fun setUp() {
        val localeProvider = mockk<LocaleProvider>()
        every { localeProvider.getCurrentLocale() } returns Locale.US

        delegate = SplitRowMappingDelegate(
            splitCalculatorFactory = ExpenseSplitCalculatorFactory(ExpenseCalculatorServiceImpl()),
            splitPreviewService = SplitPreviewServiceImpl(),
            formattingHelper = FormattingHelper(localeProvider)
        )
    }

    // ── applyExactAmountUpdate ───────────────────────────────────────────

    @Nested
    inner class ApplyExactAmountUpdate {

        @Test
        fun `edited user is locked at typed value`() {
            val splits = listOf(makeSplit("u1"), makeSplit("u2"), makeSplit("u3"))

            val result = delegate.applyExactAmountUpdate(
                splits = splits,
                editedUserId = "u1",
                typedAmount = "40",
                typedCents = 4000L,
                sourceAmountCents = 10000L,
                currencyCode = "EUR",
                decimalDigits = 2
            )

            val u1 = result.first { it.userId == "u1" }
            assertEquals(4000L, u1.amountCents)
            assertEquals("40", u1.amountInput)
            assertTrue(u1.isShareLocked)
        }

        @Test
        fun `remainder is distributed evenly to unlocked users`() {
            val splits = listOf(makeSplit("u1"), makeSplit("u2"), makeSplit("u3"))

            val result = delegate.applyExactAmountUpdate(
                splits = splits,
                editedUserId = "u1",
                typedAmount = "40",
                typedCents = 4000L,
                sourceAmountCents = 10000L,
                currencyCode = "EUR",
                decimalDigits = 2
            )

            val otherTotal = result.filter { it.userId != "u1" }.sumOf { it.amountCents }
            assertEquals(6000L, otherTotal)
        }

        @Test
        fun `locked users are kept unchanged`() {
            val splits = listOf(
                makeSplit("u1"),
                makeSplit("u2", amountCents = 3000L, isShareLocked = true),
                makeSplit("u3")
            )

            val result = delegate.applyExactAmountUpdate(
                splits = splits,
                editedUserId = "u1",
                typedAmount = "50",
                typedCents = 5000L,
                sourceAmountCents = 10000L,
                currencyCode = "EUR",
                decimalDigits = 2
            )

            assertEquals(3000L, result.first { it.userId == "u2" }.amountCents)
        }

        @Test
        fun `excluded users are not affected`() {
            val splits = listOf(
                makeSplit("u1"),
                makeSplit("u2", isExcluded = true),
                makeSplit("u3")
            )

            val result = delegate.applyExactAmountUpdate(
                splits = splits,
                editedUserId = "u1",
                typedAmount = "60",
                typedCents = 6000L,
                sourceAmountCents = 10000L,
                currencyCode = "EUR",
                decimalDigits = 2
            )

            val u2 = result.first { it.userId == "u2" }
            assertTrue(u2.isExcluded)
            assertEquals(0L, u2.amountCents)
        }

        @Test
        fun `total equals source amount after edit`() {
            val splits = listOf(makeSplit("u1"), makeSplit("u2"), makeSplit("u3"))

            val result = delegate.applyExactAmountUpdate(
                splits = splits,
                editedUserId = "u1",
                typedAmount = "33.33",
                typedCents = 3333L,
                sourceAmountCents = 10000L,
                currencyCode = "EUR",
                decimalDigits = 2
            )

            assertEquals(10000L, result.sumOf { it.amountCents })
        }
    }

    // ── applyPercentageUpdate ────────────────────────────────────────────

    @Nested
    inner class ApplyPercentageUpdate {

        @Test
        fun `edited user is locked at typed percentage`() {
            val splits = listOf(makeSplit("u1"), makeSplit("u2"), makeSplit("u3"))

            val result = delegate.applyPercentageUpdate(
                splits = splits,
                editedUserId = "u1",
                typedPercentage = "50",
                sourceAmountCents = 10000L,
                currencyCode = "EUR"
            )

            val u1 = result.first { it.userId == "u1" }
            assertEquals("50", u1.percentageInput)
            assertTrue(u1.isShareLocked)
            assertEquals(5000L, u1.amountCents)
        }

        @Test
        fun `remaining percentage is distributed to unlocked users`() {
            val splits = listOf(makeSplit("u1"), makeSplit("u2"), makeSplit("u3"))

            val result = delegate.applyPercentageUpdate(
                splits = splits,
                editedUserId = "u1",
                typedPercentage = "50",
                sourceAmountCents = 10000L,
                currencyCode = "EUR"
            )

            val otherCents = result.filter { it.userId != "u1" }.sumOf { it.amountCents }
            assertEquals(5000L, otherCents)
        }

        @Test
        fun `total cents equals source amount`() {
            val splits = listOf(makeSplit("u1"), makeSplit("u2"), makeSplit("u3"))

            val result = delegate.applyPercentageUpdate(
                splits = splits,
                editedUserId = "u2",
                typedPercentage = "40",
                sourceAmountCents = 10000L,
                currencyCode = "EUR"
            )

            assertEquals(10000L, result.sumOf { it.amountCents })
        }

        @Test
        fun `locked users keep their percentage`() {
            val splits = listOf(
                makeSplit("u1"),
                makeSplit("u2", percentageInput = "30", isShareLocked = true),
                makeSplit("u3")
            )

            val result = delegate.applyPercentageUpdate(
                splits = splits,
                editedUserId = "u1",
                typedPercentage = "50",
                sourceAmountCents = 10000L,
                currencyCode = "EUR"
            )

            val u2 = result.first { it.userId == "u2" }
            assertEquals("30", u2.percentageInput)
        }
    }

    // ── distributeRemainderEvenly ─────────────────────────────────────────

    @Nested
    inner class DistributeRemainderEvenly {

        @Test
        fun `empty active IDs returns empty map`() {
            val result = delegate.distributeRemainderEvenly(emptyList(), 5000L)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `zero remaining cents returns empty map`() {
            val result = delegate.distributeRemainderEvenly(listOf("u1", "u2"), 0L)
            assertTrue(result.isEmpty())
        }

        @Test
        fun `distributes evenly among active IDs`() {
            val result = delegate.distributeRemainderEvenly(listOf("u1", "u2"), 6000L)
            assertEquals(3000L, result["u1"])
            assertEquals(3000L, result["u2"])
        }

        @Test
        fun `handles uneven distribution with remainder`() {
            val result = delegate.distributeRemainderEvenly(listOf("u1", "u2", "u3"), 10000L)
            assertEquals(10000L, result.values.sum())
        }
    }

    // ── formatAmountOrEmpty ──────────────────────────────────────────────

    @Nested
    inner class FormatAmountOrEmpty {

        @Test
        fun `returns formatted string when source is positive`() {
            val result = delegate.formatAmountOrEmpty(5000L, 10000L, "EUR")
            assertTrue(result.isNotEmpty())
        }

        @Test
        fun `returns empty string when source is zero`() {
            val result = delegate.formatAmountOrEmpty(5000L, 0L, "EUR")
            assertEquals("", result)
        }
    }
}
