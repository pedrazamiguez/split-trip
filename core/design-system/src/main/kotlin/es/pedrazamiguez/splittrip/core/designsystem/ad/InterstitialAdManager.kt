package es.pedrazamiguez.splittrip.core.designsystem.ad

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.usecase.ad.ShouldShowAdsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class InterstitialAdManager(
    private val appConfigRepository: AppConfigRepository,
    private val shouldShowAdsUseCase: ShouldShowAdsUseCase,
    private val telemetryTracker: TelemetryTracker,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()),
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var isAdEnabled: Boolean = false
    private var actionCounter: Int = 0
    private var lastShownTimestampMs: Long = 0L
    private var cachedInterstitialAd: InterstitialAd? = null
    private var isLoading: Boolean = false

    init {
        coroutineScope.launch {
            shouldShowAdsUseCase().collect { enabled ->
                isAdEnabled = enabled
                if (!enabled) {
                    cachedInterstitialAd = null
                }
            }
        }
    }

    fun preloadAd(context: Context) {
        if (!isAdEnabled || cachedInterstitialAd != null || isLoading) {
            return
        }

        val adUnitId = appConfigRepository.admobInterstitialAdUnitId.value
        if (adUnitId.isBlank()) {
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()
        try {
            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        cachedInterstitialAd = interstitialAd
                        isLoading = false
                        telemetryTracker.trackEvent(
                            MonetizationTelemetry.Event.AD_LOADED,
                            mapOf(MonetizationTelemetry.Param.AD_TYPE to MonetizationTelemetry.AdType.INTERSTITIAL)
                        )
                        Timber.d("InterstitialAdManager: Interstitial ad preloaded successfully")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        cachedInterstitialAd = null
                        isLoading = false
                        telemetryTracker.trackEvent(
                            MonetizationTelemetry.Event.AD_LOAD_FAILED,
                            mapOf(
                                MonetizationTelemetry.Param.AD_TYPE to MonetizationTelemetry.AdType.INTERSTITIAL,
                                MonetizationTelemetry.Param.ERROR_CODE to loadAdError.code
                            )
                        )
                        Timber.w(
                            "InterstitialAdManager: Failed to preload interstitial ad (%s: %s)",
                            loadAdError.code,
                            loadAdError.message
                        )
                    }
                }
            )
        } catch (e: Exception) {
            isLoading = false
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.AD_LOAD_FAILED,
                mapOf(
                    MonetizationTelemetry.Param.AD_TYPE to MonetizationTelemetry.AdType.INTERSTITIAL,
                    MonetizationTelemetry.Param.FAILURE_STAGE to MonetizationTelemetry.FailureStage.AD_LOAD_EXCEPTION
                )
            )
            Timber.e(e, "InterstitialAdManager: Exception during InterstitialAd.load")
        }
    }

    fun onActionCompleted(activity: Activity, onComplete: () -> Unit) {
        actionCounter++

        val frequency = appConfigRepository.adInterstitialActionFrequency.value
        val minIntervalMs = appConfigRepository.adInterstitialMinIntervalSeconds.value * 1000L
        val currentTime = timeProvider()
        val hasElapsedMinInterval = (currentTime - lastShownTimestampMs) >= minIntervalMs
        val hasMetFrequency = actionCounter >= frequency
        val ad = cachedInterstitialAd

        val eligibilityReason = resolveEligibilityReason(
            isAdEnabled = isAdEnabled,
            hasMetFrequency = hasMetFrequency,
            hasElapsedMinInterval = hasElapsedMinInterval,
            hasCachedAd = ad != null
        )
        telemetryTracker.trackEvent(
            MonetizationTelemetry.Event.AD_ELIGIBILITY_EVALUATED,
            mapOf(
                MonetizationTelemetry.Param.AD_TYPE to MonetizationTelemetry.AdType.INTERSTITIAL,
                MonetizationTelemetry.Param.IS_ELIGIBLE to
                    (eligibilityReason == MonetizationTelemetry.AdEligibilityReason.ELIGIBLE),
                MonetizationTelemetry.Param.REASON to eligibilityReason
            )
        )

        if (eligibilityReason == MonetizationTelemetry.AdEligibilityReason.ELIGIBLE && ad != null) {
            actionCounter = 0
            lastShownTimestampMs = currentTime

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.AD_IMPRESSION,
                        mapOf(MonetizationTelemetry.Param.AD_TYPE to MonetizationTelemetry.AdType.INTERSTITIAL)
                    )
                }

                override fun onAdDismissedFullScreenContent() {
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.AD_DISMISSED,
                        mapOf(MonetizationTelemetry.Param.AD_TYPE to MonetizationTelemetry.AdType.INTERSTITIAL)
                    )
                    cachedInterstitialAd = null
                    onComplete()
                    preloadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Timber.w("InterstitialAdManager: Failed to show interstitial ad: %s", adError.message)
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.AD_SHOW_FAILED,
                        mapOf(
                            MonetizationTelemetry.Param.AD_TYPE to MonetizationTelemetry.AdType.INTERSTITIAL,
                            MonetizationTelemetry.Param.ERROR_CODE to adError.code
                        )
                    )
                    cachedInterstitialAd = null
                    onComplete()
                    preloadAd(activity)
                }
            }
            ad.show(activity)
        } else {
            onComplete()
            if (isAdEnabled && cachedInterstitialAd == null) {
                preloadAd(activity)
            }
        }
    }

    private fun resolveEligibilityReason(
        isAdEnabled: Boolean,
        hasMetFrequency: Boolean,
        hasElapsedMinInterval: Boolean,
        hasCachedAd: Boolean
    ): String = when {
        !isAdEnabled -> MonetizationTelemetry.AdEligibilityReason.ADS_DISABLED
        !hasMetFrequency -> MonetizationTelemetry.AdEligibilityReason.FREQUENCY_CAP
        !hasElapsedMinInterval -> MonetizationTelemetry.AdEligibilityReason.MIN_INTERVAL_COOLDOWN
        !hasCachedAd -> MonetizationTelemetry.AdEligibilityReason.NO_CACHED_AD
        else -> MonetizationTelemetry.AdEligibilityReason.ELIGIBLE
    }

    // Visible for testing
    internal fun setCachedAd(ad: InterstitialAd?) {
        cachedInterstitialAd = ad
    }

    internal fun getCachedAd(): InterstitialAd? = cachedInterstitialAd

    internal fun setActionCounter(counter: Int) {
        actionCounter = counter
    }

    internal fun getActionCounter(): Int = actionCounter

    internal fun setAdEnabled(enabled: Boolean) {
        isAdEnabled = enabled
    }

    internal fun isAdEnabled(): Boolean = isAdEnabled

    internal fun setLastShownTimestamp(timestamp: Long) {
        lastShownTimestampMs = timestamp
    }
}
