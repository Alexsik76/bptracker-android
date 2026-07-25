package ua.vn.home.bptracker.feature.prescriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.data.dto.CourseType
import ua.vn.home.bptracker.data.dto.MedicationItemReadDto
import ua.vn.home.bptracker.ui.components.EmptyState
import ua.vn.home.bptracker.ui.components.ListStateHost
import ua.vn.home.bptracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionDetailScreen(
    state: ListUiState<PrescriptionDetailPayload>,
    onEditPrescription: () -> Unit,
    onDeletePrescription: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (MedicationItemReadDto) -> Unit,
    onDeleteItem: (String) -> Unit,
    onBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.measurement_delete_confirm_title)) },
            text = { Text(stringResource(R.string.prescriptions_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeletePrescription()
                }) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            val title = (state as? ListUiState.Content)?.data?.prescription?.doctor ?: ""
            TopAppBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEditPrescription) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        ListStateHost(
            state = state,
            onRetry = { /* TODO */ },
            modifier = Modifier.padding(padding)
        ) { payload, _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val p = payload.prescription
                val items = payload.items

                PrescriptionHeader(p)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.med_items_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onAddItem) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(MaterialTheme.spacing.extraSmall))
                        Text(stringResource(R.string.med_items_add_btn))
                    }
                }

                if (items.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.med_items_empty),
                        description = ""
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items) { item ->
                            MedicationItemRow(
                                item = item,
                                onClick = { onEditItem(item) },
                                onDelete = { onDeleteItem(item.id) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            )
                        }
                        item {
                            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrescriptionHeader(p: ua.vn.home.bptracker.data.dto.PrescriptionReadDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(
                text = stringResource(R.string.prescriptions_prescribed_on, p.prescribedOn),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Surface(
                color = if (p.isActive) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = stringResource(
                        if (p.isActive) R.string.prescriptions_active 
                        else R.string.prescriptions_inactive
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.small,
                        vertical = MaterialTheme.spacing.extraSmall
                    ),
                    color = if (p.isActive) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MedicationItemRow(
    item: MedicationItemReadDto,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.medicine,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (!item.condition.isNullOrBlank()) {
                Text(
                    text = item.condition,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            val doseUnitLabel = item.doseUnit?.let { stringResource(it.labelRes()) } ?: ""
            val slotsLabels = item.whenSlots.map { stringResource(it.labelRes()) }
            val slotsLabel = slotsLabels.joinToString()
            
            Text(
                text = "${item.doseAmount} $doseUnitLabel · $slotsLabel",
                style = MaterialTheme.typography.bodySmall
            )
            val freqUnitLabel = stringResource(item.freqPeriodUnit.labelRes())
            val courseLabel = if (item.courseType == CourseType.Ongoing) 
                stringResource(R.string.med_items_course_ongoing) 
            else 
                stringResource(R.string.med_items_course_limited)
            
            Text(
                text = "${item.freqCount}x per ${item.freqPeriod} $freqUnitLabel · $courseLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
        }
    }
}
