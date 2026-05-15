package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

/**
 * Groups all event callbacks for [EntitySplitEditor] to avoid exceeding the parameter limit.
 */
data class EntitySplitEditorEvents(
    val onAmountChanged: (entityId: String, amount: String) -> Unit,
    val onPercentageChanged: (entityId: String, percentage: String) -> Unit,
    val onExcludedToggled: (entityId: String) -> Unit,
    val onShareLockToggled: (entityId: String) -> Unit,
    val onAccordionToggled: (entityId: String) -> Unit,
    val onIntraSubunitSplitTypeChanged: (subunitId: String, splitTypeId: String) -> Unit,
    val onIntraSubunitAmountChanged: (subunitId: String, userId: String, amount: String) -> Unit,
    val onIntraSubunitPercentageChanged: (subunitId: String, userId: String, percentage: String) -> Unit,
    val onIntraSubunitShareLockToggled: (subunitId: String, userId: String) -> Unit
)
