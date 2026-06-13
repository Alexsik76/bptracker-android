package ua.vn.home.bptracker.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.ui.theme.BPTrackerTheme

@Composable
fun HomeScreen(
    email: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Signed in as $email")
        Button(onClick = onLogout, modifier = Modifier.padding(top = 24.dp)) {
            Text("Log out")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BPTrackerTheme {
        HomeScreen(email = "user@example.com", onLogout = {})
    }
}
