package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.foundation.ChartColors
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.BreakdownPercentage
import es.pedrazamiguez.splittrip.domain.model.EntitySpendingInsight
import es.pedrazamiguez.splittrip.domain.model.EntitySpendingType
import es.pedrazamiguez.splittrip.domain.model.GroupSpendingInsights
import es.pedrazamiguez.splittrip.domain.model.MemberSpendingInsight
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CategoryInsightUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.DailySpendingBarUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntityBreakdownMode
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntitySpendingInsightUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingBurnRateUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.SpendingInsightsUiState
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class SpendingInsightsUiMapper(
    private val formattingHelper: FormattingHelper,
    private val resourceProvider: ResourceProvider,
    private val userUiMapper: UserUiMapper
) {

    private data class MonthlyPaceCopy(
        val label: String,
        val formattedValue: String
    )

    @Suppress("LongParameterList")
    fun mapToUiState(
        insights: GroupSpendingInsights,
        selectedPreset: SpendingPeriodPreset,
        customStartDate: LocalDate?,
        customEndDate: LocalDate?,
        entityBreakdownMode: EntityBreakdownMode,
        selectedDayBarId: String?,
        referenceYear: Int = LocalDate.now().year
    ): SpendingInsightsUiState {
        val hasPeriodExpenses = insights.expenseCount > 0 && insights.totalSpentCents > 0L
        val periodSummaryLabel = buildPeriodSummaryLabel(
            insights = insights,
            hasPeriodExpenses = hasPeriodExpenses,
            referenceYear = referenceYear
        )

        val mappedBurnRate = if (hasPeriodExpenses) {
            mapBurnRate(insights = insights, referenceYear = referenceYear)
        } else {
            null
        }

        val resolvedSelectedDayBarId = resolveSelectedDayBarId(
            burnRate = mappedBurnRate,
            requestedBarId = selectedDayBarId
        )

        val categoryItems = if (hasPeriodExpenses) {
            mapCategoryItems(insights)
        } else {
            persistentListOf()
        }

        val memberItems = if (hasPeriodExpenses) {
            mapMemberItems(insights)
        } else {
            persistentListOf()
        }

        val entityItems = if (hasPeriodExpenses) {
            mapEntityItems(insights)
        } else {
            persistentListOf()
        }

        return SpendingInsightsUiState(
            isLoading = false,
            groupId = insights.groupId,
            selectedPreset = selectedPreset,
            periodSummaryLabel = periodSummaryLabel,
            customStartDate = customStartDate,
            customEndDate = customEndDate,
            formattedCustomStartDate = formattingHelper.formatShortDate(customStartDate, referenceYear),
            formattedCustomEndDate = formattingHelper.formatShortDate(customEndDate, referenceYear),
            oldestExpenseDate = insights.oldestExpenseDate,
            newestExpenseDate = insights.newestExpenseDate,
            hasAnyGroupExpenses = insights.hasAnyGroupExpenses,
            hasPeriodExpenses = hasPeriodExpenses,
            burnRate = mappedBurnRate,
            selectedDayBarId = resolvedSelectedDayBarId,
            categoryItems = categoryItems,
            hasSubunits = insights.hasSubunits,
            entityBreakdownMode = entityBreakdownMode,
            entityItems = entityItems,
            memberItems = memberItems
        )
    }

    private fun buildPeriodSummaryLabel(
        insights: GroupSpendingInsights,
        hasPeriodExpenses: Boolean,
        referenceYear: Int
    ): String {
        val windowLabel = resolveWindowLabel(insights = insights, referenceYear = referenceYear)
        if (!hasPeriodExpenses) return windowLabel

        val expenseCountLabel = resourceProvider.getQuantityString(
            R.plurals.balances_insights_expense_count,
            insights.expenseCount,
            insights.expenseCount
        )
        return resourceProvider.getString(
            R.string.balances_insights_period_summary,
            windowLabel,
            expenseCountLabel
        )
    }

    private fun resolveWindowLabel(
        insights: GroupSpendingInsights,
        referenceYear: Int
    ): String {
        val start = insights.burnRate.windowStartDate
        val end = insights.burnRate.windowEndDate
        return when (insights.period) {
            SpendingInsightsPeriod.ThisMonth,
            SpendingInsightsPeriod.LastMonth ->
                start?.let { formattingHelper.formatMediumDate(it) }.orEmpty()

            SpendingInsightsPeriod.AllTime,
            is SpendingInsightsPeriod.CustomRange -> formatDateRangeLabel(
                start = start,
                end = end,
                referenceYear = referenceYear
            )
        }
    }

    private fun formatDateRangeLabel(
        start: LocalDate?,
        end: LocalDate?,
        referenceYear: Int
    ): String {
        if (start == null || end == null) {
            return resourceProvider.getString(R.string.balances_insights_period_all_time)
        }
        if (start == end) {
            return formattingHelper.formatShortDate(start, referenceYear)
        }
        return resourceProvider.getString(
            R.string.balances_insights_period_range,
            formattingHelper.formatShortDate(start, referenceYear),
            formattingHelper.formatShortDate(end, referenceYear)
        )
    }

    private fun mapBurnRate(
        insights: GroupSpendingInsights,
        referenceYear: Int
    ): SpendingBurnRateUiModel {
        val burnRate = insights.burnRate
        val formattedTotalSpent = formattingHelper.formatCentsWithCurrency(
            cents = insights.totalSpentCents,
            currencyCode = insights.currency
        )
        val formattedExtrasCaption = if (insights.totalExtrasCents > 0L) {
            resourceProvider.getString(
                R.string.balances_insights_extras_included,
                formattingHelper.formatCentsWithCurrency(insights.totalExtrasCents, insights.currency)
            )
        } else {
            null
        }
        val formattedDailyAverage = resourceProvider.getString(
            R.string.balances_insights_daily_average_value,
            formattingHelper.formatCentsWithCurrency(burnRate.averageDailySpendCents, insights.currency)
        )
        val activeDaysLabel = resourceProvider.getQuantityString(
            R.plurals.balances_insights_active_days,
            burnRate.activeDaysCount,
            burnRate.activeDaysCount
        )
        val monthlyPace = resolveMonthlyPaceCopy(insights)
        val dailyBars = mapDailyBars(insights = insights, referenceYear = referenceYear)
        val defaultSelectedBarId = dailyBars.firstOrNull { it.isPeakDay }?.id
            ?: dailyBars.lastOrNull { it.heightRatio > 0f }?.id

        return SpendingBurnRateUiModel(
            formattedTotalSpent = formattedTotalSpent,
            formattedExtrasCaption = formattedExtrasCaption,
            formattedDailyAverage = formattedDailyAverage,
            activeDaysLabel = activeDaysLabel,
            formattedMonthlyPace = monthlyPace?.formattedValue,
            monthlyPaceLabel = monthlyPace?.label,
            formattedPeakDayAmount = burnRate.peakDayDate?.let {
                formattingHelper.formatCentsWithCurrency(burnRate.peakDayAmountCents, insights.currency)
            },
            formattedPeakDayDate = burnRate.peakDayDate?.let {
                formattingHelper.formatShortDate(it, referenceYear)
            },
            dailyBars = dailyBars,
            defaultSelectedBarId = defaultSelectedBarId
        )
    }

    private fun resolveMonthlyPaceCopy(insights: GroupSpendingInsights): MonthlyPaceCopy? {
        val projectedCents = insights.burnRate.projectedMonthlySpendCents ?: return null
        val formattedCurrency = formattingHelper.formatCentsWithCurrency(projectedCents, insights.currency)
        return when (insights.period) {
            SpendingInsightsPeriod.ThisMonth -> MonthlyPaceCopy(
                label = resourceProvider.getString(R.string.balances_insights_monthly_pace_projected_label),
                formattedValue = resourceProvider.getString(
                    R.string.balances_insights_monthly_pace_value,
                    formattedCurrency
                )
            )

            SpendingInsightsPeriod.LastMonth -> MonthlyPaceCopy(
                label = resourceProvider.getString(R.string.balances_insights_monthly_pace_completed_label),
                formattedValue = formattedCurrency
            )

            SpendingInsightsPeriod.AllTime,
            is SpendingInsightsPeriod.CustomRange -> MonthlyPaceCopy(
                label = resourceProvider.getString(R.string.balances_insights_monthly_pace_30d_label),
                formattedValue = resourceProvider.getString(
                    R.string.balances_insights_monthly_pace_value,
                    formattedCurrency
                )
            )
        }
    }

    private fun mapDailyBars(
        insights: GroupSpendingInsights,
        referenceYear: Int
    ): ImmutableList<DailySpendingBarUiModel> {
        val peakDate = insights.burnRate.peakDayDate
        return insights.burnRate.dailySeries.map { point ->
            val fullDateLabel = formattingHelper.formatShortDate(point.date, referenceYear)
            val formattedAmount = formattingHelper.formatCentsWithCurrency(point.amountCents, insights.currency)
            val expenseCountLabel = resourceProvider.getQuantityString(
                R.plurals.balances_insights_expense_count,
                point.expenseCount,
                point.expenseCount
            )
            DailySpendingBarUiModel(
                id = point.date.toString(),
                axisLabel = point.date.dayOfMonth.toString(),
                fullDateLabel = fullDateLabel,
                formattedAmount = formattedAmount,
                expenseCountLabel = expenseCountLabel,
                heightRatio = point.heightRatio.toFloat().coerceIn(0f, 1f),
                isPeakDay = peakDate == point.date && point.amountCents > 0L,
                contentDescription = resourceProvider.getString(
                    R.string.balances_insights_daily_bar_cd,
                    fullDateLabel,
                    formattedAmount
                )
            )
        }.toImmutableList()
    }

    private fun resolveSelectedDayBarId(
        burnRate: SpendingBurnRateUiModel?,
        requestedBarId: String?
    ): String? {
        if (burnRate == null) return null
        val isValidRequested = requestedBarId != null && burnRate.dailyBars.any { it.id == requestedBarId }
        return if (isValidRequested) requestedBarId else burnRate.defaultSelectedBarId
    }

    private fun mapCategoryItems(insights: GroupSpendingInsights): ImmutableList<CategoryInsightUiModel> =
        insights.categoryBreakdown.mapIndexed { index, item ->
            CategoryInsightUiModel(
                category = item.category,
                categoryName = resourceProvider.getString(item.category.toStringRes()),
                categoryIcon = item.category.toIconVector(),
                formattedAmount = formattingHelper.formatCentsWithCurrency(item.totalAmountCents, insights.currency),
                formattedPercentage = item.breakdownPercentage.toDisplayString(),
                expenseCountLabel = resourceProvider.getQuantityString(
                    R.plurals.balances_insights_expense_count,
                    item.expenseCount,
                    item.expenseCount
                ),
                progress = item.shareRatio.toFloat().coerceIn(0f, 1f),
                color = ChartColors[index % ChartColors.size]
            )
        }.toImmutableList()

    private fun mapMemberItems(insights: GroupSpendingInsights): ImmutableList<EntitySpendingInsightUiModel> =
        insights.memberBreakdown.mapIndexed { index, member ->
            mapMemberItem(
                index = index,
                member = member,
                insights = insights
            )
        }.toImmutableList()

    private fun mapMemberItem(
        index: Int,
        member: MemberSpendingInsight,
        insights: GroupSpendingInsights
    ): EntitySpendingInsightUiModel {
        val user = insights.memberProfiles[member.userId]
        val isCurrentUser = insights.currentUserId != null && member.userId == insights.currentUserId
        val subtitle = if (member.expenseCount > 0) {
            resourceProvider.getQuantityString(
                R.plurals.balances_insights_expense_count,
                member.expenseCount,
                member.expenseCount
            )
        } else {
            null
        }
        return EntitySpendingInsightUiModel(
            id = member.userId,
            displayName = userUiMapper.mapToDisplayName(
                user = user,
                fallbackUserId = member.userId,
                currentUserId = insights.currentUserId,
                selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
            ),
            subtitle = subtitle,
            isSubunit = false,
            isCurrentUser = isCurrentUser,
            formattedAmount = formattingHelper.formatCentsWithCurrency(
                member.effectiveSpentCents,
                insights.currency
            ),
            formattedPercentage = member.breakdownPercentage.toDisplayString(),
            progress = member.shareRatio.toFloat().coerceIn(0f, 1f),
            color = ChartColors[index % ChartColors.size]
        )
    }

    private fun mapEntityItems(insights: GroupSpendingInsights): ImmutableList<EntitySpendingInsightUiModel> =
        insights.entityBreakdown.mapIndexed { index, entity ->
            mapEntityItem(
                index = index,
                entity = entity,
                insights = insights
            )
        }.toImmutableList()

    private fun mapEntityItem(
        index: Int,
        entity: EntitySpendingInsight,
        insights: GroupSpendingInsights
    ): EntitySpendingInsightUiModel {
        val isSubunit = entity.type == EntitySpendingType.SUBUNIT
        val isCurrentUser = if (isSubunit) {
            insights.currentUserId != null && insights.currentUserId in entity.memberIds
        } else {
            insights.currentUserId != null && entity.entityId == insights.currentUserId
        }
        val displayName = if (isSubunit) {
            entity.subunitName.orEmpty()
        } else {
            userUiMapper.mapToDisplayName(
                user = insights.memberProfiles[entity.entityId],
                fallbackUserId = entity.entityId,
                currentUserId = insights.currentUserId,
                selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
            )
        }
        val subtitle = when {
            isSubunit -> resourceProvider.getQuantityString(
                R.plurals.balances_insights_member_count,
                entity.memberIds.size,
                entity.memberIds.size
            )
            entity.expenseCount > 0 -> resourceProvider.getQuantityString(
                R.plurals.balances_insights_expense_count,
                entity.expenseCount,
                entity.expenseCount
            )
            else -> null
        }

        return EntitySpendingInsightUiModel(
            id = entity.entityId,
            displayName = displayName,
            subtitle = subtitle,
            isSubunit = isSubunit,
            isCurrentUser = isCurrentUser,
            formattedAmount = formattingHelper.formatCentsWithCurrency(
                entity.effectiveSpentCents,
                insights.currency
            ),
            formattedPercentage = entity.breakdownPercentage.toDisplayString(),
            progress = entity.shareRatio.toFloat().coerceIn(0f, 1f),
            color = ChartColors[index % ChartColors.size]
        )
    }
}

private fun BreakdownPercentage.toDisplayString(): String = when (this) {
    BreakdownPercentage.Zero -> "0%"
    BreakdownPercentage.SubOnePercent -> "<1%"
    is BreakdownPercentage.Whole -> "$percent%"
}
