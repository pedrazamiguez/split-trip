package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

/**
 * Bundles the localised labels for [MemberPickerCard], keeping the composable
 * parameter count within the detekt `LongParameterList` threshold.
 *
 * @param title             Localised card title (e.g. "Contributing member", "Withdrawn by").
 * @param currentUserLabel Localised label for the current user (e.g. "You").
 */
data class MemberPickerCardLabels(
    val title: String,
    val currentUserLabel: String
)
