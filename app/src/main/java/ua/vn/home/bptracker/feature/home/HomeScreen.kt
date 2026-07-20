package ua.vn.home.bptracker.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.bp.BpZone
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.ui.components.*
import ua.vn.home.bptracker.ui.theme.*
import ua.vn.home.bptracker.core.utils.TimeUtils
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    state: ListUiState<HomePayload>,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMeasurementClick: (MeasurementDto) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        @OptIn(ExperimentalMaterial3Api::class)
        TopAppBar(
            title = {
                Text(
                    text = "∿ " + stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            ),
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            ListStateHost(
                state = state,
                onRetry = onRefresh,
                onEmpty = {
                    EmptyState(
                        title = stringResource(R.string.dashboard_no_measurements),
                        description = stringResource(R.string.dashboard_empty_hint)
                    )
                }
            ) { content, _ ->
                DashboardContent(content, onHistoryClick, onMeasurementClick)
            }
        }
    }
}

@Composable
fun DashboardContent(
    content: HomePayload,
    onHistoryClick: () -> Unit,
    onMeasurementClick: (MeasurementDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            HeroCard(
                latest = content.latest,
                zone = content.zone,
                onClick = { onMeasurementClick(content.latest) },
            )
        }
        
        item {
            KpiGrid(content)
        }
        
        item {
            RecentReadingsSection(content.recent, onHistoryClick)
        }
        
        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun RecentReadingsSection(
    recent: List<MeasurementDto>,
    onHistoryClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "press")
    val borderColor by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.07f),
        label = "border"
    )

    BpCard(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                role = Role.Button,
                onClickLabel = stringResource(R.string.dashboard_show_all),
                onClick = onHistoryClick
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = stringResource(R.string.dashboard_recent_readings).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                )

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        val now = OffsetDateTime.now()
                        val filtered = recent
                            .filter { TimeUtils.parseToLocal(it.recordedAt).isAfter(now.minusHours(24)) }
                            .take(4)

                        if (filtered.isEmpty()) {
                            Text(
                                text = "No readings in the last 24h",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp).align(Alignment.CenterHorizontally),
                            )
                        } else {
                            filtered.forEachIndexed { index, m ->
                                MeasurementRow(m, endPadding = if (index == 0) 36.dp else 0.dp)
                                if (index < (filtered.size - 1)) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Chevron Indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFAEB8BE),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun MeasurementRow(m: MeasurementDto, endPadding: androidx.compose.ui.unit.Dp = 0.dp, onClick: (() -> Unit)? = null) {
    val zone = BpZone.classify(m.sys, m.dia)
    val dt = TimeUtils.parseToLocal(m.recordedAt)
    val now = OffsetDateTime.now()
    val isDark = isSystemInDarkTheme()
    
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val isToday = dt.toLocalDate().isEqual(now.toLocalDate())
    val isYesterday = dt.toLocalDate().isEqual(now.toLocalDate().minusDays(1))
    
    val dateLabel = when {
        isToday -> stringResource(R.string.common_today)
        isYesterday -> stringResource(R.string.common_yesterday)
        else -> dt.format(DateTimeFormatter.ofPattern("dd.MM"))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(start = 20.dp, top = 12.dp, bottom = 12.dp, end = 20.dp + endPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            color = zone.color(isDark),
            shape = RoundedCornerShape(50)
        ) {}
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = "${m.sys}/${m.dia}",
            style = MaterialTheme.typography.titleSmall,
            fontFamily = MaterialTheme.typography.headlineMedium.fontFamily,
            modifier = Modifier.weight(1f)
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "♡",
                color = ColorPulse,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = m.pulse.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = MaterialTheme.typography.headlineMedium.fontFamily
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = "$dateLabel ${dt.format(timeFormatter)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun KpiGrid(content: HomePayload) {
    val isDark = isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val avgZone = BpZone.classify(content.avgSys, content.avgDia)
            KpiTile(
                label = stringResource(R.string.kpi_avg_7d),
                value = "${content.avgSys}/${content.avgDia}",
                sub = stringResource(avgZone.labelRes),
                valueColor = avgZone.color(isDark),
                modifier = Modifier.weight(1f)
            )
            
            val changeSign = if (content.sysChange >= 0) "+" else ""
            val changeColor = if (content.sysChange > 0 || content.diaChange > 0) 
                ColorDanger else ColorSuccess
            
            KpiTile(
                label = stringResource(R.string.kpi_week_change),
                value = "$changeSign${content.sysChange}/${content.diaChange}",
                sub = if (content.sysChange == 0 && content.diaChange == 0) "—" else "vs last week",
                valueColor = changeColor,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiTile(
                label = stringResource(R.string.kpi_in_range),
                value = "${content.inRangePercent}%",
                sub = "Target: <140/90",
                modifier = Modifier.weight(1f)
            )
            KpiTile(
                label = stringResource(R.string.kpi_avg_pulse),
                value = content.avgPulse.toString(),
                sub = stringResource(R.string.dashboard_units_bpm),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HeroCard(latest: MeasurementDto, zone: BpZone, onClick: () -> Unit) {
    val dt = TimeUtils.parseToLocal(latest.recordedAt)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "press")
    val borderColor by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.07f),
        label = "border"
    )

    BpCard(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                role = Role.Button,
                onClickLabel = stringResource(R.string.measurement_detail_title),
                onClick = onClick
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_last_reading, dt.format(timeFormatter)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ZoneBadge(zone)
                }

                Spacer(modifier = Modifier.height(16.dp))

                ReadingValue(
                    sys = latest.sys,
                    dia = latest.dia,
                    zone = zone
                )

                Text(
                    text = buildString {
                        append(stringResource(R.string.dashboard_units_mmHg))
                        append(" · ♡")
                        append(latest.pulse)
                        append(" ")
                        append(stringResource(R.string.dashboard_units_bpm))
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = MaterialTheme.typography.headlineMedium.fontFamily
                )

                Spacer(modifier = Modifier.height(24.dp))

                PositionScaleBar(latest.sys, latest.dia)
            }

            // Chevron Indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(28.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFAEB8BE),
                    modifier = Modifier.padding(5.dp)
                )
            }
        }
    }
}
