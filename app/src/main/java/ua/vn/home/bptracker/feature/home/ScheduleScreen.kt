package ua.vn.home.bptracker.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.core.utils.TimeUtils
import ua.vn.home.bptracker.data.dto.DoseUnit
import ua.vn.home.bptracker.data.dto.WhenSlot
import ua.vn.home.bptracker.feature.reminders.TodayMed
import ua.vn.home.bptracker.feature.reminders.TodaySchedule
import ua.vn.home.bptracker.feature.reminders.TodaySlot
import ua.vn.home.bptracker.ui.components.BpCard
import ua.vn.home.bptracker.ui.components.EmptyState
import ua.vn.home.bptracker.ui.components.ListStateHost
import ua.vn.home.bptracker.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Explicit typography styles built on theme font families DmSans and DmMono
private val TopBarTitleStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp
)

private val TopBarDateStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp
)

private val ProgressTitleStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp
)

private val ProgressCounterTakenStyle = SpanStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp
)

private val ProgressCounterTotalStyle = SpanStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp
)

private val ProgressCounterTextStyle = SpanStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp
)

private val SlotHeaderPeriodStyle = SpanStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = 1.sp
)

private val SlotHeaderTimeStyle = SpanStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = 1.sp
)

private val SlotHeaderStatusStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp
)

private val MedNameStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp
)

private val MedConditionStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp
)

private val MedDoseStyle = TextStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp
)

private val FooterTakenLabelStyle = SpanStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp
)

private val FooterTakenTimeStyle = SpanStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp
)

private val FooterDeviationStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp
)

private val FooterNotTakenLabelStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp
)

private val FooterTakeActionStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp
)

private val SheetTitleStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp
)

private val SheetSubtitleStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp
)

private val SheetBodyStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp
)

private val ButtonTextStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp
)

/**
 * Calculates schedule deviation between recorded takenAt time and the scheduled slot.time.
 * |delta| <= 5 min -> onTimeLabel ("вчасно")
 * otherwise signed "+20 хв" / "−15 хв"
 * >= 60 min -> "+1 год 5 хв"
 */
