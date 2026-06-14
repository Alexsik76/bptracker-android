package ua.vn.home.bptracker.feature.login

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ua.vn.home.bptracker.R

@Composable
fun LoginScreen(
    info: String? = null,
    signingIn: Boolean = false,
    onSignIn: (Activity) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium
        )
        if (signingIn) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
        } else {
            Button(onClick = { onSignIn(activity) }, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.auth_login_passkey))
            }
        }
        if (info != null) {
            Text(text = info, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
