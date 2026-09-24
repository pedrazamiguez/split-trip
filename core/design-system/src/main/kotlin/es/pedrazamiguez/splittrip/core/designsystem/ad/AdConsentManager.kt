package es.pedrazamiguez.splittrip.core.designsystem.ad

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

object AdConsentManager {

    private val isMobileAdsInitialized = AtomicBoolean(false)

    fun gatherConsentAndInitialize(
        activity: Activity,
        onConsentCompleted: () -> Unit = {}
    ) {
        try {
            val params = ConsentRequestParameters.Builder().build()
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Timber.w(
                                "Consent form presentation error: %s: %s",
                                formError.errorCode,
                                formError.message
                            )
                        }
                        if (consentInformation.canRequestAds()) {
                            initializeMobileAds(activity)
                        }
                        onConsentCompleted()
                    }
                },
                { requestConsentError ->
                    Timber.w(
                        "Consent info update failed: %s: %s",
                        requestConsentError.errorCode,
                        requestConsentError.message
                    )
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAds(activity)
                    }
                    onConsentCompleted()
                }
            )

            if (consentInformation.canRequestAds()) {
                initializeMobileAds(activity)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to gather consent or initialize Mobile Ads")
            onConsentCompleted()
        }
    }

    private fun initializeMobileAds(context: Context) {
        if (isMobileAdsInitialized.compareAndSet(false, true)) {
            try {
                MobileAds.initialize(context) { initializationStatus ->
                    Timber.d("Google Mobile Ads initialized: %s", initializationStatus)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during MobileAds.initialize")
            }
        }
    }
}
