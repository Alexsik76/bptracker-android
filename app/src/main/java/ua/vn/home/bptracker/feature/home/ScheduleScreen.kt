package ua.vn.home.bptracker.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.utils.TimeUtils
import ua.vn.home.bptracker.data.dto.DoseUnit
import ua.vn.home.bptracker.data.dto.WhenSlot
import ua.vn.home.bptracker.feature.reminders.TodaySchedule
import ua.vn.home.bptracker.feature.reminders.TodaySlot
import ua.vn.home.bptracker.ui.components.*
import ua.vn.home.bptracker.ui.theme.ColorSuccess
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun getLocalizedTime(timeStr: String?): String {
    if (timeStr == null) return ""
    return try {
        val dt = TimeUtils.parseToLocal(timeStr)
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        timeStr.takeLast(5)
    }
}

@Composable
fun getLocalizedPeriod(slot: WhenSlot): String {
    return when (slot) {
        WhenSlot.Morning -> stringResource(R.string.period_morning)
        WhenSlot.Day -> stringResource(R.string.period_day)
        WhenSlot.Evening -> stringResource(R.string.period_evening)
    }
}

@Composable
fun getLocalizedDoseUnit(unit: DoseUnit?): String {
    return when (unit) {
        DoseUnit.Tablet -> stringResource(R.string.med_enum_unit_tablet)
        DoseUnit.Mg -> stringResource(R.string.med_enum_unit_mg)
        DoseUnit.Ml -> stringResource(R.string.med_enum_unit_ml)
        DoseUnit.Drop -> stringResource(R.string.med_enum_unit_drop)
        DoseUnit.Mcg -> stringResource(R.string.med_enum_unit_mcg)
        DoseUnit.Iu -> stringResource(R.string.med_enum_unit_iu)
        null -> ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    state: ScheduleState,
    onConfirm: (WhenSlot) -> Unit,
    onEditTime: (WhenSlot, String) -> Unit,
    onDelete: (WhenSlot) -> Unit,
    onRefresh: () -> Unit,
    onEditClick: () -> Unit,
    onPrescriptionsClick: () -> Unit,
) {
    var selectedSlotForIntake by remember { mutableStateOf<TodaySlot?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.schedule_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = onPrescriptionsClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = stringResource(R.string.prescriptions_title),
                        )
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rem_config_title))
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
                is ScheduleState.NotConfigured -> NotConfiguredState(onEditClick)
                is ScheduleState.Error -> ErrorState(
                    message = state.message,
                    onRetry = onRefresh
                )
                is ScheduleState.Content -> {
                    Column(Modifier.fillMaxSize()) {
                        if (state.isRefreshing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        
                        if (state.schedule.slots.isEmpty()) {
                            EmptyState(
                                title = stringResource(R.string.schedule_empty),
                                description = stringResource(R.string.schedule_empty_hint),
                            )
                        } else {
                            ScheduleContent(
                                schedule = state.schedule,
                                onSlotClick = { selectedSlotForIntake = it },
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedSlotForIntake != null) {
        IntakeBottomSheet(
            slot = selectedSlotForIntake!!,
            onConfirm = { 
                onConfirm(it)
                selectedSlotForIntake = null 
            },
            onEditTime = { slot, time -> 
                onEditTime(slot, time)
                selectedSlotForIntake = null
            },
            onDelete = { 
                onDelete(it)
                selectedSlotForIntake = null
            },
            onDismiss = { selectedSlotForIntake = null }
        )
    }
}

@Composable
fun NotConfiguredState(onEditClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.schedule_not_configured))
            Spacer(Modifier.height(8.dp))
            Button(onClick = onEditClick) {
                Text(stringResource(R.string.schedule_set_hours))
            }
        }
    }
}

@Composable
fun ScheduleContent(
    schedule: TodaySchedule,
    onSlotClick: (TodaySlot) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(schedule.slots) { slot ->
            SlotCard(slot, onClick = { onSlotClick(slot) })
        }
        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun SlotCard(slot: TodaySlot, onClick: () -> Unit) {
    BpCard(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
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
                            text = "${getLocalizedPeriod(slot.slot)} · ${slot.time}",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    
                    if (slot.taken) {
                        Surface(
                            color = ColorSuccess.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ColorSuccess,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = getLocalizedTime(slot.takenAt),
                                    color = ColorSuccess,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.schedule_action_confirm).uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                slot.meds.forEach { med ->
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            Text("• ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            val unit = getLocalizedDoseUnit(med.doseUnit)
                            val dose = if (unit.isNotEmpty()) "${med.doseAmount} $unit" else med.doseAmount
                            Text(
                                text = "${med.medicine} ($dose)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        med.condition?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeBottomSheet(
    slot: TodaySlot,
    onConfirm: (WhenSlot) -> Unit,
    onEditTime: (WhenSlot, String) -> Unit,
    onDelete: (WhenSlot) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var showTimePicker by remember { mutableStateOf(value = false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.schedule_intake_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${getLocalizedPeriod(slot.slot)} · ${slot.time}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            slot.meds.forEach { med ->
                val unit = getLocalizedDoseUnit(med.doseUnit)
                val dose = if (unit.isNotEmpty()) "${med.doseAmount} $unit" else med.doseAmount
                Text(
                    text = "• ${med.medicine} ($dose)",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(Modifier.height(32.dp))

            if (!slot.taken) {
                Button(
                    onClick = { onConfirm(slot.slot) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.schedule_action_confirm))
                }
            } else {
                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.schedule_action_edit))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onDelete(slot.slot) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.schedule_action_delete))
                }
            }
        }
    }

    if (showTimePicker) {
        val now = OffsetDateTime.now()
        val timePickerState = rememberTimePickerState(
            initialHour = now.hour,
            initialMinute = now.minute
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val edited = now.withHour(timePickerState.hour)
                            .withMinute(timePickerState.minute)
                            .withSecond(0).withNano(0)
                        onEditTime(slot.slot, edited.toString())
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }
}
