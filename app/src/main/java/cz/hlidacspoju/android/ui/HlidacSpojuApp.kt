package cz.hlidacspoju.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    MaterialTheme {
        Surface(modifier = Modifier) {
            val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(container))
            val settings by viewModel.settings.collectAsState()
            val navController = rememberNavController()

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
