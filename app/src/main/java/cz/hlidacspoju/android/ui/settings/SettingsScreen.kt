package cz.hlidacspoju.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.hlidacspoju.android.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    var apiKey by remember(settings.golemioApiKey) { mutableStateOf(settings.golemioApiKey) }
    var pollIntervalText by remember(settings.pollIntervalSeconds) {
        mutableStateOf(settings.pollIntervalSeconds.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nastavení") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Golemio API klíč") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pollIntervalText,
                onValueChange = { pollIntervalText = it },
                label = { Text("Kontrola každých (sekund)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                viewModel.updateApiKey(apiKey.trim())
                pollIntervalText.toIntOrNull()?.let { viewModel.updatePollIntervalSeconds(it) }
                onBack()
            }) {
                Text("Uložit")
            }
            Button(onClick = { viewModel.checkNow() }) {
                Text("Zkontrolovat nyní")
            }
        }
    }
}
