package ua.vn.home.bptracker.feature.prescriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.data.dto.PrescriptionReadDto
import ua.vn.home.bptracker.ui.components.EmptyState
import ua.vn.home.bptracker.ui.components.ListStateHost
import ua.vn.home.bptracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionsScreen(
    state: ListUiState<List<PrescriptionReadDto>>,
    onAddClick: () -> Unit,
    onPrescriptionClick: (PrescriptionReadDto) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prescriptions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = onAddClick) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.prescriptions_add_btn))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.large),
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ListStateHost(
                state = state,
                onRetry = onRefresh,
                onEmpty = {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        EmptyState(
                            title = stringResource(R.string.prescriptions_empty),
                            description = stringResource(R.string.prescriptions_empty_hint)
                        )
                    }
                }
            ) { list, refreshing ->
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (list.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = MaterialTheme.spacing.large)
                        ) {
                            items(list) { prescription ->
                                PrescriptionRow(
                                    prescription = prescription,
                                    onClick = { onPrescriptionClick(prescription) }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                )
                            }
                            item {
                                Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                            }
                        }
                    } else {
                        // This branch is for the initial skeleton frame: Content(emptyList, refreshing=true)
                        // We show nothing or a specific skeleton if we want, but keeping it empty is fine
                        // as the transition to Empty state will happen once refreshing is false.
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
fun PrescriptionRow(
    prescription: PrescriptionReadDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prescription.doctor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.prescriptions_prescribed_on, prescription.prescribedOn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Surface(
            color = if (prescription.isActive) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = stringResource(
                    if (prescription.isActive) R.string.prescriptions_active 
                    else R.string.prescriptions_inactive
                ),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (prescription.isActive) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
