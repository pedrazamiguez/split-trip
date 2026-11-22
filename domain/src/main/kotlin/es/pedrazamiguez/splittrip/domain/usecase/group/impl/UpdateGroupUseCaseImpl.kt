package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveCurrencyException
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupCurrenciesInUseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.UpdateGroupUseCase
import java.time.YearMonth

class UpdateGroupUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val getGroupCurrenciesInUseUseCase: GetGroupCurrenciesInUseUseCase,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val yearMonthProvider: () -> YearMonth = { YearMonth.now() }
) : UpdateGroupUseCase {

    override suspend operator fun invoke(group: Group): Result<Unit> = runCatching {
        val existingGroup = groupRepository.getGroupById(group.id)
            ?: throw IllegalArgumentException("Group not found with id: ${group.id}")
        if (existingGroup.status == GroupStatus.ARCHIVED) {
            throw GroupArchivedException(group.id)
        }

        val existingSupportedCurrencies = setOf(existingGroup.currency) + existingGroup.extraCurrencies
        val updatedSupportedCurrencies = setOf(group.currency) + group.extraCurrencies
        val removedCurrencies = existingSupportedCurrencies - updatedSupportedCurrencies

        if (removedCurrencies.isNotEmpty()) {
            val currenciesInUse = getGroupCurrenciesInUseUseCase(group.id)
            val blockedCurrency = removedCurrencies.firstOrNull { it in currenciesInUse }
            if (blockedCurrency != null) {
                throw CannotRemoveCurrencyException(
                    currencyCode = blockedCurrency,
                    reason = CannotRemoveCurrencyException.Reason.CURRENCY_IN_USE
                )
            }
        }

        groupRepository.updateGroup(group)
        val isNewOrReplacedCover = !group.mainImagePath.isNullOrBlank() &&
            group.mainImagePath != existingGroup.mainImagePath
        if (isNewOrReplacedCover) {
            userPreferenceRepository.incrementMonthlyManualReceiptAttachmentCount(yearMonthProvider())
        }
    }
}
