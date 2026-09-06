package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.ScanSessionViewModel
import java.io.File
import java.util.UUID

@Composable
fun CameraScanScreen(
    viewModel: ScanSessionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCrop: (pageIndex: Int) -> Unit,
    onNavigateToReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        // Informative Camera Permission UI
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To scan paper documents, receipts, contracts, and books into PDFs, the camera is needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Grant Camera Permission",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }
        return
    }

    // CameraX Implementation
    val draftPages by viewModel.draftPages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var isMultiPageMode by remember { mutableStateOf(true) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importImages(uris) {
                onNavigateToReview()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black).testTag("camera_scan_screen")) {
        // Camera Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setFlashMode(flashMode)
                        .build()
                    imageCapture = capture

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        // Handle camera bind failure gracefully
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Document Guide Reticle / Viewfinder overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 120.dp)
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
        )

        // Top Controls: Back, Flash, Mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(44.dp)
                    .testTag("camera_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Mode Selector: Single vs Batch
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                Text(
                    text = "Single",
                    color = if (!isMultiPageMode) Color.White else Color.White.copy(alpha = 0.6f),
                    fontWeight = if (!isMultiPageMode) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!isMultiPageMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isMultiPageMode = false }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text(
                    text = "Batch (Multi)",
                    color = if (isMultiPageMode) Color.White else Color.White.copy(alpha = 0.6f),
                    fontWeight = if (isMultiPageMode) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isMultiPageMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isMultiPageMode = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Flash Toggle
            IconButton(
                onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                        else -> ImageCapture.FLASH_MODE_AUTO
                    }
                    imageCapture?.flashMode = flashMode
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(44.dp)
                    .testTag("flash_toggle_btn")
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                        else -> Icons.Default.FlashAuto
                    } ,
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }
        }

        // Bottom Controls: Gallery Import, Shutter Button, Page Review / Done
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Import from gallery
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(54.dp)
                        .testTag("camera_gallery_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Import Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isLoading) Color.Gray else Color.White)
                        .clickable(enabled = !isLoading) {
                            val capture = imageCapture ?: return@clickable
                            val dir = File(context.filesDir, "captures").apply { if (!exists()) mkdirs() }
                            val photoFile = File(dir, "capture_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}.jpg")

                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            capture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        viewModel.addCapturedImage(photoFile) { pageIndex ->
                                            if (!isMultiPageMode) {
                                                onNavigateToCrop(pageIndex)
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        // Error handled gracefully
                                    }
                                }
                            )
                        }
                        .testTag("shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                }

                // Review / Finish Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            if (draftPages.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .clickable(enabled = draftPages.isNotEmpty()) {
                            onNavigateToReview()
                        }
                        .testTag("review_done_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    if (draftPages.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${draftPages.size}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "done",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}
