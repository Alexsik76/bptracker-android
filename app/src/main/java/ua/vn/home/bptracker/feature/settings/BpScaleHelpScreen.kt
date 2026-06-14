package ua.vn.home.bptracker.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.bp.BpZone
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.ui.components.BpCard
import ua.vn.home.bptracker.ui.components.PositionScaleBar
import ua.vn.home.bptracker.ui.components.ReadingValue
import ua.vn.home.bptracker.ui.components.ZoneBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BpScaleHelpScreen(
    latestMeasurement: MeasurementDto?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_bp_scale_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Intro
            Text(
                text = stringResource(R.string.bp_scale_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Scale Table
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BpZone.entries.forEach { zone ->
                    ZoneInfoRow(zone)
                }
            }

            // Latest Measurement
            if (latestMeasurement != null) {
                val zone = BpZone.classify(latestMeasurement.sys, latestMeasurement.dia)
                Text(
                    text = stringResource(R.string.bp_scale_your_latest).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BpCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ReadingValue(sys = latestMeasurement.sys, dia = latestMeasurement.dia, zone = zone, fontSize = 22.sp)
                            ZoneBadge(zone)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        PositionScaleBar(latestMeasurement.sys, latestMeasurement.dia)
                    }
                }
            }

            // Disclaimer
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(R.string.bp_scale_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ZoneInfoRow(zone: BpZone) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(zone.color(isDark))
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(zone.labelRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = getZoneDescription(zone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = getZoneRange(zone),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = MaterialTheme.typography.headlineMedium.fontFamily,
                color = zone.color(isDark)
            )
        }
    }
}

@Composable
fun getZoneDescription(zone: BpZone): String {
    return when (zone) {
        BpZone.OPTIMAL -> stringResource(R.string.bp_scale_optimal_desc)
        BpZone.NORMAL -> stringResource(R.string.bp_scale_normal_desc)
        BpZone.STAGE1 -> stringResource(R.string.bp_scale_stage1_desc)
        BpZone.STAGE2 -> stringResource(R.string.bp_scale_stage2_desc)
    }
}

fun getZoneRange(zone: BpZone): String {
    return when (zone) {
        BpZone.OPTIMAL -> "SYS < 120  &  DIA < 80"
        BpZone.NORMAL -> "SYS 120-139  /  DIA 80-89"
        BpZone.STAGE1 -> "SYS 140-159  /  DIA 90-99"
        BpZone.STAGE2 -> "SYS ≥ 160  |  DIA ≥ 100"
    }
}
