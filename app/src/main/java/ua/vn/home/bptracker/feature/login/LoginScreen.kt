package ua.vn.home.bptracker.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.ui.theme.BPTrackerTheme

@Composable
fun LoginScreen(
    info: String? = null,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BP Tracker")
        Button(onClick = onSignIn, modifier = Modifier.padding(top = 24.dp)) {
            Text("Sign in with passkey")
        }
        if (info != null) {
            Text(
                text = info,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    BPTrackerTheme {
        LoginScreen(onSignIn = {})
    }
}
