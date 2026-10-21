package com.example.diet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.diet.data.local.entity.WeightRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Insert
    suspend fun insert(record: WeightRecord): Long

    @Delete
    suspend fun delete(record: WeightRecord)

    @Query("SELECT * FROM weight_record WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<WeightRecord?>

    @Query("SELECT * FROM weight_record ORDER BY date ASC")
    fun observeAll(): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_record")
    suspend fun getAll(): List<WeightRecord>

    @Query("DELETE FROM weight_record")
    suspend fun clear()
}
