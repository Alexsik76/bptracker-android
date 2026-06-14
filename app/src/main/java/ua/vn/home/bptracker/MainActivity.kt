package ua.vn.home.bptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.vn.home.bptracker.feature.camera.CameraScanScreen
import ua.vn.home.bptracker.feature.home.*
import ua.vn.home.bptracker.feature.login.AuthState
import ua.vn.home.bptracker.feature.login.AuthViewModel
import ua.vn.home.bptracker.feature.login.LoginScreen
import ua.vn.home.bptracker.ui.components.BpBottomNavBar
import ua.vn.home.bptracker.ui.theme.BPTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BPTrackerTheme {
                val vm: AuthViewModel = viewModel()
                val state by vm.state.collectAsState()

                when (val s = state) {
                    is AuthState.Loading -> Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                    is AuthState.LoggedOut -> LoginScreen(
                        info = s.info,
                        signingIn = s.signingIn,
                        onSignIn = { activity -> vm.signIn(activity) }
                    )
                    is AuthState.LoggedIn -> MainAuthenticatedLayout(onLogout = vm::logout)
                }
            }
        }
    }
}

@Composable
fun MainAuthenticatedLayout(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeOverlay by remember { mutableStateOf<String?>(null) } // Simple state-based routing
    
    // When an overlay is active (e.g. Manual Entry), we show it full screen.
    // Otherwise, we show the tabbed Scaffold.
    when (activeOverlay) {
        "camera" -> {
            CameraScanScreen(
                onCapture = { activeOverlay = "manual_entry" }, // TODO: Pass captured image
                onCancel = { activeOverlay = null }
            )
        }
        "manual_entry" -> {
            val entryVm: ManualEntryViewModel = viewModel()
            val entryState by entryVm.state.collectAsState()
            ManualEntryScreen(
                state = entryState,
                onSysChange = entryVm::onSysChange,
                onDiaChange = entryVm::onDiaChange,
                onPulseChange = entryVm::onPulseChange,
                onSave = entryVm::save,
                onBack = { activeOverlay = null }
            )
        }
        null -> {
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
                                onLogout = onLogout
                            )
                        }
                        1 -> {
                            val scheduleVm: ScheduleViewModel = viewModel()
                            val scheduleState by scheduleVm.state.collectAsState()
                            ScheduleScreen(
                                state = scheduleState,
                                onConfirm = scheduleVm::confirm,
                                onRefresh = scheduleVm::refresh
                            )
                        }
                    }
                }
            }
        }
    }
}
