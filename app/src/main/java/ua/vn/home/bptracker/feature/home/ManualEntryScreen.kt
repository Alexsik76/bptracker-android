package ua.vn.home.bptracker.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.ui.OperationUiState
import ua.vn.home.bptracker.ui.theme.*
import ua.vn.home.bptracker.ui.components.*

@Composable
fun ManualEntryScreen(
    state: ManualEntryState,
    onSysChange: (String) -> Unit,
    onDiaChange: (String) -> Unit,
    onPulseChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(state.saveOperation) {
        if (state.saveOperation is OperationUiState.Success) onBack()
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.local_ocr_result_title),
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(MaterialTheme.spacing.small).size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = stringResource(R.string.common_back),
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
        ) {
            // Photo Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AddAPhoto, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                )
            }

            // Entry Fields
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                ValueField(
                    label = stringResource(R.string.entry_sys),
                    secondary = stringResource(R.string.entry_sys_sub),
                    dotColor = ColorSys,
                    value = state.sys,
                    unit = stringResource(R.string.dashboard_units_mmHg),
                    onValueChange = onSysChange,
                    isValid = state.sysValid
                )
                ValueField(
                    label = stringResource(R.string.entry_dia),
                    secondary = stringResource(R.string.entry_dia_sub),
                    dotColor = ColorDia,
                    value = state.dia,
                    unit = stringResource(R.string.dashboard_units_mmHg),
                    onValueChange = onDiaChange,
                    isValid = state.diaValid
                )
                ValueField(
                    label = stringResource(R.string.entry_pulse),
                    secondary = "",
                    dotColor = ColorPulse,
                    value = state.pulse,
                    unit = stringResource(R.string.dashboard_units_bpm),
                    onValueChange = onPulseChange,
                    isValid = state.pulseValid
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.large))

            if (state.saveOperation is OperationUiState.Error) {
                Text(
                    text = state.saveOperation.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.listSpacing)
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.common_cancel), fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = state.isValid && state.saveOperation !is OperationUiState.InProgress,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (state.saveOperation is OperationUiState.InProgress) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(stringResource(R.string.common_save), fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
            
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}
