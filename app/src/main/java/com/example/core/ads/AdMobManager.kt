package com.example.core.ads

import android.app.Activity
import android.content.Context
import com.example.core.analytics.AppAnalytics
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobManager {

    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var lastInterstitialShowTime = 0L
    private const val INTERSTITIAL_COOLDOWN_MS = 60_000L // Minimum 60 seconds between interstitials

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                MobileAds.initialize(context) {
                    isInitialized = true
                }
            } catch (e: Exception) {
                // Ignore initialization failures in offline/restricted environments
            }
        }
    }

    /**
     * Pre-loads an interstitial ad.
     */
    fun loadInterstitial(context: Context, isTestMode: Boolean, isProUser: Boolean) {
        if (isProUser) return
        if (interstitialAd != null) return

        try {
            val adUnitId = AdMobConfig.getInterstitialAdUnitId(isTestMode)
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                    }
                }
            )
        } catch (e: Exception) {
            interstitialAd = null
        }
    }

    /**
     * Safely shows interstitial ad adhering to policy (rate-limited, never on start).
     */
    fun showInterstitial(
        activity: Activity,
        isProUser: Boolean,
        onAdDismissed: () -> Unit
    ) {
        if (isProUser) {
            onAdDismissed()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInterstitialShowTime < INTERSTITIAL_COOLDOWN_MS) {
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialShowTime = System.currentTimeMillis()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    AppAnalytics.logEvent("ad_shown", mapOf("type" to "interstitial"))
                }
            }
            ad.show(activity)
        } else {
            onAdDismissed()
        }
    }

    /**
     * Load rewarded ad for bonus scans or AI queries.
     */
    fun loadRewarded(context: Context, isTestMode: Boolean, onLoaded: () -> Unit = {}) {
        try {
            val adUnitId = AdMobConfig.getRewardedAdUnitId(isTestMode)
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context,
                adUnitId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        onLoaded()
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                    }
                }
            )
        } catch (e: Exception) {
            rewardedAd = null
        }
    }

    fun showRewarded(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    onAdClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    AppAnalytics.logEvent("ad_shown", mapOf("type" to "rewarded"))
                }
            }
            ad.show(activity) { _ ->
                onRewardEarned()
            }
        } else {
            onAdClosed()
        }
    }
}
