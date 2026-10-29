package ua.vn.home.bptracker.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import ua.vn.home.bptracker.ui.theme.ColorDanger
import ua.vn.home.bptracker.ui.theme.ColorPulse
import ua.vn.home.bptracker.ui.theme.ColorSuccess
import ua.vn.home.bptracker.ui.theme.DarkBgPrimary
import ua.vn.home.bptracker.ui.theme.DmMono
import ua.vn.home.bptracker.ui.theme.DmSans
import ua.vn.home.bptracker.ui.theme.ZoneStage1DarkText
import ua.vn.home.bptracker.ui.theme.spacing
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Private constant in screen file
private val ColorMissedRed = Color(0xFFF87171)

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

private val AllDoneTextStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp
)

private val ActiveSlotNameStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = 1.sp
)

private val ActiveSlotTimeStyle = TextStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.Medium,
    fontSize = 40.sp,
    letterSpacing = (-1).sp
)

private val ActiveChipTextStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.5.sp
)

private val ActiveChipMonoSpanStyle = SpanStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.5.sp
)

private val ActiveMedNameStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.5.sp
)

private val ActiveMedDoseStyle = TextStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.Normal,
    fontSize = 11.5.sp
)

private val ActiveButtonTextStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp
)

private val CompactSlotNameStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp
)

private val CompactStatusTextStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.5.sp
)

private val CompactStatusMonoSpanStyle = SpanStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.5.sp
)

private val CompactMedsTextStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp
)

private val CompactMedsDoseMonoSpanStyle = SpanStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp
)

private val CompactDeviationStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp
)

private val CompactDeviationMonoSpanStyle = SpanStyle(
    fontFamily = DmMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp
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
 * |delta| <= 15 min -> onTimeLabel ("вчасно")
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

        if (absMinutes <= 15) {
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

private fun formatWithMonoDigits(text: String, spanStyle: SpanStyle): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        text.forEachIndexed { index, char ->
            if (char.isDigit()) {
                addStyle(spanStyle, index, index + 1)
            }
        }
    }
}

@Composable
private fun formatActiveChipText(state: ActiveChipState): AnnotatedString {
    return when (state) {
        is ActiveChipState.DueNow -> {
            buildAnnotatedString {
                append(stringResource(R.string.schedule_chip_due_now))
            }
        }
        is ActiveChipState.BeforeMinutes -> {
            val template = stringResource(R.string.schedule_chip_before_minutes, state.minutes)
            formatWithMonoDigits(template, ActiveChipMonoSpanStyle)
        }
        is ActiveChipState.BeforeHours -> {
            val template = stringResource(R.string.schedule_chip_before_hours, state.hours)
            formatWithMonoDigits(template, ActiveChipMonoSpanStyle)
        }
        is ActiveChipState.LateMinutes -> {
            val template = stringResource(R.string.schedule_chip_late_minutes, state.minutes)
            formatWithMonoDigits(template, ActiveChipMonoSpanStyle)
        }
        is ActiveChipState.LateHoursMinutes -> {
            val template = if (state.minutes == 0L) {
                stringResource(R.string.schedule_chip_late_hours, state.hours)
            } else {
                stringResource(
                    R.string.schedule_chip_late_hours_minutes,
                    state.hours,
                    state.minutes
                )
            }
            formatWithMonoDigits(template, ActiveChipMonoSpanStyle)
        }
    }
}

