package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.foundation.ChartColors
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReceiptEuro
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CategorySpendingUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SubcategorySpendingUiModel
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class CategorySpendingUiMapper(
    private val localeProvider: LocaleProvider,
    private val resourceProvider: ResourceProvider
) {
    private data class UncoloredCategoryEntry(
        val name: String,
        val icon: ImageVector,
        val amount: Long,
        val formattedAmount: String,
        val formattedPercentage: String,
        val progress: Float,
        val subcategories: ImmutableList<SubcategorySpendingUiModel>
    )

    private fun formatPercentage(amount: Long, total: Long): String {
        if (amount <= 0L || total <= 0L) return "0%"
        val rounded = (amount.toFloat() / total.toFloat() * 100f).roundToInt()
        return if (rounded == 0) "<1%" else "$rounded%"
    }

    private fun filterActiveNonScheduledExpenses(
        expenses: List<Expense>,
        today: LocalDate
    ): List<Expense> = expenses.filter { expense ->
        val isFutureScheduled = expense.paymentStatus == PaymentStatus.SCHEDULED &&
            expense.dueDate?.toLocalDate()?.isAfter(today) == true
        expense.paymentStatus != PaymentStatus.CANCELLED && !isFutureScheduled
    }

    fun mapExpenses(
        expenses: List<Expense>,
        groupCurrency: String,
        withdrawals: List<CashWithdrawal> = emptyList(),
        today: LocalDate = LocalDate.now()
    ): ImmutableList<CategorySpendingUiModel> {
        val locale = localeProvider.getCurrentLocale()

        val activeExpenses = filterActiveNonScheduledExpenses(expenses, today)
        val positiveExpenses = activeExpenses.filter { it.groupAmount > 0 }

        val expenseExtras = activeExpenses.flatMap { it.addOns }
            .filter { it.type != AddOnType.DISCOUNT && it.groupAmountCents > 0L }
        val withdrawalExtras = withdrawals.flatMap { it.addOns }
            .filter { it.type != AddOnType.DISCOUNT && it.groupAmountCents > 0L }
        val allExtras = expenseExtras + withdrawalExtras
        val extrasTotalCents = allExtras.sumOf { it.groupAmountCents }

        val baseTotal = positiveExpenses.sumOf { it.groupAmount }
        val globalTotal = baseTotal + extrasTotalCents

        if (globalTotal <= 0L) return persistentListOf()

        val standardEntries = buildStandardCategoryEntries(
            positiveExpenses = positiveExpenses,
            globalTotal = globalTotal,
            groupCurrency = groupCurrency,
            locale = locale
        )
        val extrasEntry = buildExtrasCategoryEntry(
            allExtras = allExtras,
            extrasTotalCents = extrasTotalCents,
            globalTotal = globalTotal,
            groupCurrency = groupCurrency,
            locale = locale
        )

        val allEntries = if (extrasEntry != null) {
            standardEntries + extrasEntry
        } else {
            standardEntries
        }

        return allEntries
            .sortedByDescending { it.amount }
            .mapIndexed { index, entry ->
                CategorySpendingUiModel(
                    categoryName = entry.name,
                    categoryIcon = entry.icon,
                    formattedAmount = entry.formattedAmount,
                    formattedPercentage = entry.formattedPercentage,
                    progress = entry.progress,
                    color = ChartColors[index % ChartColors.size],
                    subcategories = entry.subcategories
                )
            }
            .toImmutableList()
    }

    private fun buildStandardCategoryEntries(
        positiveExpenses: List<Expense>,
        globalTotal: Long,
        groupCurrency: String,
        locale: Locale
    ): List<UncoloredCategoryEntry> = positiveExpenses.groupBy { it.category }
        .map { (category, categoryExpenses) ->
            val amount = categoryExpenses.sumOf { it.groupAmount }
            val progress = amount.toFloat() / globalTotal.toFloat()
            val formattedPercentage = formatPercentage(amount, globalTotal)
            val subcategories = buildExpenseSubcategories(
                categoryExpenses = categoryExpenses,
                categoryAmount = amount,
                groupCurrency = groupCurrency,
                locale = locale
            )

            UncoloredCategoryEntry(
                name = resourceProvider.getString(category.toStringRes()),
                icon = category.toIconVector(),
                amount = amount,
                formattedAmount = formatCurrencyAmount(amount, groupCurrency, locale),
                formattedPercentage = formattedPercentage,
                progress = progress,
                subcategories = subcategories
            )
        }

    private fun buildExpenseSubcategories(
        categoryExpenses: List<Expense>,
        categoryAmount: Long,
        groupCurrency: String,
        locale: Locale
    ): ImmutableList<SubcategorySpendingUiModel> = categoryExpenses
        .groupBy { it.subcategory }
        .mapValues { (_, subcategoryExpenses) -> subcategoryExpenses.sumOf { it.groupAmount } }
        .entries
        .sortedByDescending { it.value }
        .map { entry ->
            val subcategory = entry.key
            val subAmount = entry.value
            val formattedPercentage = formatPercentage(subAmount, categoryAmount)
            SubcategorySpendingUiModel(
                subcategoryName = resourceProvider.getString(subcategory.toStringRes()),
                subcategoryIcon = subcategory.toIconVector(),
                formattedAmount = formatCurrencyAmount(subAmount, groupCurrency, locale),
                formattedPercentage = formattedPercentage,
                rawAmountCents = subAmount
            )
        }
        .toImmutableList()

    private fun buildExtrasCategoryEntry(
        allExtras: List<AddOn>,
        extrasTotalCents: Long,
        globalTotal: Long,
        groupCurrency: String,
        locale: Locale
    ): UncoloredCategoryEntry? {
        if (extrasTotalCents <= 0L) return null

        val extrasSubcategories = allExtras
            .groupBy { it.type }
            .mapValues { (_, addOns) -> addOns.sumOf { it.groupAmountCents } }
            .entries
            .sortedByDescending { it.value }
            .map { entry ->
                val addOnType = entry.key
                val subAmount = entry.value
                val formattedPercentage = formatPercentage(subAmount, extrasTotalCents)
                val typeName = resolveAddOnTypeName(addOnType)
                SubcategorySpendingUiModel(
                    subcategoryName = typeName,
                    subcategoryIcon = addOnType.toIconVector(),
                    formattedAmount = formatCurrencyAmount(subAmount, groupCurrency, locale),
                    formattedPercentage = formattedPercentage,
                    rawAmountCents = subAmount
                )
            }
            .toImmutableList()

        return UncoloredCategoryEntry(
            name = resourceProvider.getString(R.string.balances_total_extras),
            icon = TablerIcons.Outline.ReceiptEuro,
            amount = extrasTotalCents,
            formattedAmount = formatCurrencyAmount(extrasTotalCents, groupCurrency, locale),
            formattedPercentage = formatPercentage(extrasTotalCents, globalTotal),
            progress = extrasTotalCents.toFloat() / globalTotal.toFloat(),
            subcategories = extrasSubcategories
        )
    }

    private fun resolveAddOnTypeName(addOnType: AddOnType): String = when (addOnType) {
        AddOnType.FEE -> resourceProvider.getString(R.string.balances_extras_add_on_fee_plural)
        AddOnType.SURCHARGE -> resourceProvider.getString(R.string.balances_extras_add_on_surcharge_plural)
        AddOnType.TIP -> resourceProvider.getString(R.string.balances_extras_add_on_tip_plural)
        AddOnType.DISCOUNT -> ""
    }

    fun calculateTotalCents(
        expenses: List<Expense>,
        withdrawals: List<CashWithdrawal> = emptyList(),
        today: LocalDate = LocalDate.now()
    ): Long {
        val activeExpenses = filterActiveNonScheduledExpenses(expenses, today)
        val baseTotalCents = activeExpenses.filter { it.groupAmount > 0 }.sumOf { it.groupAmount }
        val expenseExtrasCents = activeExpenses.sumOf { expense ->
            expense.addOns
                .filter { it.type != AddOnType.DISCOUNT && it.groupAmountCents > 0L }
                .sumOf { it.groupAmountCents }
        }
        val withdrawalExtrasCents = withdrawals.sumOf { withdrawal ->
            withdrawal.addOns
                .filter { it.type != AddOnType.DISCOUNT && it.groupAmountCents > 0L }
                .sumOf { it.groupAmountCents }
        }
        return baseTotalCents + expenseExtrasCents + withdrawalExtrasCents
    }

    fun formatTotalAmount(totalCents: Long, groupCurrency: String): String {
        if (totalCents <= 0) return ""
        return formatCurrencyAmount(totalCents, groupCurrency, localeProvider.getCurrentLocale())
    }
}
