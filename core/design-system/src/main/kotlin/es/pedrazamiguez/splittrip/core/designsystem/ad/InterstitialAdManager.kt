package es.pedrazamiguez.splittrip.core.designsystem.ad

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
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
                        Timber.d("InterstitialAdManager: Interstitial ad preloaded successfully")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        cachedInterstitialAd = null
                        isLoading = false
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
        val isEligibleByPolicy = isAdEnabled && hasMetFrequency && hasElapsedMinInterval
        val ad = cachedInterstitialAd

        if (isEligibleByPolicy && ad != null) {
            actionCounter = 0
            lastShownTimestampMs = currentTime

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    cachedInterstitialAd = null
                    onComplete()
                    preloadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Timber.w("InterstitialAdManager: Failed to show interstitial ad: %s", adError.message)
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
