package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import timber.log.Timber

internal fun buildUpdatedExtraCurrencies(
    previousCurrency: CurrencyUiModel?,
    newCode: String,
    currentExtras: ImmutableList<CurrencyUiModel>,
    currenciesInUse: Set<String>
): ImmutableList<CurrencyUiModel> {
    val baseExtras = currentExtras.filter { it.code != newCode }
    if (previousCurrency == null || previousCurrency.code == newCode) {
        return baseExtras.toImmutableList()
    }
    val shouldPreserve = previousCurrency.code in currenciesInUse &&
        baseExtras.none { it.code == previousCurrency.code }
    return if (shouldPreserve) {
        (baseExtras + previousCurrency).toImmutableList()
    } else {
        baseExtras.toImmutableList()
    }
}

internal fun resolveMemberRemovalBlockReason(
    isEditMode: Boolean,
    selectedMembersCount: Int,
    nonRemovableMembers: ImmutableMap<String, CannotRemoveMemberException.Reason>,
    userId: String
): CannotRemoveMemberException.Reason? {
    nonRemovableMembers[userId]?.let { return it }
    if (isEditMode && selectedMembersCount <= 1) {
        return CannotRemoveMemberException.Reason.LAST_MEMBER
    }
    return null
}

internal suspend fun <T> loadEditGuard(
    fallback: T,
    errorMessage: String,
    block: suspend () -> T
): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Timber.w(e, errorMessage)
    fallback
}
