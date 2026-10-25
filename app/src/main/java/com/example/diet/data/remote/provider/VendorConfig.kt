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
    val authPrefix: String = "Bearer",
    /** 图片是否使用 data:image/jpeg;base64,xxx 形式（智谱等厂商只接受纯 base64） */
    val imageDataUri: Boolean = true
)
