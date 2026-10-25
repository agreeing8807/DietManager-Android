package com.example.diet.data

import android.content.Context
import com.example.diet.data.local.dao.DrinkPresetDao
import com.example.diet.data.local.dao.MealDao
import com.example.diet.data.local.dao.MealItemDao
import com.example.diet.data.local.dao.QuickItemDao
import com.example.diet.data.local.dao.ReminderDao
import com.example.diet.data.local.dao.UserProfileDao
import com.example.diet.data.local.dao.WaterDao
import com.example.diet.data.local.dao.WeightDao
import com.example.diet.data.local.entity.DrinkPreset
import com.example.diet.data.local.entity.MealItem
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.data.local.entity.QuickItem
import com.example.diet.data.local.entity.ReminderSetting
import com.example.diet.data.local.entity.UserProfile
import com.example.diet.data.local.entity.WaterRecord
import com.example.diet.data.local.entity.WeightRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupData(
    val profile: UserProfile,
    val weights: List<WeightRecord>,
    val meals: List<MealRecord>,
    val waters: List<WaterRecord>,
    val reminders: List<ReminderSetting>,
    val quickItems: List<QuickItem> = emptyList(),
    val drinkPresets: List<DrinkPreset> = emptyList(),
    /** 餐食条目明细（旧备份没有该字段，导入后为历史记录） */
    val mealItems: List<MealItem> = emptyList(),
    val exportedAt: Long
)

/**
 * 本地数据导出/导入备份。文件保存在应用私有目录，无需存储权限。
 */
@Singleton
class BackupManager @Inject constructor(
    private val profileDao: UserProfileDao,
    private val weightDao: WeightDao,
    private val mealDao: MealDao,
    private val waterDao: WaterDao,
    private val reminderDao: ReminderDao,
    private val quickItemDao: QuickItemDao,
    private val drinkPresetDao: DrinkPresetDao,
    private val mealItemDao: MealItemDao,
    @ApplicationContext context: Context
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val dir = File(context.filesDir, "backups").apply { mkdirs() }

    suspend fun export(): String {
        val profile = profileDao.get() ?: UserProfile()
        val data = BackupData(
            profile = profile,
            weights = weightDao.getAll(),
            meals = mealDao.getAll(),
            waters = waterDao.getAll(),
            reminders = reminderDao.getAll(),
            quickItems = quickItemDao.getAll(),
            drinkPresets = drinkPresetDao.getAll(),
            mealItems = mealItemDao.getAll(),
            exportedAt = System.currentTimeMillis()
        )
        val name = "diet_backup_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".json"
        val file = File(dir, name)
        file.writeText(json.encodeToString(data))
        return file.absolutePath
    }

    suspend fun importLatest(): Result<String> {
        val file = dir.listFiles()
            ?.filter { it.extension == "json" }
            ?.maxByOrNull { it.lastModified() }
            ?: return Result.failure(IllegalStateException("未找到备份文件"))
        return runCatching {
            val data = json.decodeFromString<BackupData>(file.readText())
            profileDao.clear(); profileDao.upsert(data.profile)
            weightDao.clear(); data.weights.forEach { weightDao.insert(it.copy(id = 0)) }
            // 餐食重新插入后主键会变，明细需要按新旧 id 映射后再写回
            val mealIdMap = mutableMapOf<Long, Long>()
            mealDao.clear()
            data.meals.forEach { old ->
                mealIdMap[old.id] = mealDao.insert(old.copy(id = 0))
            }
            mealItemDao.clear()
            data.mealItems.forEach { item ->
                mealIdMap[item.recordId]?.let { newRecordId ->
                    mealItemDao.insert(item.copy(id = 0, recordId = newRecordId))
                }
            }
            waterDao.clear(); data.waters.forEach { waterDao.insert(it.copy(id = 0)) }
            reminderDao.clear(); data.reminders.forEach { reminderDao.insert(it.copy(id = 0)) }
            quickItemDao.clear(); data.quickItems.forEach { quickItemDao.insert(it.copy(id = 0)) }
            drinkPresetDao.clear(); data.drinkPresets.forEach { drinkPresetDao.insert(it.copy(id = 0)) }
            file.name
        }
    }
}
