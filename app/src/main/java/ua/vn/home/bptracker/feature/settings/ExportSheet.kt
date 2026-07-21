package ua.vn.home.bptracker.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.ui.components.SegmentedControl
import ua.vn.home.bptracker.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    selectedPeriod: ExportPeriod,
    isSending: Boolean,
    onPeriodSelect: (ExportPeriod) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(MaterialTheme.spacing.screenPadding)
                .padding(bottom = MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
                Text(
                    text = stringResource(R.string.export_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.export_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SegmentedControl(
                options = listOf(
                    stringResource(R.string.export_period_1m),
                    stringResource(R.string.export_period_3m),
                    stringResource(R.string.export_period_6m),
                    stringResource(R.string.export_period_all)
                ),
                selectedIndex = selectedPeriod.ordinal,
                onSelect = { onPeriodSelect(ExportPeriod.entries[it]) }
            )

            Button(
                onClick = onSend,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = stringResource(R.string.export_send))
                }
            }
        }
    }
}
