package ua.vn.home.bptracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.config.AppLanguage
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.feature.camera.CameraScanScreen
import ua.vn.home.bptracker.feature.home.*
import ua.vn.home.bptracker.feature.login.AuthState
import ua.vn.home.bptracker.feature.login.AuthViewModel
import ua.vn.home.bptracker.feature.login.LoginScreen
import ua.vn.home.bptracker.feature.prescriptions.*
import ua.vn.home.bptracker.feature.reminders.ReminderConfigScreen
import ua.vn.home.bptracker.feature.reminders.ReminderConfigViewModel
import ua.vn.home.bptracker.feature.settings.BpScaleHelpScreen
import ua.vn.home.bptracker.feature.settings.SettingsScreen
import ua.vn.home.bptracker.feature.settings.SettingsViewModel
import ua.vn.home.bptracker.ui.components.BpBottomNavBar
import ua.vn.home.bptracker.ui.components.LocalActivity
import ua.vn.home.bptracker.ui.theme.BPTrackerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
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

        lifecycleScope.launch {
            if (ServiceLocator.settingsStore.remindersEnabled.first()) {
                ServiceLocator.reminderScheduler.rescheduleAll()
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
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(Modifier.padding(innerPadding)) {
                            when (val s = state) {
                                is AuthState.Loading -> Box(Modifier.fillMaxSize().systemBarsPadding()) {
                                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                                }
                                is AuthState.LoggedOut -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                                    LoginScreen(
                                        info = s.info,
                                        signingIn = s.signingIn,
                                        linkSent = s.linkSent,
                                        onSignIn = { activity -> authVm.signIn(activity) },
                                        onRequestMagicLink = { email -> authVm.requestMagicLink(email) },
                                    )
                                }
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
                                                TextButton(
                                                    onClick = {
                                                        activity?.let {
                                                            authVm.registerPasskey(it)
                                                            authVm.dismissEnrollPrompt()
                                                        }
                                                    },
                                                ) {
                                                    Text(stringResource(R.string.common_save))
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { authVm.dismissEnrollPrompt() }) {
                                                    Text(stringResource(R.string.common_cancel))
                                                }
                                            },
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
    val navController = rememberNavController()
    val homeVm: HomeViewModel = viewModel()
    var selectedMeasurement by remember { mutableStateOf<MeasurementDto?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("home", "schedule")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                val selectedTab = if (currentRoute == "schedule") 1 else 0
                BpBottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        val route = if (tab == 1) "schedule" else "home"
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onScanClick = { navController.navigate("camera") }
                )
            }
        }
    ) { innerPadding ->
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.padding(innerPadding),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
            composable("home") {
                val homeState by homeVm.state.collectAsState()

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    homeVm.refresh()
                }

                HomeScreen(
                    state = homeState,
                    onRefresh = homeVm::refresh,
                    onSettingsClick = {
                        if (homeState is HomeState.Content) {
                            selectedMeasurement = (homeState as HomeState.Content).latest
                        }
                        navController.navigate("settings")
                    },
                    onHistoryClick = { navController.navigate("history") },
                    onMeasurementClick = { m ->
                        selectedMeasurement = m
                        navController.navigate("measurement_detail")
                    },
                )
            }

            composable("schedule") {
                val scheduleVm: ScheduleViewModel = viewModel()
                val scheduleState by scheduleVm.state.collectAsState()

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    scheduleVm.refresh()
                }

                ScheduleScreen(
                    state = scheduleState,
                    onConfirm = scheduleVm::confirmSlot,
                    onEditTime = scheduleVm::editTime,
                    onDelete = scheduleVm::deleteIntake,
                    onRefresh = scheduleVm::refresh,
                    onEditClick = { navController.navigate("reminder_config") },
                    onPrescriptionsClick = { navController.navigate("prescriptions") }
                )
            }

            composable("camera") {
                CameraScanScreen(
                    onCapture = { bitmap ->
                        capturedBitmap = bitmap
                        navController.navigate("ocr_review")
                    },
                    onEnterManually = { navController.navigate("manual_entry") },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable("ocr_review") {
                val reviewVm: ScanReviewViewModel = viewModel()
                val reviewState by reviewVm.state.collectAsState()

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
                        navController.popBackStack()
                    },
                    onRemoteOcr = reviewVm::recognizeRemote,
                    onBack = {
                        reviewVm.reset()
                        navController.popBackStack("home", inclusive = false)
                        capturedBitmap = null
                    }
                )
            }

            composable("manual_entry") {
                val entryVm: ManualEntryViewModel = viewModel()
                val entryState by entryVm.state.collectAsState()
                ManualEntryScreen(
                    state = entryState,
                    onSysChange = entryVm::onSysChange,
                    onDiaChange = entryVm::onDiaChange,
                    onPulseChange = entryVm::onPulseChange,
                    onSave = entryVm::save,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("measurement_detail") {
                val detailVm: MeasurementDetailViewModel = viewModel()
                val detailState by detailVm.state.collectAsState()

                LaunchedEffect(selectedMeasurement) {
                    selectedMeasurement?.let { detailVm.setMeasurement(it) }
                }

                MeasurementDetailScreen(
                    state = detailState,
                    onDelete = {
                        detailVm.delete()
                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                        selectedMeasurement = null
                    }
                )
            }

            composable("settings") {
                val settingsVm: SettingsViewModel = viewModel()
                val settingsState by settingsVm.state.collectAsState()
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
                    onHelpClick = { navController.navigate("bp_scale") },
                    onBack = { navController.popBackStack() },
                    onRefresh = settingsVm::refresh
                )
            }

            composable("bp_scale") {
                BpScaleHelpScreen(
                    latestMeasurement = selectedMeasurement,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("history") {
                val homeState by homeVm.state.collectAsState()

                MeasurementHistoryScreen(
                    state = homeState,
                    onRefresh = homeVm::refresh,
                    onMeasurementClick = { m ->
                        selectedMeasurement = m
                        navController.navigate("measurement_detail")
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("reminder_config") {
                val configVm: ReminderConfigViewModel = viewModel()
                val configState by configVm.state.collectAsState()

                ReminderConfigScreen(
                    state = configState,
                    onTimeChange = configVm::onTimeChange,
                    onMaxRemindersChange = configVm::onMaxRemindersChange,
                    onDurationChange = configVm::onDurationMinutesChange,
                    onSave = configVm::save,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("prescriptions") {
                val listVm: PrescriptionsViewModel = viewModel()
                val listState by listVm.state.collectAsState()

                LaunchedEffect(Unit) { listVm.refresh() }

                PrescriptionsScreen(
                    state = listState,
                    onAddClick = {
                        navController.navigate("prescription_form")
                    },
                    onPrescriptionClick = { p ->
                        navController.navigate("prescription_detail/${p.id}")
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "prescription_detail/{prescriptionId}",
                arguments = listOf(navArgument("prescriptionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val prescriptionId = backStackEntry.arguments?.getString("prescriptionId")
                val detailVm: PrescriptionDetailViewModel = viewModel()
                val detailState by detailVm.state.collectAsState()

                LaunchedEffect(prescriptionId) {
                    prescriptionId?.let { detailVm.setPrescriptionId(it) }
                }

                PrescriptionDetailScreen(
                    state = detailState,
                    onEditPrescription = {
                        navController.navigate("prescription_form?prescriptionId=$prescriptionId")
                    },
                    onDeletePrescription = {
                        detailVm.deletePrescription()
                        navController.popBackStack()
                    },
                    onAddItem = {
                        navController.navigate("med_item_form/$prescriptionId")
                    },
                    onEditItem = { item ->
                        navController.navigate("med_item_form/$prescriptionId?itemId=${item.id}")
                    },
                    onDeleteItem = detailVm::deleteItem,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "prescription_form?prescriptionId={prescriptionId}",
                arguments = listOf(navArgument("prescriptionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val prescriptionId = backStackEntry.arguments?.getString("prescriptionId")
                val formVm: PrescriptionFormViewModel = viewModel()
                val formState by formVm.state.collectAsState()

                LaunchedEffect(prescriptionId) {
                    formVm.init(prescriptionId)
                }

                LaunchedEffect(formState.isSaved) {
                    if (formState.isSaved) {
                        val newId = formState.savedId
                        if (newId != null) {
                            navController.navigate("prescription_detail/$newId") {
                                popUpTo("prescription_form?prescriptionId={prescriptionId}") { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                }

                PrescriptionFormScreen(
                    state = formState,
                    onDoctorChange = formVm::onDoctorChange,
                    onDateChange = formVm::onDateChange,
                    onIsActiveChange = formVm::onIsActiveChange,
                    onSave = formVm::save,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "med_item_form/{prescriptionId}?itemId={itemId}",
                arguments = listOf(
                    navArgument("prescriptionId") { type = NavType.StringType },
                    navArgument("itemId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val prescriptionId = backStackEntry.arguments?.getString("prescriptionId") ?: ""
                val itemId = backStackEntry.arguments?.getString("itemId")
                val itemFormVm: MedicationItemFormViewModel = viewModel()
                val itemFormState by itemFormVm.state.collectAsState()

                LaunchedEffect(prescriptionId, itemId) {
                    itemFormVm.init(prescriptionId, itemId)
                }

                MedicationItemFormScreen(
                    state = itemFormState,
                    onMedicineChange = itemFormVm::onMedicineChange,
                    onConditionChange = itemFormVm::onConditionChange,
                    onWhenSlotsChange = itemFormVm::onWhenSlotsChange,
                    onDoseAmountChange = itemFormVm::onDoseAmountChange,
                    onDoseUnitChange = itemFormVm::onDoseUnitChange,
                    onFreqCountChange = itemFormVm::onFreqCountChange,
                    onFreqPeriodChange = itemFormVm::onFreqPeriodChange,
                    onFreqPeriodUnitChange = itemFormVm::onFreqPeriodUnitChange,
                    onCourseTypeChange = itemFormVm::onCourseTypeChange,
                    onCourseStartChange = itemFormVm::onCourseStartChange,
                    onCourseIntakesChange = itemFormVm::onCourseIntakesChange,
                    onSave = itemFormVm::save,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
