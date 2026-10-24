package com.example.diet.data.remote

import kotlinx.serialization.Serializable

/** 一条已添加的大模型 API 配置：厂商 + 模型 + 密钥（密钥仅本地加密存储） */
@Serializable
data class ApiEntry(
    val id: String,
    val vendorId: String,
    val model: String,
    val apiKey: String
)
