package com.example.diet.data.repository

import com.example.diet.data.local.dao.DailyCalories
import com.example.diet.data.local.dao.MealDao
import com.example.diet.data.local.dao.MealItemDao
import com.example.diet.data.local.entity.MealItem
import com.example.diet.data.local.entity.MealRecord
import com.example.diet.domain.MealItems
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepository @Inject constructor(
    private val dao: MealDao,
    private val itemDao: MealItemDao
) {
    fun observeByDate(date: String): Flow<List<MealRecord>> = dao.observeByDate(date)

    fun observeCaloriesByDate(date: String): Flow<Int> = dao.observeCaloriesByDate(date)

    fun observeDailyCaloriesBetween(start: String, end: String): Flow<List<DailyCalories>> =
        dao.observeDailyCaloriesBetween(start, end)

    /** 某条记录的食物明细（按录入顺序） */
    fun observeItems(recordId: Long): Flow<List<MealItem>> = itemDao.observeByRecord(recordId)

    suspend fun getItems(recordId: Long): List<MealItem> = itemDao.getByRecord(recordId)

    suspend fun addMeal(record: MealRecord): Long = dao.insert(record)

    /** 分析结果落库后写入条目明细，并同步记录头的总量 */
    suspend fun addItems(recordId: Long, items: List<MealItem>) {
        if (items.isEmpty()) return
        itemDao.insertAll(items.map { it.copy(recordId = recordId) })
    }

    suspend fun updateItem(item: MealItem) {
        itemDao.update(item)
        syncTotals(item.recordId)
    }

    /** 批量替换明细（重新分析后使用） */
    suspend fun replaceItems(recordId: Long, items: List<MealItem>) {
        itemDao.deleteByRecord(recordId)
        itemDao.insertAll(items.map { it.copy(recordId = recordId) })
        syncTotals(recordId)
    }

    /** 批量更新条目（补全营养后使用），只重算一次合计 */
    suspend fun updateItems(items: List<MealItem>) {
        if (items.isEmpty()) return
        items.forEach { itemDao.update(it) }
        syncTotals(items.first().recordId)
    }

    suspend fun deleteItem(item: MealItem) {
        itemDao.delete(item)
        syncTotals(item.recordId)
    }

    suspend fun deleteMeal(record: MealRecord) {
        itemDao.deleteByRecord(record.id)
        dao.delete(record)
    }

    suspend fun updateMeal(record: MealRecord) = dao.update(record)

    /**
     * 明细是真源，记录头（四项数值 + 状态）只是缓存：
     * 任何条目变更后都在这里重算，避免两处数字打架。
     */
    suspend fun syncTotals(recordId: Long) {
        val record = dao.getById(recordId) ?: return
        val items = itemDao.getByRecord(recordId)
        val totals = MealItems.totals(items)
        dao.update(
            record.copy(
                caloriesKcal = totals.kcal,
                proteinG = totals.proteinG,
                carbG = totals.carbG,
                fatG = totals.fatG,
                status = MealItems.status(items)
            )
        )
    }

    /** 备份用 */
    suspend fun getAllItems(): List<MealItem> = itemDao.getAll()

    suspend fun clearItems() = itemDao.clear()
}
