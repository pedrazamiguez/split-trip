package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiAction
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Interface for the handler managing registration submission and collision dialog dismissals.
 */
interface RegisterSubmitHandler {

    /**
     * Binds the handler to the ViewModel's state flow, actions channel, and CoroutineScope.
     */
    fun bind(
        stateFlow: MutableStateFlow<RegisterUiState>,
        actionsChannel: Channel<RegisterUiAction>,
        scope: CoroutineScope
    )

    fun handleSubmitSignUp()
    fun handleDismissCollisionDialog()
}
