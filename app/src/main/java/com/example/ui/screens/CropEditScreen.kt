package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.image.FilterMode
import com.example.ui.ScanSessionViewModel
import com.example.ui.components.CropOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropEditScreen(
    pageIndex: Int,
    viewModel: ScanSessionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(pageIndex) {
        viewModel.loadPageForEditing(pageIndex)
    }

    val currentBitmap by viewModel.currentEditBitmap.collectAsState()
    val currentQuad by viewModel.currentEditQuad.collectAsState()
    val currentFilter by viewModel.currentEditFilter.collectAsState()
    val currentRotation by viewModel.currentEditRotation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("crop_edit_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Adjust Page ${pageIndex + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("crop_back_btn")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveCurrentPageEdits {
                                onNavigateToReview()
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.testTag("crop_done_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Page",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                .background(Color.Black)
        ) {
            // Main Canvas Area: Image + Interactive Crop Overlay
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (currentBitmap != null) {
                    val bmp = currentBitmap!!
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Crop Target",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (currentQuad != null) {
                        CropOverlay(
                            quad = currentQuad!!,
                            imageWidth = bmp.width.toFloat(),
                            imageHeight = bmp.height.toFloat(),
                            onQuadChanged = { newQuad ->
                                viewModel.updateCurrentQuad(newQuad)
                            }
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            // Bottom Toolbar: Controls & Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // Adjustment Buttons: Auto-Detect, Full, Rotate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AdjustmentButton(
                        name = "Auto Detect",
                        icon = Icons.Default.AutoAwesome,
                        onClick = { viewModel.autoDetectCurrentQuad() },
                        modifier = Modifier.testTag("crop_auto_detect_btn")
                    )
                    AdjustmentButton(
                        name = "Full Frame",
                        icon = Icons.Default.CropFree,
                        onClick = { viewModel.resetQuadToFull() },
                        modifier = Modifier.testTag("crop_full_frame_btn")
                    )
                    AdjustmentButton(
                        name = "Rotate 90°",
                        icon = Icons.Default.RotateRight,
                        onClick = { viewModel.rotateCurrentPage() },
                        modifier = Modifier.testTag("crop_rotate_btn")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Filter Selector
                Text(
                    text = "Filter Enhancement",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        name = "Magic Color",
                        isSelected = currentFilter == FilterMode.DOCUMENT,
                        onClick = { viewModel.setCurrentFilter(FilterMode.DOCUMENT) },
                        modifier = Modifier.weight(1f).testTag("filter_magic_color")
                    )
                    FilterChip(
                        name = "B&W",
                        isSelected = currentFilter == FilterMode.BW,
                        onClick = { viewModel.setCurrentFilter(FilterMode.BW) },
                        modifier = Modifier.weight(1f).testTag("filter_bw")
                    )
                    FilterChip(
                        name = "Grayscale",
                        isSelected = currentFilter == FilterMode.GRAYSCALE,
                        onClick = { viewModel.setCurrentFilter(FilterMode.GRAYSCALE) },
                        modifier = Modifier.weight(1f).testTag("filter_grayscale")
                    )
                    FilterChip(
                        name = "Original",
                        isSelected = currentFilter == FilterMode.ORIGINAL,
                        onClick = { viewModel.setCurrentFilter(FilterMode.ORIGINAL) },
                        modifier = Modifier.weight(1f).testTag("filter_original")
                    )
                }
            }
        }
    }
}

@Composable
fun AdjustmentButton(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun FilterChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
