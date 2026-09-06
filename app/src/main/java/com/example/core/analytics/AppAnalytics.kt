package com.example.core.analytics

import android.util.Log

object AppAnalytics {

    private const val TAG = "AppAnalytics"

    /**
     * Logs an anonymous app performance or feature event.
     * Document contents and sensitive data are strictly excluded.
     */
    fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        val sanitizedParams = params.filterKeys { key ->
            key in listOf("type", "page_count", "source", "format", "quality", "is_pro", "status")
        }
        Log.d(TAG, "Event: $name | params: $sanitizedParams")
        // Ready for Firebase Analytics or external SDK:
        // FirebaseAnalytics.getInstance(context).logEvent(name, bundle)
    }
}
