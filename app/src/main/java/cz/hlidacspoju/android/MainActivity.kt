package cz.hlidacspoju.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import cz.hlidacspoju.android.monitoring.MonitoringForegroundService
import cz.hlidacspoju.android.service.AppContainer
import cz.hlidacspoju.android.ui.HlidacSpojuApp

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { startMonitoringService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer.getInstance(applicationContext)

        ensureNotificationPermissionThenStartService()

        setContent {
            HlidacSpojuApp(container = container)
        }
    }

    private fun ensureNotificationPermissionThenStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                startMonitoringService()
            } else {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startMonitoringService()
        }
    }

    private fun startMonitoringService() {
        ContextCompat.startForegroundService(this, Intent(this, MonitoringForegroundService::class.java))
    }
}
