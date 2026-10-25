package com.example.diet.data.repository

import com.example.diet.data.local.dao.QuickItemDao
import com.example.diet.data.local.entity.QuickItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickItemRepository @Inject constructor(
    private val dao: QuickItemDao
) {
    fun observeAll(): Flow<List<QuickItem>> = dao.observeAll()

    suspend fun add(item: QuickItem): Long = dao.insert(item)

    suspend fun update(item: QuickItem) = dao.update(item)

    suspend fun delete(item: QuickItem) = dao.delete(item)
}
