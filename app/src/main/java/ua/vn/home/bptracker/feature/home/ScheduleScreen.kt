package ua.vn.home.bptracker.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.data.dto.TodayIntake
import ua.vn.home.bptracker.ui.components.*
import ua.vn.home.bptracker.ui.theme.ColorPulse
import ua.vn.home.bptracker.ui.theme.ColorSuccess
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun getLocalizedPeriod(period: String): String {
    return when (period.lowercase()) {
        "morning" -> stringResource(R.string.period_morning)
        "day" -> stringResource(R.string.period_day)
        "evening" -> stringResource(R.string.period_evening)
        else -> period
    }
}

@Composable
fun ScheduleScreen(
    state: ScheduleState,
    onConfirm: (String) -> Unit,
    onRefresh: () -> Unit,
    onEditClick: () -> Unit
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.schedule_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                is ScheduleState.Loading -> LoadingState()
                is ScheduleState.Empty -> EmptyState(
                    title = stringResource(R.string.schedule_empty),
                    description = stringResource(R.string.schedule_empty_hint)
                )
                is ScheduleState.Error -> ErrorState(
                    message = state.message,
                    onRetry = onRefresh
                )
                is ScheduleState.Content -> ScheduleContent(state.intakes, onConfirm)
            }
        }
    }
}

@Composable
fun ScheduleContent(intakes: List<TodayIntake>, onConfirm: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(intakes) { intake ->
            IntakeCard(intake, onConfirm)
        }
        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun IntakeCard(intake: TodayIntake, onConfirm: (String) -> Unit) {
    val isConfirmed = intake.status == "Confirmed"
    val isPending = intake.status == null

    BpCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${getLocalizedPeriod(intake.period)} · ${intake.time}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                StatusBadge(intake)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (intake.meds.isEmpty()) {
                Text(
                    text = "• Немає вказаних ліків",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                intake.meds.forEach { med ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = med,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isPending) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onConfirm(intake.period) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorSuccess,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.schedule_confirm_btn))
                }
            } else if (isConfirmed && intake.timeTaken != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val dt = OffsetDateTime.parse(intake.timeTaken)
                val timeStr = dt.format(DateTimeFormatter.ofPattern("HH:mm"))
                Text(
                    text = stringResource(R.string.schedule_taken_at, timeStr),
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorSuccess
                )
            }
        }
    }
}

@Composable
fun StatusBadge(intake: TodayIntake) {
    val (text, color) = when (intake.status) {
        "Confirmed" -> stringResource(R.string.schedule_taken) to ColorSuccess
        "Missed" -> stringResource(R.string.schedule_missed) to MaterialTheme.colorScheme.error
        else -> stringResource(R.string.schedule_pending) to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
