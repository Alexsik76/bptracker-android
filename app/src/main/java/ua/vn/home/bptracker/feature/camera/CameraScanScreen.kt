package ua.vn.home.bptracker.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaActionSound
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.ui.theme.spacing
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

private val FRAME_WIDTH = 280.dp
private val FRAME_HEIGHT = 220.dp
private const val FRAME_MARGIN = 0.10f

@Composable
fun CameraScanScreen(
    onCapture: (Bitmap) -> Unit,
    onEnterManually: () -> Unit,
    onCancel: () -> Unit,
    onLiveResult: (Bitmap, Int, Int, Int) -> Unit = { _, _, _, _ -> },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val mediaActionSound = remember { MediaActionSound() }
    val shutterSoundEnabled by ServiceLocator.settingsStore.liveOcrShutterSound.collectAsState(initial = true)
    val currentShutterSoundEnabled by rememberUpdatedState(shutterSoundEnabled)
    var voteProgress by remember { mutableIntStateOf(0) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderRef = remember { AtomicReference<ProcessCameraProvider?>(null) }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            android.util.Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
            )
            .build()
    }

    val liveOcrAnalyzer = remember {
        LiveOcrAnalyzer(
            ocrEngine = ServiceLocator.ocrEngine,
            onProgress = { minVotes ->
                voteProgress = minVotes
            },
            onConsensus = { session ->
                imageAnalysis.clearAnalyzer()
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                if (currentShutterSoundEnabled) {
                    mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
                }
                onLiveResult(session.bitmap, session.sys, session.dia, session.pul)
            }
        )
    }

    DisposableEffect(Unit) {
        imageAnalysis.setAnalyzer(analyzerExecutor, liveOcrAnalyzer)
        onDispose {
            imageAnalysis.clearAnalyzer()
            cameraProviderRef.get()?.unbind(imageAnalysis)
            liveOcrAnalyzer.stop()
            analyzerExecutor.shutdown()
            mediaActionSound.release()
        }
    }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

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
                modifier = Modifier.fillMaxSize(),
            ) { view ->
                view.post {
                    cameraProviderFuture.addListener(
                        {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProviderRef.set(cameraProvider)
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = view.surfaceProvider
                            }

                            val viewPort = view.viewPort
                            if (viewPort != null) {
                                val useCaseGroup = UseCaseGroup.Builder()
                                    .addUseCase(preview)
                                    .addUseCase(imageCapture)
                                    .addUseCase(imageAnalysis)
                                    .setViewPort(viewPort)
                                    .build()

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        useCaseGroup,
                                    )
                                } catch (e: Exception) {
                                    Log.e("CameraScan", "Use case binding failed", e)
                                }
                            }
                        },
                        ContextCompat.getMainExecutor(context),
                    )
                }
            }

            // Viewfinder with Dimmed Background
            val frameWidthPx = with(density) { FRAME_WIDTH.toPx() }
            val frameHeightPx = with(density) { FRAME_HEIGHT.toPx() }
            val viewfinderSize = Size(frameWidthPx, frameHeightPx)
            
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
                        .size(FRAME_WIDTH, FRAME_HEIGHT)
                        .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                )
            }

            // UI Overlays
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = MaterialTheme.spacing.large),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hint
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.camera_hint),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 16.sp
                    )
                }

                // Progress Indicator: 3 dots filling according to minimum per-field vote count (0..3)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    for (i in 1..3) {
                        val filled = voteProgress >= i
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (filled) Color(0xFF66BB6A) else Color.White.copy(alpha = 0.3f))
                        )
                    }
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
                                imageAnalysis.clearAnalyzer()
                                cameraProviderRef.get()?.unbind(imageAnalysis)
                                liveOcrAnalyzer.stop()
                                capturePhoto(
                                    imageCapture = imageCapture,
                                    context = context,
                                    viewWidthPx = previewView.width.toFloat(),
                                    viewHeightPx = previewView.height.toFloat(),
                                    frameWidthPx = frameWidthPx,
                                    frameHeightPx = frameHeightPx,
                                    onCapture = onCapture,
                                )
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
    viewWidthPx: Float,
    viewHeightPx: Float,
    frameWidthPx: Float,
    frameHeightPx: Float,
    onCapture: (Bitmap) -> Unit,
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val rotation = image.imageInfo.rotationDegrees
                val cropRect = image.cropRect
                val fullBitmap = image.toBitmap()
                image.close()

                // 1. ViewPort crop (as now)
                val viewPortCropped = Bitmap.createBitmap(
                    fullBitmap,
                    cropRect.left,
                    cropRect.top,
                    cropRect.width(),
                    cropRect.height(),
                )
                if (viewPortCropped !== fullBitmap) {
                    fullBitmap.recycle()
                }

                // 2. Rotate (as now)
                val matrix = Matrix().apply {
                    postRotate(rotation.toFloat())
                }
                val rotated = Bitmap.createBitmap(
                    viewPortCropped,
                    0,
                    0,
                    viewPortCropped.width,
                    viewPortCropped.height,
                    matrix,
                    true,
                )
                if (rotated !== viewPortCropped) {
                    viewPortCropped.recycle()
                }

                // 3. Viewfinder crop
                val crop = viewfinderCropRect(
                    imageWidth = rotated.width,
                    imageHeight = rotated.height,
                    viewWidthPx = viewWidthPx,
                    viewHeightPx = viewHeightPx,
                    frameWidthPx = frameWidthPx,
                    frameHeightPx = frameHeightPx,
                    margin = FRAME_MARGIN,
                )
                val viewfinderCropped = Bitmap.createBitmap(
                    rotated,
                    crop.left,
                    crop.top,
                    crop.width,
                    crop.height,
                )
                if (viewfinderCropped !== rotated) {
                    rotated.recycle()
                }

                // 4. Downscale to <= 1024 px on long edge (as now)
                val targetSize = 1024f
                val scale = if (viewfinderCropped.width > viewfinderCropped.height) {
                    targetSize / viewfinderCropped.width
                } else {
                    targetSize / viewfinderCropped.height
                }
                val finalBitmap = if (scale < 1f) {
                    val scaled = viewfinderCropped.scale(
                        width = (viewfinderCropped.width * scale).toInt(),
                        height = (viewfinderCropped.height * scale).toInt(),
                        filter = true,
                    )
                    if (scaled !== viewfinderCropped) {
                        viewfinderCropped.recycle()
                    }
                    scaled
                } else {
                    viewfinderCropped
                }

                Log.d("CameraScan", "Final bitmap size: ${finalBitmap.width}x${finalBitmap.height}")
                onCapture(finalBitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScan", "Capture failed", exception)
            }
        }
    )
}

/**
 * ImageProxy to Bitmap helper
 */

