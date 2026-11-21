package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state

import androidx.compose.runtime.Immutable
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CategoryInsightUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntityBreakdownMode
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntitySpendingInsightUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingBurnRateUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class SpendingInsightsUiState(
    val isLoading: Boolean = true,
    val groupId: String? = null,
    val selectedPreset: SpendingPeriodPreset = SpendingPeriodPreset.ALL_TIME,
    val periodSummaryLabel: String = "",
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    val formattedCustomStartDate: String = "",
    val formattedCustomEndDate: String = "",
    val oldestExpenseDate: LocalDate? = null,
    val newestExpenseDate: LocalDate? = null,
    val hasAnyGroupExpenses: Boolean = false,
    val hasPeriodExpenses: Boolean = false,
    val burnRate: SpendingBurnRateUiModel? = null,
    val selectedDayBarId: String? = null,
    val categoryItems: ImmutableList<CategoryInsightUiModel> = persistentListOf(),
    val hasSubunits: Boolean = false,
    val entityBreakdownMode: EntityBreakdownMode = EntityBreakdownMode.BY_SUBUNIT,
    val entityItems: ImmutableList<EntitySpendingInsightUiModel> = persistentListOf(),
    val memberItems: ImmutableList<EntitySpendingInsightUiModel> = persistentListOf()
)
