package com.posturecoach.ui.scan

import android.Manifest
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.posturecoach.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    onCaptured: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val cameraPerm = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) {
        if (!cameraPerm.status.isGranted) cameraPerm.launchPermissionRequest()
    }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        val current = state
        if (current is ScanViewModel.State.Success) {
            onCaptured(current.scanId)
            viewModel.reset()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraPerm.status is com.google.accompanist.permissions.PermissionStatus.Granted) {
            CameraContent(
                analyzing = state is ScanViewModel.State.Analyzing,
                onCapture = viewModel::analyze,
                onCancel = onCancel,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.grant_camera),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
            }
        }

        if (state is ScanViewModel.State.Error) {
            ErrorBanner(
                message = stringResource(R.string.no_pose_detected),
                onDismiss = viewModel::reset,
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private val com.google.accompanist.permissions.PermissionStatus.isGranted: Boolean
    get() = this is com.google.accompanist.permissions.PermissionStatus.Granted

@Composable
private fun CameraContent(
    analyzing: Boolean,
    onCapture: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER } }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture = remember(lensFacing) { ImageCapture.Builder().build() }

    DisposableEffect(lensFacing) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { providerFuture.get().unbindAll() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        SilhouetteOverlay(modifier = Modifier.fillMaxSize())
        TopBar(onCancel = onCancel)
        BottomBar(
            analyzing = analyzing,
            onCapture = {
                val file = newImageFile(context)
                val output = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture.takePicture(
                    output,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exception: ImageCaptureException) = Unit
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            onCapture(file.absolutePath)
                        }
                    },
                )
            },
            onFlip = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            },
        )

        if (analyzing) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.analyzing),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TopBar(onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f)),
        ) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel), tint = Color.White)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun BoxScope.BottomBar(
    analyzing: Boolean,
    onCapture: () -> Unit,
    onFlip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.scan_tip),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Spacer(Modifier.width(56.dp))
            Spacer(Modifier.weight(1f))
            CaptureButton(enabled = !analyzing, onClick = onCapture)
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onFlip,
                enabled = !analyzing,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .size(56.dp),
            ) {
                Icon(Icons.Filled.Cameraswitch, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun CaptureButton(enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = alpha)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxSize()) {
            // Tap target; visuals handled by parent box.
        }
    }
}

@Composable
private fun SilhouetteOverlay(modifier: Modifier = Modifier) {
    val color = Color.White.copy(alpha = 0.55f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val torsoTop = h * 0.20f
        val torsoBottom = h * 0.65f
        val headRadius = w * 0.07f
        val torsoWidth = w * 0.16f
        val legBottom = h * 0.95f
        val stroke = Stroke(
            width = 4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 14f), 0f),
        )
        drawCircle(color, radius = headRadius, center = Offset(cx, torsoTop - headRadius * 1.1f), style = stroke)
        drawRoundRect(
            color = color,
            topLeft = Offset(cx - torsoWidth / 2f, torsoTop),
            size = Size(torsoWidth, torsoBottom - torsoTop),
            style = stroke,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(torsoWidth * 0.4f, torsoWidth * 0.4f),
        )
        drawLine(
            color,
            start = Offset(cx - torsoWidth * 0.25f, torsoBottom),
            end = Offset(cx - torsoWidth * 0.25f, legBottom),
            strokeWidth = 4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 14f), 0f),
        )
        drawLine(
            color,
            start = Offset(cx + torsoWidth * 0.25f, torsoBottom),
            end = Offset(cx + torsoWidth * 0.25f, legBottom),
            strokeWidth = 4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 14f), 0f),
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

private fun newImageFile(context: Context): File {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val dir = File(context.filesDir, "scans").apply { mkdirs() }
    return File(dir, "scan_$stamp.jpg")
}
