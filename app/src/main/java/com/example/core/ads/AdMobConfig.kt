package com.example.core.ads

import com.example.BuildConfig

/**
 * Central Configuration for Google AdMob Ads.
 *
 * To deploy with your real production AdMob IDs:
 * Replace the PRODUCTION_* values below with your verified AdMob Unit IDs,
 * or set isTestModeEnabled = false in release builds.
 */
object AdMobConfig {

    // =========================================================================
    // 1. PRODUCTION ADMOB IDS (Provided by Publisher FX Signal Lab)
    // =========================================================================
    const val PRODUCTION_APP_ID = "ca-app-pub-1895906484640218~7373211607"
    const val PRODUCTION_BANNER_ID = "ca-app-pub-1895906484640218/9453308985"
    const val PRODUCTION_INTERSTITIAL_ID = "ca-app-pub-1895906484640218/8024677245"
    const val PRODUCTION_REWARDED_ID = "ca-app-pub-1895906484640218/6080311232"

    // =========================================================================
    // 2. OFFICIAL GOOGLE TEST ADMOB IDS (Guaranteed safe for dev / automated test)
    // =========================================================================
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    /**
     * Resolves the active Banner Ad Unit ID.
     * Uses Test ID during Debug or when user toggles test mode in Settings.
     */
    fun getBannerAdUnitId(isTestMode: Boolean): String {
        return if (BuildConfig.DEBUG || isTestMode) TEST_BANNER_ID else PRODUCTION_BANNER_ID
    }

    /**
     * Resolves the active Interstitial Ad Unit ID.
     */
    fun getInterstitialAdUnitId(isTestMode: Boolean): String {
        return if (BuildConfig.DEBUG || isTestMode) TEST_INTERSTITIAL_ID else PRODUCTION_INTERSTITIAL_ID
    }

    /**
     * Resolves the active Rewarded Ad Unit ID.
     */
    fun getRewardedAdUnitId(isTestMode: Boolean): String {
        return if (BuildConfig.DEBUG || isTestMode) TEST_REWARDED_ID else PRODUCTION_REWARDED_ID
    }
}
