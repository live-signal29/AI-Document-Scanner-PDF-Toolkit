package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    type: String, // "privacy", "terms", "about"
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (type) {
        "privacy" -> "Privacy Policy"
        "terms" -> "Terms of Service"
        else -> "About FX Signal Lab"
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("legal_screen_$type"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    when (type) {
                        "privacy" -> {
                            LegalSectionHeader("1. Local-First Processing Commitment")
                            LegalBody("AI PDF Scanner by FX Signal Lab is engineered on strict local-first privacy principles. All document capturing, perspective cropping, image enhancement, and optical character recognition (OCR) are performed entirely on your device using on-device ML Kit models.")

                            LegalSectionHeader("2. Document Content Confidentiality")
                            LegalBody("We never store, upload, transmit, or sell your scanned documents, receipts, identification cards, contracts, or personal photographs. Your files reside solely in your device's secured private application storage directory.")

                            LegalSectionHeader("3. Optional AI Features")
                            LegalBody("If you explicitly choose to utilize online AI features (such as Gemini AI Summaries or translations), the text extracted from that specific document is securely processed over encrypted HTTPS strictly to generate the requested analysis. No document content is used for training public models.")

                            LegalSectionHeader("4. Advertisements & Anonymous Analytics")
                            LegalBody("To support free development, Google AdMob SDK is used to display non-intrusive advertisements. AdMob may collect anonymous device identifiers in compliance with Google Play Developer Program Policies. Pro subscribers enjoy a completely ad-free experience.")

                            LegalSectionHeader("5. Data Deletion & Trash Bin")
                            LegalBody("You have total control over your data. Deleting documents from the app permanently removes them from device storage. You can empty the trash at any time.")
                        }
                        "terms" -> {
                            LegalSectionHeader("1. Acceptance of Terms")
                            LegalBody("By downloading or using AI PDF Scanner, published by FX Signal Lab, you agree to these Terms of Service. If you do not agree, please uninstall the application.")

                            LegalSectionHeader("2. Permitted Use")
                            LegalBody("AI PDF Scanner provides document digitization, OCR, and PDF utility tools. You are solely responsible for ensuring you have the legal right and authorization to scan and store the documents processed.")

                            LegalSectionHeader("3. Pro Subscription & In-App Features")
                            LegalBody("Certain premium features, including ad removal and enhanced AI processing limits, may require a PRO subscription or one-time upgrade. Purchases are managed through Google Play Billing.")

                            LegalSectionHeader("4. Disclaimer of Warranty")
                            LegalBody("The software is provided 'as is' without warranty of any kind. While our OCR and processing algorithms strive for maximum accuracy, FX Signal Lab does not guarantee that OCR text extraction will be 100% error-free for all handwriting or degraded prints.")
                        }
                        else -> {
                            LegalSectionHeader("About FX Signal Lab")
                            LegalBody("FX Signal Lab is dedicated to building state-of-the-art mobile productivity and utility applications. Our mission is to combine powerful on-device machine learning with intuitive, beautiful Material 3 interfaces that respect user privacy.")

                            LegalSectionHeader("AI PDF Scanner – Scan, OCR & PDF")
                            LegalBody("Designed for professionals, students, accountants, and remote workers who need fast, dependable document scanning on Android. Engineered with Kotlin, Jetpack Compose, CameraX, Room, and Google ML Kit.")

                            LegalSectionHeader("Support & Feedback")
                            LegalBody("Have questions, bug reports, or feature requests? Contact our team at:\nsupport@fxsignallab.com")

                            LegalSectionHeader("Third-Party Acknowledgements")
                            LegalBody("• Google ML Kit Vision (Apache 2.0)\n• Android Jetpack & Compose (Apache 2.0)\n• Google AdMob SDK\n• Coil Image Loader")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegalSectionHeader(text: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
fun LegalBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 22.sp
    )
}
