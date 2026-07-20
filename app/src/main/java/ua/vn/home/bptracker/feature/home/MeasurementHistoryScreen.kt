package ua.vn.home.bptracker.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
    state: ListUiState<HomePayload>,
    onRefresh: () -> Unit,
    onMeasurementClick: (MeasurementDto) -> Unit,
    onBack: () -> Unit,
) {
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
                            title = stringResource(R.string.dashboard_no_measurements)
                        )
                    }
                }
            ) { content, isRefreshing ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (content.recent.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = MaterialTheme.spacing.large),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(content.recent) { m ->
                                MeasurementRow(m, onClick = { onMeasurementClick(m) })
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.cardPadding),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                )
                            }
                            item {
                                Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
