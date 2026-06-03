package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.getNameRes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.domain.enums.Currency
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.DefaultCurrencyScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DefaultCurrencyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun DefaultCurrencyFeature(viewModel: DefaultCurrencyViewModel = koinViewModel()) {
    val selectedCurrency by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()

    val navController = LocalRootNavController.current
    val scope = rememberCoroutineScope()
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    FeatureScaffold(currentRoute = Routes.SETTINGS_DEFAULT_CURRENCY) {
        DefaultCurrencyScreen(
            availableCurrencies = viewModel.availableCurrencies,
            selectedCurrencyCode = selectedCurrency,
            onCurrencySelected = { newCurrencyCode ->
                viewModel.onCurrencySelected(newCurrencyCode)

                val currency = try {
                    Currency.fromString(newCurrencyCode)
                } catch (_: Exception) {
                    null
                }
                val currencyName = if (currency != null) {
                    context.getString(currency.getNameRes())
                } else {
                    newCurrencyCode
                }

                pillController.showPill(
                    context.getString(
                        R.string.settings_preferences_currency_changed_format,
                        "$currencyName ($newCurrencyCode)"
                    )
                )

                scope.launch {
                    delay(UiConstants.NAV_FEEDBACK_DELAY)
                    navController.popBackStack()
                }
            }
        )
    }
}
