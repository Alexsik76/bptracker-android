package ua.vn.home.bptracker.feature.home

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.ui.OperationUiState
import ua.vn.home.bptracker.ui.theme.*

private enum class Field(
    val labelRes: Int,
    val color: Color,
    val hasUnit: Boolean,
    val hasHeartIcon: Boolean,
    val fontSize: TextUnit
) {
    SYS(R.string.scan_review_sys, ColorSys, hasUnit = true, hasHeartIcon = false, fontSize = 56.sp),
    DIA(R.string.scan_review_dia, ColorDia, hasUnit = true, hasHeartIcon = false, fontSize = 44.sp),
    PUL(R.string.scan_review_pul, ColorPulse, hasUnit = false, hasHeartIcon = true, fontSize = 30.sp)
}

private const val MaxEditDigits = 3

private val SwipeUpThreshold = 80.dp

private val TopBarTitleStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp
)

private val ErrorTextStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp
)

private val LabelTextStyle = TextStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    letterSpacing = 0.5.sp
)

private val UnitTextStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp
)

private val EmptyValueTextStyle = TextStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.Medium,
    fontSize = 20.sp,
    lineHeight = 20.sp,
    textAlign = TextAlign.End
)

private val RetakeButtonStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp
)

private val SaveButtonStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp
)

private val MenuItemTextStyle = TextStyle(
    fontFamily = DmSans,
    fontSize = 15.sp
)

private fun valueTextStyle(fontSize: TextUnit, color: Color) = TextStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.Medium,
    fontSize = fontSize,
    lineHeight = fontSize,
    letterSpacing = (-1).sp,
    textAlign = TextAlign.End,
    color = color
)

@Composable
fun ScanReviewScreen(
    state: ScanReviewState?,
    onSysChange: (String) -> Unit,
    onDiaChange: (String) -> Unit,
    onPulseChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetake: () -> Unit,
    onRemoteOcr: () -> Unit,
    onBack: () -> Unit
) {
    val readyState = state as? ScanReviewState.Ready
    val recognizingState = state as? ScanReviewState.Recognizing

    var editing by rememberSaveable { mutableStateOf<Field?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    var showFullscreenViewer by rememberSaveable { mutableStateOf(false) }

    val startEdit: (Field, String) -> Unit = { field, current ->
        draft = current
        editing = field
    }

    val discardEdit: () -> Unit = {
        editing = null
    }

    val confirmEdit: (Field) -> Unit = { field ->
        when (field) {
            Field.SYS -> onSysChange(draft)
            Field.DIA -> onDiaChange(draft)
            Field.PUL -> onPulseChange(draft)
        }
        editing = null
    }

    // Only intercept back while a value is being edited; overview back is handled by the caller
    BackHandler(enabled = editing != null) {
        discardEdit()
    }

    LaunchedEffect(readyState?.saveOperation) {
        if (readyState?.saveOperation is OperationUiState.Success) onBack()
    }

    val image = readyState?.image ?: recognizingState?.image

    if (showFullscreenViewer && image != null) {
        FullscreenImageViewer(
            image = image,
            onDismiss = { showFullscreenViewer = false }
        )
    }

    val activeField = editing
    if (activeField != null) {
        FocusedValueEditor(
            field = activeField,
            draft = draft,
            image = image,
            onOpenViewer = { showFullscreenViewer = true },
            onSwipeUp = discardEdit,
            onDigit = { digit ->
                if (draft.length < MaxEditDigits) draft += digit
            },
            onBackspace = {
                draft = draft.dropLast(1)
            },
            onConfirm = { confirmEdit(activeField) }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.local_ocr_result_title),
                        style = TopBarTitleStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (readyState != null) {
                var menuExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val splitButtonShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(splitButtonShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                splitButtonShape
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(onClick = onRetake),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.local_ocr_retake),
                                    style = RetakeButtonStyle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                            )

                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .fillMaxHeight()
                                    .clickable { menuExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = stringResource(R.string.scan_review_more_actions_cd),
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.local_ocr_server_recognize),
                                        style = MenuItemTextStyle
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRemoteOcr()
                                }
                            )
                        }
                    }

                    val saveEnabled = readyState.isValid && readyState.saveOperation !is OperationUiState.InProgress
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        enabled = saveEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                            contentColor = DarkBgPrimary,
                            disabledContentColor = DarkBgPrimary.copy(alpha = 0.38f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (readyState.saveOperation is OperationUiState.InProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = DarkBgPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.common_save),
                                style = SaveButtonStyle,
                                color = if (saveEnabled) DarkBgPrimary else DarkBgPrimary.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val configuration = LocalConfiguration.current
            val minPhotoHeight = (configuration.screenHeightDp * 0.4f).dp

            // Photo Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = minPhotoHeight)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                        RoundedCornerShape(18.dp)
                    )
                    .then(
                        if (image != null) {
                            Modifier.clickable { showFullscreenViewer = true }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = stringResource(R.string.scan_review_photo_cd),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                if (recognizingState != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            if (readyState != null) {
                // Error Messages
                if (readyState.ocrError != null || readyState.saveOperation is OperationUiState.Error) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        readyState.ocrError?.let { errorRes ->
                            CompactErrorRow(message = stringResource(errorRes))
                        }

                        if (readyState.saveOperation is OperationUiState.Error) {
                            CompactErrorRow(message = readyState.saveOperation.message)
                        }
                    }
                }

                // Values Card
                ValuesCard {
                    ReadingRow(
                        field = Field.SYS,
                        value = readyState.sys,
                        isValid = readyState.sysValid,
                        onClick = { startEdit(Field.SYS, readyState.sys) }
                    )

                    ReadingRow(
                        field = Field.DIA,
                        value = readyState.dia,
                        isValid = readyState.diaValid,
                        onClick = { startEdit(Field.DIA, readyState.dia) }
                    )

                    ReadingRow(
                        field = Field.PUL,
                        value = readyState.pulse,
                        isValid = readyState.pulseValid,
                        onClick = { startEdit(Field.PUL, readyState.pulse) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusedValueEditor(
    field: Field,
    draft: String,
    image: Bitmap?,
    onOpenViewer: () -> Unit,
    onSwipeUp: () -> Unit,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit
) {
    val swipeThresholdPx = with(LocalDensity.current) { SwipeUpThreshold.toPx() }
    val label = stringResource(field.labelRes)
    val unit = stringResource(R.string.dashboard_units_mmHg)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Photo and header; swipe up here discards the edit (keypad is excluded)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(swipeThresholdPx) {
                    var totalDrag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f },
                        onDragEnd = {
                            if (totalDrag <= -swipeThresholdPx) onSwipeUp()
                        },
                        onVerticalDrag = { _, dragAmount -> totalDrag += dragAmount }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                        RoundedCornerShape(18.dp)
                    )
                    .then(
                        if (image != null) {
                            Modifier.clickable(onClick = onOpenViewer)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = stringResource(R.string.scan_review_photo_cd),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Header: label (always visible) on the left, draft value on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = LabelTextStyle.copy(color = field.color)
                    )
                    if (field.hasUnit) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = unit,
                            style = UnitTextStyle.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        )
                    }
                    if (field.hasHeartIcon) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = field.color
                        )
                    }
                }

                if (draft.isEmpty()) {
                    Text(
                        text = "—",
                        style = valueTextStyle(
                            fontSize = field.fontSize,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
                        )
                    )
                } else {
                    Text(
                        text = draft,
                        style = valueTextStyle(
                            fontSize = field.fontSize,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        NumericKeypad(
            onDigit = onDigit,
            onBackspace = onBackspace,
            onConfirm = onConfirm,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

@Composable
private fun CompactErrorRow(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = ErrorTextStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FullscreenImageViewer(
    image: Bitmap,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.05f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                                offset = Offset.Zero
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        val maxPan = calculateMaxPan(containerSize, image.width, image.height, newScale)
                        scale = newScale
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxPan.x, maxPan.x),
                            y = (offset.y + pan.y).coerceIn(-maxPan.y, maxPan.y)
                        )
                    }
                }
        ) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.scan_review_close_viewer_cd),
                    tint = Color.White
                )
            }
        }
    }
}

