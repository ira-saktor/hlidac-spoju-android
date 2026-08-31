package cz.hlidacspoju.android.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cz.hlidacspoju.android.ui.LocalStrings

/**
 * First-run wizard that walks the user through registering for a free Golemio API key and
 * getting it into the app.
 */
@Composable
fun OnboardingScreen(onFinished: (apiKey: String) -> Unit) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf("") }
    val strings = LocalStrings.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(strings("welcome_title"), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        Text(strings("welcome_body"))
        Button(onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://api.golemio.cz/api-keys"))
            )
        }) {
            Text(strings("open_golemio_registration"))
        }
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(strings("golemio_api_key")) },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { onFinished(apiKey.trim()) }, enabled = apiKey.isNotBlank()) {
            Text(strings("finish"))
        }
        TextButton(onClick = { onFinished("") }) {
            Text(strings("skip"))
        }
    }
}
