package ua.vn.home.bptracker.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.ui.OperationUiState
import ua.vn.home.bptracker.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val localContext = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saveState) {
        if (state.saveState is OperationUiState.Success) {
            snackbarHostState.showSnackbar(localContext.getString(R.string.profile_saved))
        }
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
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
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.profile_email_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = state.email,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.profile_name_label)) },
                    supportingText = { Text(stringResource(R.string.profile_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.saveState !is OperationUiState.InProgress
                )

                if (state.saveState is OperationUiState.Error) {
                    Text(
                        text = state.saveState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.saveState !is OperationUiState.InProgress
                ) {
                    if (state.saveState is OperationUiState.InProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.profile_save))
                    }
                }
            }
        }
    }
}