fun calculateScheduleDeviation(
    scheduleDate: String,
    slotTime: String,
    takenAt: String?,
    onTimeLabel: String = "вчасно",
    minuteUnit: String = "хв",
    hourUnit: String = "год"
): String? {
    if (takenAt.isNullOrBlank() || scheduleDate.isBlank() || slotTime.isBlank()) return null
    return try {
        val parsedDate = LocalDate.parse(scheduleDate.trim())
        val parsedTime = try {
            LocalTime.parse(slotTime.trim())
        } catch (_: Exception) {
            val parts = slotTime.trim().split(":")
            LocalTime.of(parts[0].toInt(), if (parts.size > 1) parts[1].toInt() else 0)
        }
        val scheduledDateTime = LocalDateTime.of(parsedDate, parsedTime)
        val takenDateTime = TimeUtils.parseToLocal(takenAt).toLocalDateTime()
        val deltaMinutes = java.time.Duration.between(scheduledDateTime, takenDateTime).toMinutes()
        val absMinutes = kotlin.math.abs(deltaMinutes)

        if (absMinutes <= 5) {
            onTimeLabel
        } else {
            val sign = if (deltaMinutes > 0) "+" else "−"
            if (absMinutes < 60) {
                "$sign$absMinutes $minuteUnit"
            } else {
                val hours = absMinutes / 60
                val mins = absMinutes % 60
                if (mins > 0) {
                    "$sign$hours $hourUnit $mins $minuteUnit"
                } else {
                    "$sign$hours $hourUnit"
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}

fun formatScheduleDate(dateStr: String, locale: Locale = Locale.getDefault()): String {
    return try {
        val date = LocalDate.parse(dateStr.trim())
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
        val formatted = date.format(formatter)
        formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    } catch (_: Exception) {
        dateStr
    }
}

fun formatTakenAtTime(takenAtIso: String?): String {
    if (takenAtIso.isNullOrBlank()) return ""
    return try {
        val dt = TimeUtils.parseToLocal(takenAtIso)
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        takenAtIso.takeLast(5)
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
    state: ListUiState<TodaySchedule>,
    onConfirm: (WhenSlot) -> Unit,
    onEditTime: (WhenSlot, String) -> Unit,
    onDelete: (WhenSlot) -> Unit,
    onRefresh: () -> Unit,
    onEditClick: () -> Unit,
    onPrescriptionsClick: () -> Unit,
) {
    var selectedSlotForIntake by remember { mutableStateOf<TodaySlot?>(null) }
    val schedule = (state as? ListUiState.Content)?.data
    val scheduleDate = schedule?.date ?: LocalDate.now().toString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.schedule_title),
                        style = TopBarTitleStyle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = formatScheduleDate(scheduleDate),
                        style = TopBarDateStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = onPrescriptionsClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Assignment,
                        contentDescription = stringResource(R.string.prescriptions_title),
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.rem_config_title),
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            ListStateHost(
                state = state,
                onRetry = onRefresh
            ) { loadedSchedule, refreshing ->
                if (!loadedSchedule.configured) {
                    NotConfiguredState(onEditClick)
                } else {
                    Column(Modifier.fillMaxSize()) {
                        if (refreshing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        if (loadedSchedule.slots.isEmpty()) {
                            EmptyState(
                                title = stringResource(R.string.schedule_empty),
                                description = stringResource(R.string.schedule_empty_hint),
                            )
                        } else {
                            ScheduleContent(
                                schedule = loadedSchedule,
                                onSlotClick = { selectedSlotForIntake = it }
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
            Text(
                text = stringResource(R.string.schedule_not_configured),
                style = SheetBodyStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onEditClick) {
                Text(
                    text = stringResource(R.string.schedule_set_hours),
                    style = ButtonTextStyle
                )
            }
        }
    }
}

@Composable
fun ScheduleContent(
    schedule: TodaySchedule,
    onSlotClick: (TodaySlot) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.screenPadding,
            top = MaterialTheme.spacing.screenPadding,
            end = MaterialTheme.spacing.screenPadding,
            bottom = MaterialTheme.spacing.large
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ProgressCard(schedule = schedule)
        }
        items(schedule.slots) { slot ->
            Column {
                SlotHeader(slot = slot)
                Spacer(modifier = Modifier.height(8.dp))
                SlotCard(
                    slot = slot,
                    scheduleDate = schedule.date,
                    onClick = { onSlotClick(slot) }
                )
            }
        }
        item {
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
fun ProgressCard(schedule: TodaySchedule, modifier: Modifier = Modifier) {
    val takenCount = schedule.slots.count { it.taken }
    val totalCount = schedule.slots.size
    val progress = if (totalCount > 0) takenCount.toFloat() / totalCount.toFloat() else 0f
    val isDark = isSystemInDarkTheme()
    val trackColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    BpCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.schedule_today),
                    style = ProgressTitleStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val restText = pluralStringResource(R.plurals.schedule_intakes_count, totalCount, totalCount)
                val totalStr = totalCount.toString()
                val totalIndex = restText.indexOf(totalStr)
                val counterText = buildAnnotatedString {
                    withStyle(ProgressCounterTakenStyle.copy(color = ColorSuccess)) {
                        append(takenCount.toString())
                    }
                    append(" ")
                    if (totalIndex != -1) {
                        withStyle(ProgressCounterTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(restText.substring(0, totalIndex))
                        }
                        withStyle(ProgressCounterTotalStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(totalStr)
                        }
                        withStyle(ProgressCounterTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(restText.substring(totalIndex + totalStr.length))
                        }
                    } else {
                        withStyle(ProgressCounterTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(restText)
                        }
                    }
                }

                Text(text = counterText)
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = ColorSuccess,
                trackColor = trackColor,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
fun SlotHeader(slot: TodaySlot, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val period = getLocalizedPeriod(slot.slot).uppercase()
        val headerText = buildAnnotatedString {
            withStyle(SlotHeaderPeriodStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                append("$period · ")
            }
            withStyle(SlotHeaderTimeStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                append(slot.time)
            }
        }
        Text(text = headerText)

        if (slot.taken) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ColorSuccess,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.schedule_status_completed),
                    style = SlotHeaderStatusStyle,
                    color = ColorSuccess
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ColorWarning)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.schedule_status_pending),
                    style = SlotHeaderStatusStyle,
                    color = ColorWarning
                )
            }
        }
    }
}

@Composable
fun SlotCard(
    slot: TodaySlot,
    scheduleDate: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)

    BpCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            slot.meds.forEachIndexed { index, med ->
                MedRow(med = med)
                if (index < slot.meds.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = dividerColor)
                }
            }
            HorizontalDivider(thickness = 1.dp, color = dividerColor)
            SlotFooterRow(
                slot = slot,
                scheduleDate = scheduleDate,
                onClick = onClick
            )
        }
    }
}

