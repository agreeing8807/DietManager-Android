package com.example.diet.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.diet.data.repository.ReminderRepository
import com.example.diet.util.ReminderScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 重新排程所有启用的喝水提醒（开机 / 闹钟触发后调用）。
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = reminderRepository.getEnabled()
        ReminderScheduler.scheduleAll(applicationContext, settings)
        return Result.success()
    }
}
