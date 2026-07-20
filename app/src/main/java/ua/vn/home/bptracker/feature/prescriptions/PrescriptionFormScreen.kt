package ua.vn.home.bptracker.feature.prescriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
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
import ua.vn.home.bptracker.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionFormScreen(
    state: PrescriptionFormState,
    onDoctorChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onIsActiveChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(state.prescribedOn).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        onDateChange(date.toString())
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (state.id == null) stringResource(R.string.prescriptions_add_btn) else state.doctor) 
                },
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
                .padding(MaterialTheme.spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            OutlinedTextField(
                value = state.doctor,
                onValueChange = onDoctorChange,
                label = { Text(stringResource(R.string.prescriptions_doctor_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = state.prescribedOn,
                onValueChange = {},
                label = { Text(stringResource(R.string.prescriptions_date_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                readOnly = true,
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.prescriptions_pick_date_desc)
                        )
                    }
                }
            )

            if (state.id != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.prescriptions_active))
                    Switch(checked = state.isActive, onCheckedChange = onIsActiveChange)
                }
            }

            if (state.saveOperation is OperationUiState.Error) {
                Text(state.saveOperation.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(MaterialTheme.spacing.large))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state.isValid && state.saveOperation !is OperationUiState.InProgress,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.saveOperation is OperationUiState.InProgress) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(stringResource(R.string.common_save), fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
            
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}