@Composable
fun MedRow(med: TodayMed, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Medication,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = med.medicine,
                style = MedNameStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!med.condition.isNullOrBlank()) {
                Text(
                    text = med.condition,
                    style = MedConditionStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val unit = getLocalizedDoseUnit(med.doseUnit)
        val doseText = if (unit.isNotEmpty()) "${med.doseAmount} $unit" else med.doseAmount
        if (doseText.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = doseText,
                style = MedDoseStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SlotFooterRow(
    slot: TodaySlot,
    scheduleDate: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (slot.taken) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(ColorSuccess.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ColorSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val timeStr = formatTakenAtTime(slot.takenAt)
                val label = stringResource(R.string.schedule_taken_at_prefix)
                val footerText = buildAnnotatedString {
                    withStyle(FooterTakenLabelStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(label)
                        if (!label.endsWith(" ")) append(" ")
                    }
                    withStyle(FooterTakenTimeStyle.copy(color = ColorSuccess)) {
                        append(timeStr)
                    }
                }
                Text(text = footerText)
            }

            val deviation = calculateScheduleDeviation(
                scheduleDate = scheduleDate,
                slotTime = slot.time,
                takenAt = slot.takenAt,
                onTimeLabel = stringResource(R.string.schedule_deviation_on_time),
                minuteUnit = stringResource(R.string.schedule_deviation_minute_short),
                hourUnit = stringResource(R.string.schedule_deviation_hour_short)
            )
            if (!deviation.isNullOrBlank()) {
                Text(
                    text = deviation,
                    style = FooterDeviationStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.schedule_not_taken),
                style = FooterNotTakenLabelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.schedule_action_take),
                style = FooterTakeActionStyle,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onClick)
            )
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
                style = SheetTitleStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${getLocalizedPeriod(slot.slot)} · ${slot.time}",
                style = SheetSubtitleStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            slot.meds.forEach { med ->
                val unit = getLocalizedDoseUnit(med.doseUnit)
                val dose = if (unit.isNotEmpty()) "${med.doseAmount} $unit" else med.doseAmount
                Text(
                    text = "• ${med.medicine} ($dose)",
                    style = SheetBodyStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(32.dp))

            if (!slot.taken) {
                Button(
                    onClick = { onConfirm(slot.slot) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.schedule_action_confirm),
                        style = ButtonTextStyle
                    )
                }
            } else {
                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.schedule_action_edit),
                        style = ButtonTextStyle
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onDelete(slot.slot) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.schedule_action_delete),
                        style = ButtonTextStyle
                    )
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
                    Text(
                        text = stringResource(R.string.common_save),
                        style = ButtonTextStyle
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        style = ButtonTextStyle
                    )
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }
}
