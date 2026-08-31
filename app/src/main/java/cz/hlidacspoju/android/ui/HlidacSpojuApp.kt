package cz.hlidacspoju.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cz.hlidacspoju.android.model.AppTheme
import cz.hlidacspoju.android.service.AppContainer
import cz.hlidacspoju.android.ui.main.MainScreen
import cz.hlidacspoju.android.ui.onboarding.OnboardingScreen
import cz.hlidacspoju.android.ui.settings.SettingsScreen

private object Routes {
    const val MAIN = "main"
    const val ONBOARDING = "onboarding"
    const val SETTINGS = "settings"
}

@Composable
fun HlidacSpojuApp(container: AppContainer) {
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(container))
    val settings by viewModel.settings.collectAsState()
    val isDark = when (settings.theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier) {
            val navController = rememberNavController()
            val strings = remember(settings.language) { Strings(settings.language) }

            CompositionLocalProvider(LocalStrings provides strings) {
            NavHost(
                navController = navController,
                startDestination = if (settings.onboardingCompleted) Routes.MAIN else Routes.ONBOARDING
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onFinished = { apiKey ->
                            viewModel.completeOnboarding(apiKey)
                            navController.navigate(Routes.MAIN) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.MAIN) {
                    MainScreen(
                        viewModel = viewModel,
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            }
        }
    }
}
