package es.pedrazamiguez.splittrip.features.authentication.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.authentication.presentation.feature.ForgotPasswordFeature
import es.pedrazamiguez.splittrip.features.authentication.presentation.feature.LoginFeature
import es.pedrazamiguez.splittrip.features.authentication.presentation.feature.RegisterFeature

fun NavGraphBuilder.loginGraph(onLoginSuccess: () -> Unit) {
    composable(Routes.LOGIN) {
        LoginFeature(
            onLoginSuccess = onLoginSuccess
        )
    }
    composable(Routes.REGISTER) {
        RegisterFeature(
            onRegisterSuccess = onLoginSuccess
        )
    }
    composable(Routes.FORGOT_PASSWORD) {
        ForgotPasswordFeature()
    }
}
