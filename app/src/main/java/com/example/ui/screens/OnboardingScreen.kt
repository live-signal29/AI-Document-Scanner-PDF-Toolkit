package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScanSessionViewModel

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun OnboardingScreen(
    viewModel: ScanSessionViewModel,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingPage(
            title = "Smart Document Scanner",
            subtitle = "Capture crystal-clear scans with automatic edge detection, perspective correction, and Magic Color filters.",
            icon = Icons.Default.CameraAlt,
            tag = "HIGH ACCURACY"
        ),
        OnboardingPage(
            title = "On-Device OCR Engine",
            subtitle = "Transform receipts, contracts, and printed documents into editable, searchable text directly on your device.",
            icon = Icons.Default.TextFields,
            tag = "OFFLINE & FAST"
        ),
        OnboardingPage(
            title = "AI Document Assistant",
            subtitle = "Generate executive summaries, extract key dates & amounts, and ask instant questions about your documents.",
            icon = Icons.Default.AutoAwesome,
            tag = "SMART INSIGHTS"
        ),
        OnboardingPage(
            title = "Privacy-First Architecture",
            subtitle = "Your sensitive documents remain completely private in local device storage. We never upload or share your files.",
            icon = Icons.Default.Lock,
            tag = "100% PRIVATE"
        )
    )

    var currentPageIndex by remember { mutableIntStateOf(0) }
    val currentPage = pages[currentPageIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("onboarding_screen")
    ) {
        // Skip Button
        if (currentPageIndex < pages.size - 1) {
            TextButton(
                onClick = {
                    viewModel.prefs.isOnboardingCompleted = true
                    onFinishOnboarding()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .testTag("onboarding_skip_btn")
            ) {
                Text(
                    text = "Skip",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Center Content Carousel
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = currentPage.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = currentPage.tag,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentPage.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = currentPage.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (index == currentPageIndex) 24.dp else 8.dp,
                                height = 8.dp
                            )
                            .background(
                                color = if (index == currentPageIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }

        // Bottom CTA Button
        Button(
            onClick = {
                if (currentPageIndex < pages.size - 1) {
                    currentPageIndex++
                } else {
                    viewModel.prefs.isOnboardingCompleted = true
                    onFinishOnboarding()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .height(52.dp)
                .testTag("onboarding_next_btn"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (currentPageIndex == pages.size - 1) "Get Started" else "Next",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