private fun formatCompactMeds(meds: List<TodayMed>): AnnotatedString {
    return buildAnnotatedString {
        meds.forEachIndexed { index, med ->
            if (index > 0) {
                append(" · ")
            }
            append(med.medicine)
            if (med.doseAmount.isNotBlank()) {
                append(" ")
                val start = length
                append(med.doseAmount)
                addStyle(CompactMedsDoseMonoSpanStyle, start, length)
            }
        }
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

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = LocalTime.now()
        }
    }

    val statuses = remember(schedule?.slots, now) {
        computeSlotStatuses(schedule?.slots ?: emptyList(), now)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbarConfirmed = stringResource(R.string.schedule_snackbar_confirmed)
    val snackbarUndo = stringResource(R.string.schedule_snackbar_undo)

    val handleActiveConfirm: (WhenSlot) -> Unit = { slotWhen ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onConfirm(slotWhen)
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = snackbarConfirmed,
                actionLabel = snackbarUndo,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onDelete(slotWhen)
            }
        }
    }

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
                                statuses = statuses,
                                now = now,
                                onSlotClick = { selectedSlotForIntake = it },
                                onConfirmSlot = handleActiveConfirm
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 96.dp)
            )
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
    statuses: List<SlotStatus>,
    now: LocalTime,
    onSlotClick: (TodaySlot) -> Unit,
    onConfirmSlot: (WhenSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val allDone = schedule.slots.isNotEmpty() && statuses.all { it == SlotStatus.DONE }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.screenPadding,
            top = MaterialTheme.spacing.screenPadding,
            end = MaterialTheme.spacing.screenPadding,
            bottom = MaterialTheme.spacing.large
        )
    ) {
        item(key = "progress_header") {
            Column {
                ProgressCard(schedule = schedule)
                Spacer(modifier = Modifier.height(20.dp))
                if (allDone) {
                    Text(
                        text = stringResource(R.string.schedule_all_done),
                        style = AllDoneTextStyle,
                        color = ColorPulse
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        itemsIndexed(
            items = schedule.slots,
            key = { _, slot -> slot.slot.name }
        ) { index, slot ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(12.dp))
            }
            val status = statuses.getOrElse(index) { SlotStatus.UPCOMING }
            AnimatedContent(
                targetState = status,
                label = "SlotTransition_${slot.slot.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) { targetStatus ->
                if (targetStatus == SlotStatus.ACTIVE) {
                    ActiveSlotCard(
                        slot = slot,
                        now = now,
                        onConfirm = { onConfirmSlot(slot.slot) }
                    )
                } else {
                    CompactSlotCard(
                        slot = slot,
                        status = targetStatus,
                        scheduleDate = schedule.date,
                        onClick = { onSlotClick(slot) }
                    )
                }
            }
        }

        item(key = "nav_spacer") {
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
                    .height(7.dp)
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
fun ActiveSlotCard(
    slot: TodaySlot,
    now: LocalTime,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = ZoneStage1DarkText.copy(alpha = 0.35f)
    val bgColor = ZoneStage1DarkText.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = getLocalizedPeriod(slot.slot).uppercase(),
                        style = ActiveSlotNameStyle,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = slot.time,
                        style = ActiveSlotTimeStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val chipState = remember(slot.time, now) {
                    computeActiveChipState(slot.time, now)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ZoneStage1DarkText.copy(alpha = 0.12f))
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ZoneStage1DarkText)
                        )
                        Text(
                            text = formatActiveChipText(chipState),
                            style = ActiveChipTextStyle,
                            color = ZoneStage1DarkText
                        )
                    }
                }
            }

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                slot.meds.forEach { med ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                                CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = med.medicine,
                                style = ActiveMedNameStyle,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            if (med.doseAmount.isNotBlank()) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = med.doseAmount,
                                    style = ActiveMedDoseStyle,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val medCount = slot.meds.size
            val slotPeriodName = getLocalizedPeriod(slot.slot)
            val buttonCd = pluralStringResource(
                R.plurals.schedule_confirm_content_description,
                medCount,
                slotPeriodName,
                medCount
            )

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = primaryColor.copy(alpha = 0.7f),
                        spotColor = primaryColor.copy(alpha = 0.7f)
                    )
                    .semantics { contentDescription = buttonCd },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = DarkBgPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = DarkBgPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.schedule_confirm_btn),
                        style = ActiveButtonTextStyle,
                        color = DarkBgPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun CompactSlotCard(
    slot: TodaySlot,
    status: SlotStatus,
    scheduleDate: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val bgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (iconBgColor, iconTint, iconVector) = when (status) {
                SlotStatus.DONE -> Triple(
                    ColorSuccess.copy(alpha = 0.14f),
                    ColorPulse,
                    Icons.Default.Check
                )
                SlotStatus.UPCOMING -> Triple(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    Icons.Outlined.Schedule
                )
                SlotStatus.MISSED -> Triple(
                    ColorDanger.copy(alpha = 0.14f),
                    ColorMissedRed,
                    Icons.Default.PriorityHigh
                )
                SlotStatus.ACTIVE -> Triple(
                    Color.Transparent,
                    Color.Transparent,
                    Icons.Default.Check
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getLocalizedPeriod(slot.slot),
                        style = CompactSlotNameStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.alignByBaseline()
                    )
                    when (status) {
                        SlotStatus.DONE -> {
                            val prefix = stringResource(R.string.schedule_compact_taken_prefix)
                            val takenTime = formatTakenAtTime(slot.takenAt)
                            val text = buildAnnotatedString {
                                append(prefix)
                                val start = length
                                append(takenTime)
                                addStyle(
                                    CompactStatusMonoSpanStyle.copy(color = ColorPulse),
                                    start,
                                    length
                                )
                            }
                            Text(
                                text = text,
                                style = CompactStatusTextStyle,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                        SlotStatus.UPCOMING -> {
                            val prefix = stringResource(R.string.schedule_compact_at_prefix)
                            val text = buildAnnotatedString {
                                append(prefix)
                                val start = length
                                append(slot.time)
                                addStyle(
                                    CompactStatusMonoSpanStyle.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    ),
                                    start,
                                    length
                                )
                            }
                            Text(
                                text = text,
                                style = CompactStatusTextStyle,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                        SlotStatus.MISSED -> {
                            Text(
                                text = stringResource(R.string.schedule_compact_missed),
                                style = CompactStatusTextStyle,
                                color = ColorMissedRed,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                        SlotStatus.ACTIVE -> {}
                    }
                }

                Text(
                    text = formatCompactMeds(slot.meds),
                    style = CompactMedsTextStyle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (status == SlotStatus.DONE) {
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
                        text = formatWithMonoDigits(deviation, CompactDeviationMonoSpanStyle),
                        style = CompactDeviationStyle,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
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
