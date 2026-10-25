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
    val imageDataUri: Boolean = true,
    /**
     * 同一厂商下只有部分模型支持图片时的白名单（模型名包含任一关键字即视为支持）。
     * 为空表示该厂商所有模型都按 supportsVision 处理。
     */
    val visionModels: List<String> = emptyList()
) {
    /** 按实际使用的模型名判断是否支持图片输入 */
    fun supportsImage(model: String): Boolean {
        if (!supportsVision) return false
        if (visionModels.isEmpty()) return true
        val current = model.ifBlank { defaultModel }
        return visionModels.any { current.contains(it, ignoreCase = true) }
    }
}