private fun calculateMaxPan(
    containerSize: IntSize,
    imageWidth: Int,
    imageHeight: Int,
    scale: Float
): Offset {
    if (containerSize.width <= 0 || containerSize.height <= 0 || imageWidth <= 0 || imageHeight <= 0) {
        return Offset.Zero
    }
    val fitScale = minOf(
        containerSize.width.toFloat() / imageWidth.toFloat(),
        containerSize.height.toFloat() / imageHeight.toFloat()
    )
    val fittedWidth = imageWidth.toFloat() * fitScale
    val fittedHeight = imageHeight.toFloat() * fitScale

    val maxX = maxOf(0f, (fittedWidth * scale - containerSize.width.toFloat()) / 2f)
    val maxY = maxOf(0f, (fittedHeight * scale - containerSize.height.toFloat()) / 2f)
    return Offset(maxX, maxY)
}

@Composable
private fun ValuesCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                RoundedCornerShape(20.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
private fun ReadingRow(
    field: Field,
    value: String,
    isValid: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(field.labelRes)

    val valueColor = if (value.isNotEmpty() && !isValid) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val rowShape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left column (weight 1)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (field.hasHeartIcon) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = LabelTextStyle.copy(color = field.color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = field.color
                    )
                }
            } else {
                Text(
                    text = label,
                    style = LabelTextStyle.copy(color = field.color)
                )
            }
            if (field.hasUnit) {
                Text(
                    text = stringResource(R.string.dashboard_units_mmHg),
                    style = UnitTextStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                )
            }
        }

        // Value
        Text(
            text = if (value.isEmpty()) "—" else value,
            style = if (value.isEmpty()) {
                EmptyValueTextStyle.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
                )
            } else {
                valueTextStyle(fontSize = field.fontSize, color = valueColor)
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 40dp square button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.scan_review_edit_cd, label),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
            )
        }
    }
}
