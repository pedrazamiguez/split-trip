package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.extensions.utcToSystemLocalDate
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.expense.presentation.model.DateRangePreset
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class ExpensesFilterUiMapper(
    private val formattingHelper: FormattingHelper,
    private val userUiMapper: UserUiMapper
) {

    fun extractDateBounds(
        expenses: List<Expense>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Pair<LocalDate?, LocalDate?> {
        val dates = expenses.mapNotNull { it.effectiveDate?.utcToSystemLocalDate(zoneId) }
        if (dates.isEmpty()) return null to null
        return dates.minOrNull() to dates.maxOrNull()
    }

    fun formatFilterDate(date: LocalDate?): String {
        return formattingHelper.formatShortDate(date)
    }

    fun calculatePresetRange(
        preset: DateRangePreset,
        today: LocalDate = LocalDate.now()
    ): Pair<LocalDate, LocalDate> = when (preset) {
        DateRangePreset.TODAY -> today to today
        DateRangePreset.YESTERDAY -> {
            val yesterday = today.minusDays(1)
            yesterday to yesterday
        }
        DateRangePreset.THIS_WEEK -> {
            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            startOfWeek to today
        }
        DateRangePreset.LAST_15_DAYS -> {
            val start = today.minusDays(14)
            start to today
        }
        DateRangePreset.THIS_MONTH -> {
            val startOfMonth = today.withDayOfMonth(1)
            startOfMonth to today
        }
    }

    fun findMatchingPreset(
        startDate: LocalDate?,
        endDate: LocalDate?,
        today: LocalDate = LocalDate.now()
    ): DateRangePreset? {
        if (startDate == null || endDate == null) return null
        return DateRangePreset.entries.firstOrNull { preset ->
            val (presetStart, presetEnd) = calculatePresetRange(preset, today)
            startDate == presetStart && endDate == presetEnd
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
