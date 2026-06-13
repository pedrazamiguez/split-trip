package es.pedrazamiguez.splittrip

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.google.firebase.appcheck.FirebaseAppCheck
import es.pedrazamiguez.splittrip.appcheck.createAppCheckProviderFactory
import es.pedrazamiguez.splittrip.appcheck.getDebugTokenFromPrefs
import es.pedrazamiguez.splittrip.appcheck.seedDebugToken
import es.pedrazamiguez.splittrip.core.logging.LogContext
import es.pedrazamiguez.splittrip.data.firebase.messaging.channel.NotificationChannelInitializer
import es.pedrazamiguez.splittrip.di.activityLoggingFeatureModules
import es.pedrazamiguez.splittrip.di.appModule
import es.pedrazamiguez.splittrip.di.authenticationFeatureModules
import es.pedrazamiguez.splittrip.di.balancesFeatureModules
import es.pedrazamiguez.splittrip.di.contributionsFeatureModules
import es.pedrazamiguez.splittrip.di.coreModules
import es.pedrazamiguez.splittrip.di.currenciesFeatureModules
import es.pedrazamiguez.splittrip.di.dataModules
import es.pedrazamiguez.splittrip.di.expensesFeatureModules
import es.pedrazamiguez.splittrip.di.groupsFeatureModules
import es.pedrazamiguez.splittrip.di.notificationModules
import es.pedrazamiguez.splittrip.di.profileFeatureModules
import es.pedrazamiguez.splittrip.di.settingsFeatureModules
import es.pedrazamiguez.splittrip.di.subunitsFeatureModules
import es.pedrazamiguez.splittrip.di.withdrawalsFeatureModules
import es.pedrazamiguez.splittrip.features.main.di.mainUiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.USE_DEBUG_APP_CHECK) {
            seedDebugToken(this)
        }

        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(createAppCheckProviderFactory())

        startKoin {
            androidContext(this@App)
            modules(
                appModule,
                coreModules,
                dataModules,

                mainUiModule,
                notificationModules,

                authenticationFeatureModules,
                balancesFeatureModules,
                activityLoggingFeatureModules,
                contributionsFeatureModules,
                currenciesFeatureModules,
                expensesFeatureModules,
                groupsFeatureModules,
                subunitsFeatureModules,
                profileFeatureModules,
                settingsFeatureModules,
                withdrawalsFeatureModules
            )
        }

        setupTimber()

        if (BuildConfig.USE_DEBUG_APP_CHECK) {
            probeAppCheckToken()
        }

        NotificationChannelInitializer.createChannels(this)
    }

    private fun probeAppCheckToken() {
        FirebaseAppCheck.getInstance()
            .getAppCheckToken(false)
            .addOnSuccessListener {
                val token = getDebugTokenFromPrefs(applicationContext)
                Timber.d("App Check: token obtained successfully ✓ (debug token: $token)")
            }
            .addOnFailureListener { e ->
                val token = getDebugTokenFromPrefs(applicationContext)
                Timber.e(e, "App Check: token exchange FAILED (debug token: $token)")
                if (token != null) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("App Check debug token", token))
                    Toast.makeText(
                        applicationContext,
                        "App Check FAILED ✗ — debug token copied to clipboard",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setupTimber() {
        val logContext = org.koin.core.context.GlobalContext.get().get<LogContext>()
        if (BuildConfig.DEBUG) {
            Timber.plant(es.pedrazamiguez.splittrip.core.logging.DevelopmentLogcatTree(logContext))
        } else {
            Timber.plant(es.pedrazamiguez.splittrip.core.logging.ProductionCrashlyticsTree(logContext))
        }
    }
}
