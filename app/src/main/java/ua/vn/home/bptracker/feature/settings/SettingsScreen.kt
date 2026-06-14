package ua.vn.home.bptracker.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.config.AppLanguage
import ua.vn.home.bptracker.core.config.AppTheme
import ua.vn.home.bptracker.ui.components.ListGroupCard
import ua.vn.home.bptracker.ui.components.SegmentedControl
import ua.vn.home.bptracker.ui.components.SettingRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onThemeSelect: (AppTheme) -> Unit,
    onLanguageSelect: (AppLanguage) -> Unit,
    onOcrImprovementToggle: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onHelpClick: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onHelpClick) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Help")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Appearance
            ListGroupCard(title = stringResource(R.string.settings_group_appearance)) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SegmentedControl(
                        options = listOf(
                            stringResource(R.string.settings_theme_auto),
                            stringResource(R.string.settings_theme_light),
                            stringResource(R.string.settings_theme_dark)
                        ),
                        selectedIndex = state.theme.ordinal,
                        onSelect = { onThemeSelect(AppTheme.entries[it]) }
                    )
                }
            }

            // Language
            ListGroupCard(title = stringResource(R.string.settings_group_language)) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    SegmentedControl(
                        options = listOf("System", "Українська", "English"),
                        selectedIndex = state.language.ordinal,
                        onSelect = { onLanguageSelect(AppLanguage.entries[it]) }
                    )
                }
            }

            // Privacy
            ListGroupCard(title = stringResource(R.string.settings_group_privacy)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_ocr_improve), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.settings_ocr_improve_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.ocrImprovement,
                        onCheckedChange = onOcrImprovementToggle
                    )
                }
            }

            // Help
            ListGroupCard(title = stringResource(R.string.settings_group_help)) {
                SettingRow(
                    label = stringResource(R.string.settings_bp_scale),
                    onClick = onHelpClick
                )
            }

            // Account
            ListGroupCard(title = stringResource(R.string.settings_group_account)) {
                SettingRow(
                    label = "Server",
                    value = "https://api-bptracker...",
                    showChevron = false
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                SettingRow(
                    label = stringResource(R.string.settings_export_csv),
                    onClick = { 
                        // Plan says: redirect to existing backend /export/csv (email)
                        // For now we'll simulate opening the browser to the export endpoint
                        // or using an email intent if we had the file. 
                        // Given "redirect to backend", we use URL.
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("https://api-bptracker.home.vn.ua/api/v1/export/csv")
                        }
                        try {
                            val context = ua.vn.home.bptracker.core.di.ServiceLocator.applicationContext
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                SettingRow(
                    label = stringResource(R.string.settings_logout),
                    labelColor = MaterialTheme.colorScheme.error,
                    onClick = onLogout,
                    showChevron = false
                )
            }

            // Version
            Text(
                text = "BP Tracker v${state.version}\nBuilt with ♡ in Ukraine",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
