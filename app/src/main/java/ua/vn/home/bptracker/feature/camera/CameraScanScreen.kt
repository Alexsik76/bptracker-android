package ua.vn.home.bptracker.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ua.vn.home.bptracker.R

@Composable
fun CameraScanScreen(
    onCapture: (Bitmap) -> Unit,
    onEnterManually: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        val imageCapture = remember { ImageCapture.Builder().build() }
        val previewView = remember { 
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            ) { view ->
                view.post {
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(view.surfaceProvider)
                        }

                        val viewPort = view.viewPort
                        if (viewPort != null) {
                            val useCaseGroup = UseCaseGroup.Builder()
                                .addUseCase(preview)
                                .addUseCase(imageCapture)
                                .setViewPort(viewPort)
                                .build()

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    useCaseGroup
                                )
                            } catch (e: Exception) {
                                Log.e("CameraScan", "Use case binding failed", e)
                            }
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            }

            // Viewfinder with Dimmed Background
            val viewfinderSize = with(density) { Size(280.dp.toPx(), 220.dp.toPx()) }
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val left = (canvasWidth - viewfinderSize.width) / 2
                val top = (canvasHeight - viewfinderSize.height) / 2
                
                val viewfinderRect = Rect(Offset(left, top), viewfinderSize)

                // 1. Draw dimmed overlay
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    size = size
                )

                // 2. "Cut out" the viewfinder area
                drawRect(
                    color = Color.Transparent,
                    topLeft = viewfinderRect.topLeft,
                    size = viewfinderRect.size,
                    blendMode = BlendMode.Clear
                )
            }

            // Viewfinder Border
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(280.dp, 220.dp)
                        .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                )
            }

            // UI Overlays
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hint
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.camera_hint),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 16.sp
                    )
                }

                // Enter Manually
                Text(
                    text = stringResource(R.string.camera_enter_manually),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.Underline),
                    modifier = Modifier
                        .padding(bottom = 40.dp)
                        .clickable { onEnterManually() }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.clickable { onCancel() }
                    )

                    // Shutter Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(4.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                capturePhoto(imageCapture, context, onCapture)
                            }
                    )

                    // Spacer to balance the Cancel button
                    Spacer(modifier = Modifier.width(60.dp))
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required", color = Color.White)
        }
    }
}

private fun capturePhoto(
    imageCapture: ImageCapture,
    context: android.content.Context,
    onCapture: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val rotation = image.imageInfo.rotationDegrees
                val cropRect = image.cropRect
                val fullBitmap = image.toBitmap()
                
                // Crop the bitmap to the viewfinder area (ViewPort)
                val croppedBitmap = Bitmap.createBitmap(
                    fullBitmap,
                    cropRect.left,
                    cropRect.top,
                    cropRect.width(),
                    cropRect.height()
                )
                
                image.close()

                val processed = processBitmap(croppedBitmap, rotation)
                onCapture(processed)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScan", "Capture failed", exception)
            }
        }
    )
}

private fun processBitmap(bitmap: Bitmap, rotation: Int): Bitmap {
    val matrix = Matrix().apply {
        postRotate(rotation.toFloat())
    }
    
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    
    // Downscale to ~1024px on long edge
    val targetSize = 1024f
    val scale = if (rotated.width > rotated.height) {
        targetSize / rotated.width
    } else {
        targetSize / rotated.height
    }
    
    if (scale >= 1f) return rotated

    val scaled = Bitmap.createScaledBitmap(
        rotated,
        (rotated.width * scale).toInt(),
        (rotated.height * scale).toInt(),
        true
    )
    return scaled
}

/**
 * ImageProxy to Bitmap helper
 */
private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
