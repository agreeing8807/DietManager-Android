package com.example.diet

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.diet.data.repository.ReminderRepository
import com.example.diet.ui.navigation.DietNavGraph
import com.example.diet.ui.theme.DietManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 用户拒绝时提醒仍可能不显示，属可接受的降级 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // 应用启动即写入默认提醒并排程，避免“从未打开提醒页就不提醒”
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { reminderRepository.ensureScheduled() }
        }
        setContent {
            DietManagerApp()
        }
    }
}

@Composable
fun DietManagerApp() {
    var dynamicColor by remember { mutableStateOf(true) }
    val darkTheme = isSystemInDarkTheme()
    DietManagerTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
        DietNavGraph()
    }
}

@Preview(showBackground = true)
@Composable
fun DietManagerAppPreview() {
    DietManagerApp()
}
