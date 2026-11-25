package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.extensions.utcToSystemLocalDate
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.expense.presentation.model.DateBounds
import es.pedrazamiguez.splittrip.features.expense.presentation.model.DateRange
import es.pedrazamiguez.splittrip.features.expense.presentation.model.DateRangePreset
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class ExpensesFilterUiMapper(
    private val formattingHelper: FormattingHelper,
    private val userUiMapper: UserUiMapper
) {

    fun extractAvailableCategories(expenses: List<Expense>): ImmutableList<ExpenseCategory> {
        if (expenses.isEmpty()) return persistentListOf()
        val usedCategories = expenses.mapTo(mutableSetOf()) { it.category }
        return ExpenseCategory.entries
            .filter { it in usedCategories }
            .toImmutableList()
    }

    fun extractAvailableSubcategories(expenses: List<Expense>): ImmutableList<ExpenseSubcategory> {
        if (expenses.isEmpty()) return persistentListOf()
        val usedSubcategories = expenses
            .asSequence()
            .map { it.subcategory }
            .filter { it != ExpenseSubcategory.UNSPECIFIED }
            .toSet()
        if (usedSubcategories.isEmpty()) return persistentListOf()
        return ExpenseSubcategory.entries
            .filter { it in usedSubcategories }
            .toImmutableList()
    }

    fun extractDateBounds(
        expenses: List<Expense>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DateBounds {
        val dates = expenses.mapNotNull { it.effectiveDate?.utcToSystemLocalDate(zoneId) }
        if (dates.isEmpty()) return DateBounds(startDate = null, endDate = null)
        return DateBounds(startDate = dates.minOrNull(), endDate = dates.maxOrNull())
    }

    fun formatFilterDate(date: LocalDate?): String {
        return formattingHelper.formatShortDate(date)
    }

    fun calculatePresetRange(
        preset: DateRangePreset,
        today: LocalDate = LocalDate.now()
    ): DateRange = when (preset) {
        DateRangePreset.TODAY -> DateRange(startDate = today, endDate = today)
        DateRangePreset.YESTERDAY -> {
            val yesterday = today.minusDays(1)
            DateRange(startDate = yesterday, endDate = yesterday)
        }
        DateRangePreset.THIS_WEEK -> {
            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            DateRange(startDate = startOfWeek, endDate = today)
        }
        DateRangePreset.LAST_15_DAYS -> {
            val start = today.minusDays(14)
            DateRange(startDate = start, endDate = today)
        }
        DateRangePreset.THIS_MONTH -> {
            val startOfMonth = today.withDayOfMonth(1)
            DateRange(startDate = startOfMonth, endDate = today)
        }
    }

    fun findMatchingPreset(
        startDate: LocalDate?,
        endDate: LocalDate?,
        today: LocalDate = LocalDate.now()
    ): DateRangePreset? {
        if (startDate == null || endDate == null) return null
        return DateRangePreset.entries.firstOrNull { preset ->
            val presetRange = calculatePresetRange(preset, today)
            startDate == presetRange.startDate && endDate == presetRange.endDate
        }
    }

    fun mapAvailableMembers(
        allUserIds: List<String>,
        memberProfiles: Map<String, User>,
        currentUserId: String?
    ): ImmutableList<MemberOptionUiModel> {
        return allUserIds.map { userId ->
            val user = memberProfiles[userId]
            val displayName = userUiMapper.mapToDisplayName(
                user = user,
                fallbackUserId = userId,
                currentUserId = currentUserId,
                selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
            )
            MemberOptionUiModel(
                userId = userId,
                displayName = displayName,
                isCurrentUser = userId == currentUserId
            )
        }.sortedWith(
            compareByDescending<MemberOptionUiModel> { it.isCurrentUser }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
        ).toImmutableList()
    }
}
