package com.example.diet.util

import android.content.Context

/**
 * 喝水提示标语：内置 8 条，用户可增删改，存于 SharedPreferences。
 * 通知正文与写入系统日历的事件标题都取自这里，保证两处措辞一致。
 */
object WaterReminderTexts {

    private const val PREFS = "water_reminder_texts"
    private const val KEY_TEXTS = "texts"
    private const val SEPARATOR = "\n"

    /** 单条标语长度上限，避免通知被截断成一长串 */
    private const val MAX_LENGTH = 60

    /** 标语被全部删除后使用的兜底文案，不进入列表、不显示在标语管理里 */
    const val FALLBACK_TEXT = "提醒喝水"

    /** 内置标语，恢复默认时使用 */
    val DEFAULTS: List<String> = listOf(
        "该喝水啦，来一杯温水吧",
        "补水时间到，别让身体等太久",
        "喝水提醒：小口慢饮，一次约 200 ml",
        "久坐了？起身接杯水，顺便活动一下",
        "现在喝水，能帮大脑保持清醒",
        "喝水是最便宜的提神方式，来一杯",
        "别忘了喝水，今天的饮水目标还差一点",
        "喝完这杯，接下来的状态会更好"
    )

    /**
     * 当前生效的标语。
     * 从未自定义过（本地无记录）时返回内置 8 条；用户主动删空后返回空列表，
     * 此时 [pick] 与日历会使用 [FALLBACK_TEXT]，但列表里不展示这句话。
     */
    fun current(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_TEXTS, null) ?: return DEFAULTS
        return raw.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** 保存标语列表；空白项自动丢弃，传空列表即表示"不使用自定义标语" */
    fun save(context: Context, texts: List<String>) {
        val cleaned = texts
            .map { it.replace(SEPARATOR, " ").trim() }
            .filter { it.isNotEmpty() }
            .map { it.take(MAX_LENGTH) }
        prefs(context).edit().putString(KEY_TEXTS, cleaned.joinToString(SEPARATOR)).apply()
    }

    /** 恢复内置 8 条 */
    fun reset(context: Context) {
        prefs(context).edit().remove(KEY_TEXTS).apply()
    }

    /** 随机取一条，供通知使用；标语被删空时返回 [FALLBACK_TEXT] */
    fun pick(context: Context): String = current(context).randomOrNull() ?: FALLBACK_TEXT

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
