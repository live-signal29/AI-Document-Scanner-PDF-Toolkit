package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_pdf_scanner_prefs", Context.MODE_PRIVATE)

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var pdfQuality: String
        get() = prefs.getString(KEY_PDF_QUALITY, "MEDIUM") ?: "MEDIUM"
        set(value) = prefs.edit().putString(KEY_PDF_QUALITY, value).apply()

    var ocrLanguage: String
        get() = prefs.getString(KEY_OCR_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_OCR_LANGUAGE, value).apply()

    var isProUser: Boolean
        get() = prefs.getBoolean(KEY_IS_PRO, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_PRO, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var sortOption: String
        get() = prefs.getString(KEY_SORT_OPTION, "NEWEST") ?: "NEWEST"
        set(value) = prefs.edit().putString(KEY_SORT_OPTION, value).apply()

    var isGridView: Boolean
        get() = prefs.getBoolean(KEY_IS_GRID_VIEW, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_GRID_VIEW, value).apply()

    var isAdMobTestMode: Boolean
        get() = prefs.getBoolean(KEY_ADMOB_TEST_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_ADMOB_TEST_MODE, value).apply()

    var customAiApiKey: String
        get() = prefs.getString(KEY_CUSTOM_AI_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_AI_KEY, value).apply()

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_PDF_QUALITY = "pdf_quality"
        private const val KEY_OCR_LANGUAGE = "ocr_language"
        private const val KEY_IS_PRO = "is_pro_user"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_SORT_OPTION = "sort_option"
        private const val KEY_IS_GRID_VIEW = "is_grid_view"
        private const val KEY_ADMOB_TEST_MODE = "admob_test_mode"
        private const val KEY_CUSTOM_AI_KEY = "custom_ai_api_key"
    }
}
