package ua.vn.home.bptracker.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.ui.theme.*
import ua.vn.home.bptracker.ui.components.*

@Composable
fun ScanReviewScreen(
    state: ScanReviewState?,
    onSysChange: (String) -> Unit,
    onDiaChange: (String) -> Unit,
    onPulseChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetake: () -> Unit,
    onBack: () -> Unit
) {
    val readyState = state as? ScanReviewState.Ready
    val recognizingState = state as? ScanReviewState.Recognizing

    LaunchedEffect(readyState?.saved) {
        if (readyState?.saved == true) onBack()
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.local_ocr_result_title),
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(8.dp).size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val image = readyState?.image ?: recognizingState?.image
            
            // Photo Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
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
                } else {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Збільшити", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (readyState != null) {
                // Entry Fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ValueField(
                        label = "Систолічний",
                        secondary = "верхній",
                        dotColor = ColorSys,
                        value = readyState.sys,
                        unit = stringResource(R.string.dashboard_units_mmHg),
                        onValueChange = onSysChange,
                        isValid = readyState.sysValid
                    )
                    ValueField(
                        label = "Діастолічний",
                        secondary = "нижній",
                        dotColor = ColorDia,
                        value = readyState.dia,
                        unit = stringResource(R.string.dashboard_units_mmHg),
                        onValueChange = onDiaChange,
                        isValid = readyState.diaValid
                    )
                    ValueField(
                        label = "Пульс",
                        secondary = "",
                        dotColor = ColorPulse,
                        value = readyState.pulse,
                        unit = stringResource(R.string.dashboard_units_bpm),
                        onValueChange = onPulseChange,
                        isValid = readyState.pulseValid
                    )
                }

                Spacer(Modifier.weight(1f))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Перезняти", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f).height(56.dp),
                        enabled = readyState.isValid && !readyState.saving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPrimary,
                            disabledContainerColor = DarkPrimary.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (readyState.saving) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Зберегти", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
                
                Text(
                    text = "Розпізнати через сервер",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Fallback */ }
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline)
                )
            }
        }
    }
}
