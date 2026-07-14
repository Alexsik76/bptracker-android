package ua.vn.home.bptracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.LocalActivityResultRegistryOwner
import ua.vn.home.bptracker.core.config.AppLanguage
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.feature.camera.CameraScanScreen
import ua.vn.home.bptracker.feature.home.*
import ua.vn.home.bptracker.feature.login.AuthState
import ua.vn.home.bptracker.feature.login.AuthViewModel
import ua.vn.home.bptracker.feature.login.LoginScreen
import ua.vn.home.bptracker.feature.settings.BpScaleHelpScreen
import ua.vn.home.bptracker.feature.settings.SettingsScreen
import ua.vn.home.bptracker.feature.settings.SettingsViewModel
import ua.vn.home.bptracker.ui.components.BpBottomNavBar
import ua.vn.home.bptracker.ui.components.LocalActivity
import ua.vn.home.bptracker.ui.theme.BPTrackerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private var latestIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        latestIntent = intent
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val settingsState by settingsVm.state.collectAsState()
            
            val isDark = when (settingsState.theme) {
                ua.vn.home.bptracker.core.config.AppTheme.AUTO -> isSystemInDarkTheme()
                ua.vn.home.bptracker.core.config.AppTheme.LIGHT -> false
                ua.vn.home.bptracker.core.config.AppTheme.DARK -> true
            }

            val locale = when (settingsState.language) {
                AppLanguage.UA -> Locale.forLanguageTag("uk")
                AppLanguage.EN -> Locale.forLanguageTag("en")
                AppLanguage.SYSTEM -> Locale.getDefault()
            }

            val context = LocalContext.current
            val configuration = Configuration(LocalConfiguration.current)
            configuration.setLocale(locale)
            val localizedContext = context.createConfigurationContext(configuration)

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivity provides this,
                LocalActivityResultRegistryOwner provides this
            ) {
                BPTrackerTheme(darkTheme = isDark) {
                    val authVm: AuthViewModel = viewModel()
                    val state by authVm.state.collectAsState()
                    val passkeyResult by authVm.passkeyResult.collectAsState()
                    val snackbarHostState = remember { SnackbarHostState() }

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(Modifier.padding(innerPadding)) {
                            when (val s = state) {
                                is AuthState.Loading -> Box(Modifier.fillMaxSize()) {
                                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                                }
                                is AuthState.LoggedOut -> LoginScreen(
                                    info = s.info,
                                    signingIn = s.signingIn,
                                    linkSent = s.linkSent,
                                    onSignIn = { activity -> authVm.signIn(activity) },
                                    onRequestMagicLink = { email -> authVm.requestMagicLink(email) }
                                )
                                is AuthState.LoggedIn -> {
                                    MainAuthenticatedLayout(
                                        authVm = authVm,
                                        onLogout = authVm::logout
                                    )
                                    
                                    if (s.showEnrollPrompt) {
                                        val activity = LocalActivity.current
                                        AlertDialog(
                                            onDismissRequest = { authVm.dismissEnrollPrompt() },
                                            title = { Text(stringResource(R.string.auth_add_passkey)) },
                                            text = { Text(stringResource(R.string.auth_add_passkey_prompt)) },
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    activity?.let {
                                                        authVm.registerPasskey(it)
                                                        authVm.dismissEnrollPrompt()
                                                    }
                                                }) {
                                                    Text(stringResource(R.string.common_save))
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { authVm.dismissEnrollPrompt() }) {
                                                    Text(stringResource(R.string.common_cancel))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(latestIntent) {
                        authVm.handleIntent(latestIntent?.data)
                    }

                    val res = passkeyResult
                    if (res != null) {
                        val message = stringResource(res)
                        LaunchedEffect(res) {
                            snackbarHostState.showSnackbar(message)
                            authVm.consumePasskeyResult()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestIntent = intent
    }
}

@Composable
fun MainAuthenticatedLayout(authVm: AuthViewModel, onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeOverlay by remember { mutableStateOf<String?>(null) }
    var selectedMeasurement by remember { mutableStateOf<MeasurementDto?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    when (activeOverlay) {
        "camera" -> {
            BackHandler { activeOverlay = null }
            CameraScanScreen(
                onCapture = { bitmap ->
                    capturedBitmap = bitmap
                    activeOverlay = "ocr_review"
                },
                onEnterManually = { activeOverlay = "manual_entry" },
                onCancel = { activeOverlay = null }
            )
        }
        "ocr_review" -> {
            val reviewVm: ScanReviewViewModel = viewModel()
            val reviewState by reviewVm.state.collectAsState()
            
            BackHandler {
                reviewVm.reset()
                activeOverlay = "camera"
            }

            capturedBitmap?.let { bitmap ->
                LaunchedEffect(bitmap) {
                    reviewVm.initWithImage(bitmap)
                }
            }

            ScanReviewScreen(
                state = reviewState,
                onSysChange = reviewVm::onSysChange,
                onDiaChange = reviewVm::onDiaChange,
                onPulseChange = reviewVm::onPulseChange,
                onSave = reviewVm::save,
                onRetake = { 
                    reviewVm.reset()
                    activeOverlay = "camera" 
                },
                onRemoteOcr = reviewVm::recognizeRemote,
                onBack = { 
                    reviewVm.reset()
                    activeOverlay = null
                    capturedBitmap = null
                }
            )
        }
        "manual_entry" -> {
            val entryVm: ManualEntryViewModel = viewModel()
            val entryState by entryVm.state.collectAsState()
            BackHandler { activeOverlay = null }
            ManualEntryScreen(
                state = entryState,
                onSysChange = entryVm::onSysChange,
                onDiaChange = entryVm::onDiaChange,
                onPulseChange = entryVm::onPulseChange,
                onSave = entryVm::save,
                onBack = { activeOverlay = null }
            )
        }
        "measurement_detail" -> {
            val detailVm: MeasurementDetailViewModel = viewModel()
            val detailState by detailVm.state.collectAsState()
            
            BackHandler {
                activeOverlay = null
                selectedMeasurement = null
            }

            LaunchedEffect(selectedMeasurement) {
                selectedMeasurement?.let { detailVm.setMeasurement(it) }
            }

            MeasurementDetailScreen(
                state = detailState,
                onDelete = detailVm::delete,
                onBack = { 
                    activeOverlay = null
                    selectedMeasurement = null
                }
            )
        }
        "settings" -> {
            val settingsVm: SettingsViewModel = viewModel()
            val settingsState by settingsVm.state.collectAsState()
            BackHandler { activeOverlay = null }
            val activity = LocalActivity.current
            SettingsScreen(
                state = settingsState,
                onThemeSelect = settingsVm::setTheme,
                onLanguageSelect = settingsVm::setLanguage,
                onOcrImprovementToggle = settingsVm::setOcrImprovement,
                onRemindersToggle = settingsVm::setRemindersEnabled,
                onLogout = onLogout,
                onAddPasskey = {
                    activity?.let { authVm.registerPasskey(it) }
                },
                onHelpClick = { activeOverlay = "bp_scale" },
                onBack = { activeOverlay = null },
                onRefresh = settingsVm::refresh
            )
        }
        "bp_scale" -> {
            BackHandler { activeOverlay = "settings" }
            BpScaleHelpScreen(
                latestMeasurement = selectedMeasurement, // Simplified for now
                onBack = { activeOverlay = "settings" }
            )
        }
        "history" -> {
            val homeVm: HomeViewModel = viewModel()
            val homeState by homeVm.state.collectAsState()
            
            BackHandler { activeOverlay = null }

            MeasurementHistoryScreen(
                state = homeState,
                onRefresh = homeVm::refresh,
                onMeasurementClick = { m ->
                    selectedMeasurement = m
                    activeOverlay = "measurement_detail"
                },
                onBack = { activeOverlay = null }
            )
        }
        "schedule_edit" -> {
            val editVm: ScheduleEditViewModel = viewModel()
            val editState by editVm.state.collectAsState()
            val scheduleVm: ScheduleViewModel = viewModel()

            BackHandler { activeOverlay = null }

            ScheduleEditScreen(
                state = editState,
                onTimeChange = editVm::onTimeChange,
                onDurationChange = editVm::onDurationChange,
                onMaxRemindersChange = editVm::onMaxRemindersChange,
                onSave = {
                    editVm.save()
                },
                onRetry = editVm::load,
                onBack = { 
                    activeOverlay = null
                    scheduleVm.refresh()
                }
            )
        }
        null -> {
            BackHandler(enabled = selectedTab != 0) {
                selectedTab = 0
            }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    BpBottomNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onScanClick = { activeOverlay = "camera" }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (selectedTab) {
                        0 -> {
                            val homeVm: HomeViewModel = viewModel()
                            val homeState by homeVm.state.collectAsState()
                            LaunchedEffect(Unit) { homeVm.refresh() }

                            HomeScreen(
                                state = homeState,
                                onRefresh = homeVm::refresh,
                                onLogout = onLogout,
                                onSettingsClick = { 
                                    if (homeState is HomeState.Content) {
                                        selectedMeasurement = (homeState as HomeState.Content).latest
                                    }
                                    activeOverlay = "settings" 
                                },
                                onHistoryClick = { activeOverlay = "history" },
                                onMeasurementClick = { m ->
                                    selectedMeasurement = m
                                    activeOverlay = "measurement_detail"
                                }
                            )
                        }
                        1 -> {
                            val scheduleVm: ScheduleViewModel = viewModel()
                            val scheduleState by scheduleVm.state.collectAsState()
                            ScheduleScreen(
                                state = scheduleState,
                                onConfirm = scheduleVm::confirm,
                                onRefresh = scheduleVm::refresh,
                                onEditClick = { activeOverlay = "schedule_edit" }
                            )
                        }
                    }
                }
            }
        }
    }
}
