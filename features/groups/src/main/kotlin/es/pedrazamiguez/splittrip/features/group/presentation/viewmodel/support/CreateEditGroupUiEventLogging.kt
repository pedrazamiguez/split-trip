package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support

import es.pedrazamiguez.splittrip.core.logging.sanitizer.maskEmail
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent

internal fun formatEventForLogging(event: CreateEditGroupUiEvent): String {
    return when (event) {
        is CreateEditGroupUiEvent.MemberSearchQueryChanged ->
            "MemberSearchQueryChanged(queryLength=${event.query.length})"
        is CreateEditGroupUiEvent.MemberSelected ->
            "MemberSelected(userId=${event.user.userId}, email=${event.user.email.maskEmail()})"
        is CreateEditGroupUiEvent.MemberRemoved ->
            "MemberRemoved(userId=${event.user.userId}, email=${event.user.email.maskEmail()})"
        is CreateEditGroupUiEvent.MemberScanned ->
            "MemberScanned(userId=${event.userId}, email=${event.email.maskEmail()})"
        is CreateEditGroupUiEvent.UnregisteredMemberDisplayNameChanged ->
            "UnregisteredMemberDisplayNameChanged(userId=${event.userId}, nameLength=${event.displayName.length})"
        else -> event::class.java.simpleName
    }
}
