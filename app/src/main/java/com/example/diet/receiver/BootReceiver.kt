package com.example.diet.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.diet.util.NotificationUtils
import com.example.diet.worker.ReminderWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: android.content.Intent?) {
        if (intent?.action == android.content.Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == android.content.Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            NotificationUtils.ensureChannel(context)
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().build())
        }
    }
}
