package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GridOff
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CyanPrecision
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseMask
import com.example.ui.theme.StudioHairline
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "CameraViewfinder"

@Composable
fun CameraViewfinderScreen(
    onPhotoCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(onBack = onClose)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var useFrontCamera by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var showGrid by remember { mutableStateOf(true) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(hasCameraPermission, useFrontCamera, flashMode) {
        if (!hasCameraPermission) return@LaunchedEffect
        try {
            val cameraProvider = context.getCameraProvider()
            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(flashMode)
                .build()

            val selector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            imageCapture = capture
            errorMessage = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind CameraX use cases", e)
            errorMessage = e.localizedMessage ?: "Unable to start camera viewfinder."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val future = ProcessCameraProvider.getInstance(context)
                if (future.isDone) {
                    future.get().unbindAll()
                }
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        if (!hasCameraPermission) {
            // Permission Request Card
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(StudioPanel)
                    .border(1.dp, StudioHairline, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                    tint = CyanPrecision,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Allow camera permission to shoot high-resolution photos directly into Lumina Lab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrecision,
                        contentColor = ObsidianVoid
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Grant Camera Permission",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Studio")
                }
            }
        } else {
            // Live CameraX PreviewView
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Rule of Thirds Viewfinder Grid
            if (showGrid) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val lineColor = Color.White.copy(alpha = 0.28f)
                    for (i in 1..2) {
                        val x = (w / 3f) * i
                        val y = (h / 3f) * i
                        drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.2f)
                        drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.2f)
                    }
                }
            }

            // Top Viewfinder Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianVoid.copy(alpha = 0.55f))
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("camera_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close camera",
                        tint = TextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                                else -> ImageCapture.FLASH_MODE_AUTO
                            }
                        },
                        modifier = Modifier.testTag("camera_flash_button")
                    ) {
                        val flashIcon = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Rounded.FlashOn
                            ImageCapture.FLASH_MODE_OFF -> Icons.Rounded.FlashOff
                            else -> Icons.Rounded.FlashAuto
                        }
                        Icon(
                            imageVector = flashIcon,
                            contentDescription = "Cycle flash mode",
                            tint = if (flashMode == ImageCapture.FLASH_MODE_ON) AmberWarm else TextPrimary
                        )
                    }

                    IconButton(
                        onClick = { showGrid = !showGrid },
                        modifier = Modifier.testTag("camera_grid_button")
                    ) {
                        Icon(
                            imageVector = if (showGrid) Icons.Rounded.GridOn else Icons.Rounded.GridOff,
                            contentDescription = "Toggle viewfinder grid",
                            tint = if (showGrid) CyanPrecision else TextSecondary
                        )
                    }
                }
            }

            // Error notice if binding failed (e.g. device without camera hardware)
            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioPanel.copy(alpha = 0.9f))
                        .border(1.dp, RoseMask, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoseMask,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Shutter & Lens Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianVoid.copy(alpha = 0.65f))
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 22.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(StudioPanel.copy(alpha = 0.85f))
                        .border(1.dp, StudioHairline, CircleShape)
                        .clickable { useFrontCamera = !useFrontCamera }
                        .testTag("camera_flip_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cameraswitch,
                        contentDescription = "Switch camera lens",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Tactile Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) CyanPrecision else Color.White)
                        .clickable(enabled = !isCapturing && imageCapture != null) {
                            val capture = imageCapture ?: return@clickable
                            isCapturing = true
                            captureToCacheFile(
                                context = context,
                                imageCapture = capture,
                                onSuccess = { uri ->
                                    isCapturing = false
                                    onPhotoCaptured(uri)
                                },
                                onError = { msg ->
                                    isCapturing = false
                                    errorMessage = msg
                                }
                            )
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = ObsidianVoid,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            { continuation.resume(future.get()) },
            ContextCompat.getMainExecutor(this)
        )
    }

private fun captureToCacheFile(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val dir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val outFile = File(dir, "CAP_$stamp.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(outFile)
                onSuccess(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed", exception)
                onError(exception.localizedMessage ?: "Failed to capture photo.")
            }
        }
    )
}
