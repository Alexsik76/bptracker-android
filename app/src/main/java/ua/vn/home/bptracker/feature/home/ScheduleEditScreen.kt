package ua.vn.home.bptracker.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.ui.components.BpCard
import ua.vn.home.bptracker.ui.components.ErrorState
import ua.vn.home.bptracker.ui.components.LoadingState
import ua.vn.home.bptracker.ui.components.ValueField
import ua.vn.home.bptracker.ui.theme.DarkPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditScreen(
    state: ScheduleEditState,
    onTimeChange: (String, String) -> Unit,
    onDurationChange: (String) -> Unit,
    onMaxRemindersChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val readyState = state as? ScheduleEditState.Ready

    LaunchedEffect(readyState?.saved) {
        if (readyState?.saved == true) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.schedule_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (readyState != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = { 
                            android.util.Log.d("ScheduleEdit", "Save Button Clicked")
                            onSave() 
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
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
                            Text(stringResource(R.string.common_save), fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (state) {
                is ScheduleEditState.Loading -> LoadingState()
                is ScheduleEditState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.schedule_edit_empty), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                is ScheduleEditState.Error -> ErrorState(message = state.message, onRetry = onRetry)
                is ScheduleEditState.Ready -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Periods
                        Text(
                            text = stringResource(R.string.schedule_edit_periods).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        state.periods.forEach { (name, config) ->
                            PeriodEditRow(
                                name = name,
                                config = config,
                                onTimeChange = { newTime -> onTimeChange(name, newTime) }
                            )
                        }

                        // General Settings
                        Text(
                            text = stringResource(R.string.schedule_edit_general).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ValueField(
                            label = stringResource(R.string.schedule_edit_duration),
                            secondary = stringResource(R.string.schedule_edit_minutes),
                            dotColor = MaterialTheme.colorScheme.primary,
                            value = state.durationMinutes,
                            unit = "min",
                            onValueChange = onDurationChange,
                            isValid = state.durationMinutes.toIntOrNull()?.let { it > 0 } == true
                        )

                        ValueField(
                            label = stringResource(R.string.schedule_edit_max_reminders),
                            secondary = stringResource(R.string.schedule_edit_per_period),
                            dotColor = MaterialTheme.colorScheme.secondary,
                            value = state.maxReminders,
                            unit = "qty",
                            onValueChange = onMaxRemindersChange,
                            isValid = state.maxReminders.toIntOrNull()?.let { it > 0 } == true
                        )
                        
                        if (state.error != null) {
                            Text(
                                text = state.error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodEditRow(
    name: String,
    config: ua.vn.home.bptracker.data.dto.PeriodConfig,
    onTimeChange: (String) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timeParts = (config.time ?: "08:00").split(":")
    val timeState = rememberTimePickerState(
        initialHour = timeParts[0].toInt(),
        initialMinute = timeParts[1].toInt()
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val h = if (timeState.hour < 10) "0${timeState.hour}" else "${timeState.hour}"
                    val m = if (timeState.minute < 10) "0${timeState.minute}" else "${timeState.minute}"
                    onTimeChange("$h:$m")
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            TimePicker(state = timeState)
        }
    }

    BpCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getLocalizedPeriod(name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = (config.meds ?: emptyList()).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                onClick = { showTimePicker = true },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = config.time ?: "08:00",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = MaterialTheme.typography.headlineMedium.fontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = { content() }
    )
}
