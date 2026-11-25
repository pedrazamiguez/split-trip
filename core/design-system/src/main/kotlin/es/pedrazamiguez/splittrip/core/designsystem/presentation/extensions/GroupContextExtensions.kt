package es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions

import androidx.annotation.StringRes
import es.pedrazamiguez.splittrip.core.common.R
import es.pedrazamiguez.splittrip.domain.enums.GroupContext

@StringRes
fun GroupContext.toStringRes(): Int = when (this) {
    GroupContext.TRAVEL -> R.string.group_context_travel
    GroupContext.EVERYDAY -> R.string.group_context_everyday
}

@StringRes
fun GroupContext.toDescriptionRes(): Int = when (this) {
    GroupContext.TRAVEL -> R.string.group_context_travel_description
    GroupContext.EVERYDAY -> R.string.group_context_everyday_description
}
