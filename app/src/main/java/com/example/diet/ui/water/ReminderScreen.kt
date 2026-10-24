package com.example.diet.ui.water

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.diet.data.local.entity.ReminderSetting
import com.example.diet.util.DateUtils
import com.example.diet.util.ReminderScheduler
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(navController: NavHostController, viewModel: ReminderViewModel = hiltViewModel()) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val allEnabled = reminders.isNotEmpty() && reminders.all { it.enabled }
    var exactPreferred by remember { mutableStateOf(ReminderScheduler.isExactAlarmPreferred(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("喝水提醒") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            val alarm = context.getSystemService(AlarmManager::class.java)
            val needsExactPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()
            val notificationsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!notificationsEnabled || (exactPreferred && needsExactPermission)) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!notificationsEnabled) {
                            Text("未开启通知权限，提醒将无法弹出。", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                })
                            }) { Text("去开启通知") }
                        }
                        if (exactPreferred && needsExactPermission) {
                            Text("未授予“精确闹钟”权限，提醒可能误差几分钟（已降级为非精确闹钟，仍会触发）。", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                })
                            }) { Text("去开启精确闹钟") }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("启用全部提醒", style = MaterialTheme.typography.titleMedium)
                Switch(checked = allEnabled, onCheckedChange = { viewModel.toggleAll(it) })
            }
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("使用精确闹钟", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "更准时（需授予精确闹钟权限）；关闭则可能误差几分钟但无需授权",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = exactPreferred,
                    onCheckedChange = { checked ->
                        exactPreferred = checked
                        ReminderScheduler.setExactAlarmPreferred(context, checked)
                        if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            })
                        }
                        viewModel.reschedule()
                    }
                )
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reminders, key = { it.id }) { setting ->
                    ReminderRow(setting = setting, onToggle = { viewModel.toggle(setting) }, onDelete = { viewModel.delete(setting) })
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = {
                val cal = Calendar.getInstance()
                TimePickerDialog(
                    context,
                    { _, h, m -> viewModel.addReminder(h * 60 + m) },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            }) {
                Text("+ 添加提醒时间")
            }
        }
    }
}

@Composable
private fun ReminderRow(setting: ReminderSetting, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(DateUtils.minuteOfDayToString(setting.timeMinutesOfDay), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = setting.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Color(0xFFE53935))
            }
        }
    }
}
