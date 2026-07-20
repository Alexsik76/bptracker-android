package ua.vn.home.bptracker.feature.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.ui.OperationUiState
import ua.vn.home.bptracker.ui.theme.DarkPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderConfigScreen(
    state: ReminderConfigState,
    onTimeChange: (String, Int, Int) -> Unit,
    onMaxRemindersChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(state.saveOperation) {
        if (state.saveOperation is OperationUiState.Success) onBack()
    }

    var activePickerSlot by remember { mutableStateOf<String?>(null) }

    if (activePickerSlot != null) {
        val (initialHour, initialMinute) = when (activePickerSlot) {
            "morning" -> state.morningHour to state.morningMinute
            "day" -> state.dayHour to state.dayMinute
            else -> state.eveningHour to state.eveningMinute
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { activePickerSlot = null },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(activePickerSlot!!, timePickerState.hour, timePickerState.minute)
                    activePickerSlot = null
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { activePickerSlot = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rem_config_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TimeField(
                label = stringResource(R.string.rem_config_morning),
                value = state.morningTime.take(5),
                onClick = { activePickerSlot = "morning" }
            )

            TimeField(
                label = stringResource(R.string.rem_config_day),
                value = state.dayTime.take(5),
                onClick = { activePickerSlot = "day" }
            )

            TimeField(
                label = stringResource(R.string.rem_config_evening),
                value = state.eveningTime.take(5),
                onClick = { activePickerSlot = "evening" }
            )

            OutlinedTextField(
                value = state.maxReminders,
                onValueChange = onMaxRemindersChange,
                label = { Text(stringResource(R.string.rem_config_max)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.durationMinutes,
                onValueChange = onDurationChange,
                label = { Text(stringResource(R.string.rem_config_duration)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (state.saveOperation is OperationUiState.Error) {
                Text(
                    text = state.saveOperation.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state.isValid && state.saveOperation !is OperationUiState.InProgress,
                colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.saveOperation is OperationUiState.InProgress) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(stringResource(R.string.common_save), fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TimeField(label: String, value: String, onClick: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.schedule_confirm_btn))
            }
        }
    )
}
