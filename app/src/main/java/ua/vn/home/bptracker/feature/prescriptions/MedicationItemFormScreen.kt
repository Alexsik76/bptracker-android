package ua.vn.home.bptracker.feature.prescriptions

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
import ua.vn.home.bptracker.data.dto.*
import ua.vn.home.bptracker.ui.theme.DarkPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationItemFormScreen(
    state: MedicationItemFormState,
    onMedicineChange: (String) -> Unit,
    onConditionChange: (String) -> Unit,
    onWhenSlotsChange: (WhenSlot, Boolean) -> Unit,
    onDoseAmountChange: (String) -> Unit,
    onDoseUnitChange: (DoseUnit?) -> Unit,
    onFreqCountChange: (Int) -> Unit,
    onFreqPeriodChange: (Int) -> Unit,
    onFreqPeriodUnitChange: (FreqPeriodUnit) -> Unit,
    onCourseTypeChange: (CourseType) -> Unit,
    onCourseStartChange: (String) -> Unit,
    onCourseIntakesChange: (Int?) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (state.id == null) stringResource(R.string.med_items_add_btn) else state.medicine) 
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.medicine,
                onValueChange = onMedicineChange,
                label = { Text(stringResource(R.string.med_items_medicine_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.condition ?: "",
                onValueChange = onConditionChange,
                label = { Text(stringResource(R.string.med_items_condition_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.med_items_when), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WhenSlot.entries.forEach { slot ->
                    FilterChip(
                        selected = state.whenSlots.contains(slot),
                        onClick = { onWhenSlotsChange(slot, !state.whenSlots.contains(slot)) },
                        label = { Text(slot.name) }
                    )
                }
            }

            Text(stringResource(R.string.med_items_dose), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.doseAmount,
                    onValueChange = onDoseAmountChange,
                    label = { Text(stringResource(R.string.med_items_dose_amount)) },
                    modifier = Modifier.weight(1f)
                )
                var unitExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = state.doseUnit?.name ?: stringResource(R.string.med_items_dose_unspecified),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.med_items_dose_unit)) },
                        trailingIcon = { 
                            IconButton(onClick = { unitExpanded = true }) {
                                Text("▼") 
                            }
                        }
                    )
                    DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.med_items_dose_unspecified)) }, 
                            onClick = { onDoseUnitChange(null); unitExpanded = false }
                        )
                        DoseUnit.entries.forEach { unit ->
                            DropdownMenuItem(text = { Text(unit.name) }, onClick = { onDoseUnitChange(unit); unitExpanded = false })
                        }
                    }
                }
            }

            Text(stringResource(R.string.med_items_frequency), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.freqCount.toString(),
                    onValueChange = { onFreqCountChange(it.toIntOrNull() ?: 1) },
                    modifier = Modifier.width(60.dp)
                )
                Text(stringResource(R.string.med_items_freq_times))
                Text(stringResource(R.string.med_items_freq_every))
                OutlinedTextField(
                    value = state.freqPeriod.toString(),
                    onValueChange = { onFreqPeriodChange(it.toIntOrNull() ?: 1) },
                    modifier = Modifier.width(60.dp)
                )
                var periodExpanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { periodExpanded = true }) {
                        Text(state.freqPeriodUnit.name)
                    }
                    DropdownMenu(expanded = periodExpanded, onDismissRequest = { periodExpanded = false }) {
                        FreqPeriodUnit.entries.forEach { unit ->
                            DropdownMenuItem(text = { Text(unit.name) }, onClick = { onFreqPeriodUnitChange(unit); periodExpanded = false })
                        }
                    }
                }
            }

            Text(stringResource(R.string.med_items_course), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.courseType == CourseType.Ongoing,
                    onClick = { onCourseTypeChange(CourseType.Ongoing) },
                    label = { Text(stringResource(R.string.med_items_course_ongoing)) }
                )
                FilterChip(
                    selected = state.courseType == CourseType.Course,
                    onClick = { onCourseTypeChange(CourseType.Course) },
                    label = { Text(stringResource(R.string.med_items_course_limited)) }
                )
            }

            if (state.courseType == CourseType.Course) {
                OutlinedTextField(
                    value = state.courseStart ?: "",
                    onValueChange = onCourseStartChange,
                    label = { Text(stringResource(R.string.med_items_course_start)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.courseIntakes?.toString() ?: "",
                    onValueChange = { onCourseIntakesChange(it.toIntOrNull()) },
                    label = { Text(stringResource(R.string.med_items_course_intakes)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.error != null) {
                Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state.isValid && !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(stringResource(R.string.common_save), fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
