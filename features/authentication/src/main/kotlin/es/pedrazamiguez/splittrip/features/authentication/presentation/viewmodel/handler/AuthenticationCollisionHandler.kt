package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Interface for the handler managing account collision merge events during login.
 */
interface AuthenticationCollisionHandler {

    /**
     * Binds the handler to the ViewModel's state flow and CoroutineScope.
     */
    fun bind(
        stateFlow: MutableStateFlow<AuthenticationUiState>,
        scope: CoroutineScope
    )

    fun handleCollisionPasswordChanged(value: String)
    fun handleSubmitCollisionMerge(onLoginSuccess: () -> Unit)
    fun handleDismissCollisionDialog()
}
