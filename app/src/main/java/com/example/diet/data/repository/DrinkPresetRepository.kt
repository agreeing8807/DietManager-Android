package com.example.diet.data.repository

import com.example.diet.data.local.dao.DrinkPresetDao
import com.example.diet.data.local.entity.DEFAULT_DRINK_PRESETS
import com.example.diet.data.local.entity.DrinkPreset
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrinkPresetRepository @Inject constructor(
    private val dao: DrinkPresetDao
) {
    fun observeAll(): Flow<List<DrinkPreset>> = dao.observeAll()

    /** 首次使用（或数据被清空）时写入内置水种 */
    suspend fun ensureDefaults() {
        if (dao.count() > 0) return
        DEFAULT_DRINK_PRESETS.forEach { dao.insert(it) }
    }

    suspend fun add(preset: DrinkPreset): Long = dao.insert(preset)

    suspend fun delete(preset: DrinkPreset) = dao.delete(preset)
}
