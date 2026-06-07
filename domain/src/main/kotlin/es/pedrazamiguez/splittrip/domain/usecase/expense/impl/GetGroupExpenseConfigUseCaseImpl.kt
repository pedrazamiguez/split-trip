package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.model.GroupExpenseConfig
import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpenseConfigUseCase

class GetGroupExpenseConfigUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val currencyRepository: CurrencyRepository,
    private val subunitRepository: SubunitRepository
) : GetGroupExpenseConfigUseCase {

    /**
     * Fetches the expense configuration for a specific group.
     *
     * @param groupId The ID of the group to fetch configuration for
     * @param forceRefresh Whether to bypass cache and fetch fresh currency data
     * @return Result containing GroupExpenseConfig, or failure if:
     *         - groupId is null/blank
     *         - group is not found
     *         - group's currency is not in the available currencies list
     */
    override suspend operator fun invoke(groupId: String?, forceRefresh: Boolean): Result<GroupExpenseConfig> =
        runCatching {
            require(!groupId.isNullOrBlank()) { "Group ID cannot be null or blank" }

            val group = groupRepository.getGroupById(groupId)
                ?: error("Group not found: $groupId")

            val allCurrencies = currencyRepository.getCurrencies(forceRefresh)

            val groupCurrency = allCurrencies.find { it.code == group.currency }
                ?: error("Group currency '${group.currency}' not found in available currencies")

            // Include group's main currency plus any extra currencies configured for the group
            val allowedCodes = (listOf(group.currency) + group.extraCurrencies).distinct()
            val availableCurrencies = allCurrencies.filter { it.code in allowedCodes }

            // Fetch subunits for subunit-aware splitting (one-shot read, no cloud side-effects)
            val subunits = subunitRepository.getGroupSubunits(groupId)

            GroupExpenseConfig(
                group = group,
                groupCurrency = groupCurrency,
                availableCurrencies = availableCurrencies,
                subunits = subunits
            )
        }
}
