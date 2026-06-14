package ua.vn.home.bptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.vn.home.bptracker.feature.home.HomeScreen
import ua.vn.home.bptracker.feature.home.HomeViewModel
import ua.vn.home.bptracker.feature.login.AuthState
import ua.vn.home.bptracker.feature.login.AuthViewModel
import ua.vn.home.bptracker.feature.login.LoginScreen
import ua.vn.home.bptracker.ui.theme.BPTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BPTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val vm: AuthViewModel = viewModel()
                    val state by vm.state.collectAsState()
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (val s = state) {
                            is AuthState.Loading ->
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            is AuthState.LoggedOut ->
                                LoginScreen(
                                    info = s.info,
                                    signingIn = s.signingIn,
                                    onSignIn = { activity -> vm.signIn(activity) }
                                )
                            is AuthState.LoggedIn -> {
                                val homeVm: HomeViewModel = viewModel()
                                val homeState by homeVm.state.collectAsState()
                                HomeScreen(
                                    state = homeState,
                                    onRefresh = homeVm::refresh,
                                    onLogout = vm::logout
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
