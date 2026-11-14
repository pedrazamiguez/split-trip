package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.features.group.R

internal fun CannotRemoveMemberException.Reason.toUiText(): UiText = when (this) {
    CannotRemoveMemberException.Reason.NON_ZERO_BALANCE ->
        UiText.StringResource(R.string.group_remove_member_error_balance)
    CannotRemoveMemberException.Reason.IS_CREATOR ->
        UiText.StringResource(R.string.group_remove_member_error_admin)
    CannotRemoveMemberException.Reason.LAST_MEMBER ->
        UiText.StringResource(R.string.group_remove_member_error_last_member)
    CannotRemoveMemberException.Reason.NOT_A_MEMBER,
    CannotRemoveMemberException.Reason.USER_NOT_IN_BALANCES ->
        UiText.StringResource(R.string.group_error_remove_member_failed)
}
