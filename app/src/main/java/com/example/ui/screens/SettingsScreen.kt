package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScanSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScanSessionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLegal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPro by remember { mutableStateOf(viewModel.prefs.isProUser) }
    var isTestMode by remember { mutableStateOf(viewModel.prefs.isAdMobTestMode) }
    var currentQuality by remember { mutableStateOf(viewModel.prefs.pdfQuality) }
    var currentTheme by remember { mutableStateOf(viewModel.prefs.themeMode) }

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var customApiKeyInput by remember { mutableStateOf(viewModel.prefs.customAiApiKey) }

    var showQualityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Custom API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Configure Gemini AI API Key") },
            text = {
                Column {
                    Text(
                        text = "Optionally enter your own Google Gemini API key for advanced document intelligence. Keys are stored safely in local private app preferences.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customApiKeyInput,
                        onValueChange = { customApiKeyInput = it },
                        label = { Text("Gemini API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.prefs.customAiApiKey = customApiKeyInput.trim()
                        showApiKeyDialog = false
                        Toast.makeText(context, "API Key saved", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("api_key_save_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Quality Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("PDF Export Quality") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.prefs.pdfQuality = q
                                    currentQuality = q
                                    showQualityDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (q) {
                                    "HIGH" -> "High Quality (2048px - Best for archival)"
                                    "LOW" -> "Low Quality (1024px - Smallest size)"
                                    else -> "Balanced (1600px - Recommended)"
                                },
                                modifier = Modifier.weight(1f),
                                fontWeight = if (currentQuality == q) FontWeight.Bold else FontWeight.Normal
                            )
                            if (currentQuality == q) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PRO Upgrade Banner Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pro_upgrade_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPro) "PRO Plan Active" else "Upgrade to PRO",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPro) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isPro) "All ads removed, unlimited scans & full AI access enabled." else "No ads, unlimited scans, high-resolution exports & unlimited AI.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPro) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isPro) MaterialTheme.colorScheme.primary else Color(0xFFFFD700),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val next = !isPro
                                isPro = next
                                viewModel.prefs.isProUser = next
                                Toast.makeText(context, if (next) "PRO Mode Enabled!" else "Free Mode Enabled", Toast.LENGTH_SHORT).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isPro) MaterialTheme.colorScheme.primary else Color.White,
                                contentColor = if (isPro) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("pro_toggle_btn")
                        ) {
                            Text(
                                text = if (isPro) "Switch to Free (For Testing)" else "Unlock PRO",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // General Preferences
            item {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingsClickableItem(
                    title = "PDF Export Quality",
                    subtitle = currentQuality,
                    icon = Icons.Default.HighQuality,
                    onClick = { showQualityDialog = true }
                )
            }

            item {
                SettingsClickableItem(
                    title = "Gemini AI API Key",
                    subtitle = if (customApiKeyInput.isNotBlank()) "Configured • " + customApiKeyInput.take(6) + "…" else "Not set (Uses default/offline mode)",
                    icon = Icons.Default.Key,
                    onClick = { showApiKeyDialog = true }
                )
            }

            // Ads & Developer Section
            item {
                Text(
                    text = "Monetization & Ads",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AdMob Test Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isTestMode) "Using Google official sample ad IDs" else "Using Production Publisher Ad Unit IDs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isTestMode,
                            onCheckedChange = {
                                isTestMode = it
                                viewModel.prefs.isAdMobTestMode = it
                            }
                        )
                    }
                }
            }

            // Legal & About Section
            item {
                Text(
                    text = "About & Legal",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingsClickableItem(
                    title = "Privacy Policy",
                    subtitle = "Zero tracking of private scanned document content",
                    icon = Icons.Default.PrivacyTip,
                    onClick = { onNavigateToLegal("privacy") }
                )
            }

            item {
                SettingsClickableItem(
                    title = "Terms of Service",
                    subtitle = "Usage rights and local processing terms",
                    icon = Icons.Default.Description,
                    onClick = { onNavigateToLegal("terms") }
                )
            }

            item {
                SettingsClickableItem(
                    title = "About FX Signal Lab",
                    subtitle = "Developer & Publisher Profile",
                    icon = Icons.Default.Info,
                    onClick = { onNavigateToLegal("about") }
                )
            }

            item {
                SettingsClickableItem(
                    title = "Share AI PDF Scanner",
                    subtitle = "Tell friends & colleagues about the app",
                    icon = Icons.Default.Share,
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "AI PDF Scanner – Scan, OCR & PDF")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out AI PDF Scanner by FX Signal Lab for fast, clean, private document scanning, on-device OCR, and AI summaries!"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share App"))
                    }
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AI PDF Scanner – Scan, OCR & PDF",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Version 1.0.0 (Build 1) • FX Signal Lab",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
