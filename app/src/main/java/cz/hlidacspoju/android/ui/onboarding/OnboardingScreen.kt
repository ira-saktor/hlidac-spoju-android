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

/**
 * First-run wizard that walks the user through registering for a free Golemio API key and
 * getting it into the app.
 */
@Composable
fun OnboardingScreen(onFinished: (apiKey: String) -> Unit) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Vítejte v Hlídači spojů", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        Text(
            "Aby aplikace mohla sledovat zpoždění, potřebuje bezplatný API klíč od Golemio " +
                "(Pražská datová platforma)."
        )
        Button(onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://api.golemio.cz/api-keys"))
            )
        }) {
            Text("Otevřít registraci Golemio")
        }
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Golemio API klíč") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { onFinished(apiKey.trim()) }, enabled = apiKey.isNotBlank()) {
            Text("Dokončit")
        }
        TextButton(onClick = { onFinished("") }) {
            Text("Přeskočit")
        }
    }
}
