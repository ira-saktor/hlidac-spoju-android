package cz.hlidacspoju.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.hlidacspoju.android.BuildConfig
import cz.hlidacspoju.android.model.AppLanguage
import cz.hlidacspoju.android.model.AppTheme
import cz.hlidacspoju.android.ui.AppViewModel
import cz.hlidacspoju.android.ui.LocalStrings
import cz.hlidacspoju.android.ui.Strings

private fun languageDisplayName(language: AppLanguage): String = when (language) {
    AppLanguage.CZECH -> "Čeština"
    AppLanguage.ENGLISH -> "English"
    AppLanguage.RUSSIAN -> "Русский"
    AppLanguage.GERMAN -> "Deutsch"
    AppLanguage.FRENCH -> "Français"
}

private fun themeDisplayName(strings: Strings, theme: AppTheme): String = when (theme) {
    AppTheme.SYSTEM -> strings("theme_system")
    AppTheme.LIGHT -> strings("theme_light")
    AppTheme.DARK -> strings("theme_dark")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val strings = LocalStrings.current
    var apiKey by remember(settings.golemioApiKey) { mutableStateOf(settings.golemioApiKey) }
    var pollIntervalText by remember(settings.pollIntervalSeconds) {
        mutableStateOf(settings.pollIntervalSeconds.toString())
    }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings("settings")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = strings("back"))
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
                label = { Text(strings("golemio_api_key")) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pollIntervalText,
                onValueChange = { pollIntervalText = it },
                label = { Text(strings("poll_interval")) },
                modifier = Modifier.fillMaxWidth()
            )
            Column {
                Text(strings("language"))
                Row(modifier = Modifier.wrapContentWidth()) {
                    OutlinedButton(onClick = { languageMenuExpanded = true }) {
                        Text(languageDisplayName(settings.language))
                    }
                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false }
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(languageDisplayName(language)) },
                                onClick = {
                                    viewModel.updateLanguage(language)
                                    languageMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Column {
                Text(strings("theme"))
                Row(modifier = Modifier.wrapContentWidth()) {
                    OutlinedButton(onClick = { themeMenuExpanded = true }) {
                        Text(themeDisplayName(strings, settings.theme))
                    }
                    DropdownMenu(
                        expanded = themeMenuExpanded,
                        onDismissRequest = { themeMenuExpanded = false }
                    ) {
                        AppTheme.entries.forEach { theme ->
                            DropdownMenuItem(
                                text = { Text(themeDisplayName(strings, theme)) },
                                onClick = {
                                    viewModel.updateTheme(theme)
                                    themeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            // Logging toggle is only shown in debug builds; release/store builds never expose it
            // and always run with logging disabled (see AppContainer/NetworkModule defaults).
            if (BuildConfig.DEBUG) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings("logging_enabled"), modifier = Modifier.fillMaxWidth(0.8f))
                    Switch(
                        checked = settings.loggingEnabled,
                        onCheckedChange = { viewModel.updateLoggingEnabled(it) }
                    )
                }
            }
            Button(onClick = {
                viewModel.updateApiKey(apiKey.trim())
                pollIntervalText.toIntOrNull()?.let { viewModel.updatePollIntervalSeconds(it) }
                onBack()
            }) {
                Text(strings("save"))
            }
            Button(onClick = { viewModel.checkNow() }) {
                Text(strings("check_now"))
            }
        }
    }
}
