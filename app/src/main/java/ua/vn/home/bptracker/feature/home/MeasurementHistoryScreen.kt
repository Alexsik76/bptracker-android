package ua.vn.home.bptracker.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.ui.components.EmptyState
import ua.vn.home.bptracker.ui.components.ListStateHost
import ua.vn.home.bptracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementHistoryScreen(
    state: ListUiState<HistoryState>,
    onRefresh: () -> Unit,
    onPeriodSelect: (MeasurementPeriod) -> Unit,
    onMeasurementClick: (MeasurementDto) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_recent_readings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        ) {
            val content = (state as? ListUiState.Content)?.data
            val currentPeriod = content?.period ?: MeasurementPeriod.MONTH

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.cardPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeasurementPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = period == currentPeriod,
                        onClick = { onPeriodSelect(period) },
                        label = { Text(stringResource(period.labelRes)) }
                    )
                }
            }

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
                            title = stringResource(R.string.dashboard_no_measurements)
                        )
                    }
                }
            ) { data, isRefreshing ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.large),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(data.measurements) { m ->
                            MeasurementRow(m, onClick = { onMeasurementClick(m) })
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.cardPadding),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            )
                        }

                        if (data.isFilling) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
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
