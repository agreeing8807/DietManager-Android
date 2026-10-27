package com.example.diet.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.diet.R

object NotificationUtils {
    const val CHANNEL_ID = "water_reminder_channel"
    private const val CHANNEL_NAME = "喝水提醒"

    /** 与应用主色一致，通知栏按该颜色着色 */
    private const val ACCENT_COLOR = 0xFF0288D1.toInt()

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "定时提醒补充水分"
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showReminder(context: Context, title: String, text: String) {
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // 状态栏小图标必须单色，使用与应用图标同源的「水滴」矢量
            .setSmallIcon(R.drawable.ic_stat_water)
            .setColor(ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        // 展开通知时显示应用桌面图标，保证与应用图标一致
        appIcon(context)?.let { builder.setLargeIcon(it) }
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    /** 直接取 launcher 自适应图标绘制成位图，避免另存一份图片资源后再次不同步 */
    private fun appIcon(context: Context): Bitmap? = runCatching {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return null
        val size = drawable.intrinsicWidth.takeIf { it > 0 } ?: 108
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
        }
    }.getOrNull()
}
