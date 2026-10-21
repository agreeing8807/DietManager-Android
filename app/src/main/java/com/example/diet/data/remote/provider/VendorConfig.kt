package com.example.diet.data.remote.provider

/**
 * 厂商配置：同一种 OpenAI 兼容协议，仅 baseUrl / 模型 / 是否支持视觉不同。
 * 新增厂商只需在此追加一项。
 */
data class VendorConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val supportsVision: Boolean = false,
    val authPrefix: String = "Bearer"
)
