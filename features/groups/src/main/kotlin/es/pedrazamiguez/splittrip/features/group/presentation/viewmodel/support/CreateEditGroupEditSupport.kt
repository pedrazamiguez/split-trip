package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
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

internal fun buildPendingUserSearchResults(
    query: String,
    currentUserEmail: String?,
    selectedIds: Set<String>
): List<User> {
    val normalizedEmail = User.normalizeEmail(query)
    if (currentUserEmail != null && User.areEmailsEquivalent(query, currentUserEmail)) {
        return emptyList()
    }
    val pendingUserId = User.generatePendingUserId(normalizedEmail)
    return if (pendingUserId !in selectedIds) {
        listOf(User(userId = pendingUserId, email = normalizedEmail, isPending = true))
    } else {
        emptyList()
    }
}

internal fun resolveIsCurrentUser(
    userId: String,
    email: String,
    currentUserId: String?,
    currentUserEmail: String?
): Boolean = (currentUserId != null && userId == currentUserId) ||
    (currentUserEmail != null && User.areEmailsEquivalent(email, currentUserEmail))

internal fun updateMemberDisplayName(
    members: ImmutableList<User>,
    userId: String,
    displayName: String
): ImmutableList<User> = members.map { user ->
    if (user.userId == userId) {
        user.copy(displayName = displayName.trim().takeIf { it.isNotBlank() })
    } else {
        user
    }
}.toImmutableList()

internal fun toggleExtraCurrencySelection(
    code: String,
    availableCurrencies: ImmutableList<CurrencyUiModel>,
    currentExtras: ImmutableList<CurrencyUiModel>
): ImmutableList<CurrencyUiModel> = if (currentExtras.any { it.code == code }) {
    currentExtras.filter { it.code != code }.toImmutableList()
} else {
    val item = availableCurrencies.find { it.code == code } ?: return currentExtras
    (currentExtras + item).toImmutableList()
}

internal fun CreateEditGroupUiState.withInitialCreateState(
    mappedCurrencies: ImmutableList<CurrencyUiModel>,
    defaultCurrency: CurrencyUiModel?
): CreateEditGroupUiState = copy(
    isLoading = false,
    isEditMode = false,
    availableCurrencies = mappedCurrencies,
    selectedCurrency = selectedCurrency ?: defaultCurrency,
    isLoadingCurrencies = false
).let { it.copy(initialFormSnapshot = it.toFormSnapshot()) }

internal fun CreateEditGroupUiState.withInitialEditState(
    group: Group,
    mappedCurrencies: ImmutableList<CurrencyUiModel>,
    memberUsers: List<User>,
    currenciesInUse: ImmutableSet<String>,
    nonRemovableMembers: ImmutableMap<String, CannotRemoveMemberException.Reason>
): CreateEditGroupUiState = copy(
    isLoading = false,
    isEditMode = true,
    groupId = group.id,
    groupName = group.name,
    groupDescription = group.description,
    groupContext = group.context,
    availableCurrencies = mappedCurrencies,
    selectedCurrency = mappedCurrencies.find { it.code == group.currency },
    extraCurrencies = mappedCurrencies.filter { it.code in group.extraCurrencies }.toImmutableList(),
    currenciesInUse = currenciesInUse,
    selectedMembers = memberUsers.toImmutableList(),
    nonRemovableMembers = nonRemovableMembers,
    imageUrl = group.mainImagePath,
    localGroupImagePath = group.mainImagePath,
    isLoadingCurrencies = false
).let { it.copy(initialFormSnapshot = it.toFormSnapshot()) }

internal fun mergeScannedMemberProfile(
    members: ImmutableList<User>,
    userId: String,
    fullUser: User
): ImmutableList<User> = members.map { if (it.userId == userId) fullUser else it }.toImmutableList()
