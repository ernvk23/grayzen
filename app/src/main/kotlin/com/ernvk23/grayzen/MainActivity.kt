package io.github.ernvk23.grayzen

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import io.github.ernvk23.grayzen.ui.GrayzenTheme
import io.github.ernvk23.grayzen.ui.ManagerScreen

class MainActivity : ComponentActivity() {
    private val controller by lazy { ManagerController.get(this) }
    private lateinit var viewModel: ManagerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0x00000000),
            navigationBarStyle = SystemBarStyle.dark(0xff12100e.toInt()),
        )
        viewModel = ViewModelProvider(this, ManagerViewModel.factory(application))[ManagerViewModel::class.java]
        setContent {
            GrayzenTheme {
                ManagerScreen(
                    viewModel = viewModel,
                    openAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    openUsageAccessSettings = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The configuration activity itself must always remain in color.
        controller.engine.onForeground(packageName, true)
        controller.publishState()
        viewModel.onResume()
    }
}
