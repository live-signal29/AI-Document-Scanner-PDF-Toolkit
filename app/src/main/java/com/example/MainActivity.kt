package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.core.ads.AdMobManager
import com.example.core.analytics.AppAnalytics
import com.example.ui.ScanSessionViewModel
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.AIPdfScannerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ScanSessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Mobile Ads SDK (asynchronously, safe)
        AdMobManager.initialize(this)
        AdMobManager.loadInterstitial(
            this,
            isTestMode = viewModel.prefs.isAdMobTestMode,
            isProUser = viewModel.prefs.isProUser
        )

        AppAnalytics.logEvent("app_open")

        setContent {
            val themePref = viewModel.prefs.themeMode
            val darkTheme = when (themePref) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            AIPdfScannerTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

