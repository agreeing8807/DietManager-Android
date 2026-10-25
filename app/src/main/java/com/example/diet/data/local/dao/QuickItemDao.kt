package com.example.diet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.diet.data.local.entity.QuickItem
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickItemDao {
    @Insert
    suspend fun insert(item: QuickItem): Long

    @Update
    suspend fun update(item: QuickItem)

    @Delete
    suspend fun delete(item: QuickItem)

    @Query("SELECT * FROM quick_item ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<QuickItem>>

    @Query("SELECT * FROM quick_item")
    suspend fun getAll(): List<QuickItem>

    @Query("DELETE FROM quick_item")
    suspend fun clear()
}
