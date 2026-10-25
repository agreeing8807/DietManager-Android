package com.example.diet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.diet.data.local.entity.DrinkPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkPresetDao {
    @Insert
    suspend fun insert(preset: DrinkPreset): Long

    @Update
    suspend fun update(preset: DrinkPreset)

    @Delete
    suspend fun delete(preset: DrinkPreset)

    @Query("SELECT * FROM drink_preset ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<DrinkPreset>>

    @Query("SELECT COUNT(*) FROM drink_preset")
    suspend fun count(): Int

    @Query("SELECT * FROM drink_preset")
    suspend fun getAll(): List<DrinkPreset>

    @Query("DELETE FROM drink_preset")
    suspend fun clear()
}
