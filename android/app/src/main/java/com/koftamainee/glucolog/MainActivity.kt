package com.koftamainee.glucolog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.koftamainee.glucolog.data.ThemeMode
import com.koftamainee.glucolog.data.xdrip.XdripMonitorService
import com.koftamainee.glucolog.ui.AppNavHost
import com.koftamainee.glucolog.ui.theme.GlucologTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GlucologApp).container
        startXdripMonitor()
        setContent {
            val themeMode by container.settingsDataStore.themeMode.collectAsState(
                initial = ThemeMode.SYSTEM
            )
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            GlucologTheme(darkTheme = darkTheme) {
                AppNavHost(container = container)
            }
        }
    }

    private fun startXdripMonitor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        XdripMonitorService.start(this)
    }
}

@Preview(showBackground = true)
@Composable
fun GlucologPreview() {
    GlucologTheme {
        androidx.compose.material3.Text("Glucolog")
    }
}
