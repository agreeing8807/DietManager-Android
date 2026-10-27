package com.example.diet.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.diet.util.NotificationUtils
import com.example.diet.util.WaterReminderTexts

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == com.example.diet.util.ReminderScheduler.ACTION) {
            NotificationUtils.showReminder(context, "该喝水啦 💧", WaterReminderTexts.pick(context))
            // 触发重新排程，保证次日继续提醒
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<com.example.diet.worker.ReminderWorker>().build())
        }
    }
}
