package com.example.diet.data.remote.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** OpenAI 兼容 chat/completions 请求 */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3,
    val response_format: ResponseFormat? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: JsonElement
)

@Serializable
data class ResponseFormat(val type: String)

/** OpenAI 兼容 chat/completions 响应 */
@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList(),
    val error: ApiError? = null
)

@Serializable
data class Choice(val message: RespMessage)

@Serializable
data class RespMessage(val content: String = "")

@Serializable
data class ApiError(
    val message: String = "",
    val type: String = "",
    val code: String = ""
)

/**
 * 结构化解析：餐食识别（模型只负责“看到什么”，不估算营养）
 */
@Serializable
data class MealIdentifyJson(
    val title: String = "",
    val items: List<IdentifyItemJson> = emptyList(),
    /** 图文不一致时的简要说明，无冲突留空 */
    val conflict: String = ""
)

@Serializable
data class IdentifyItemJson(
    val name: String = "",
    val brand: String = "",
    /** 可食用部分克数（饮品按 ml 近似为 g）；0 表示模型未给出，可由本地份量推导 */
    val grams: Float = 0f,
    /** 可数单位的份数：1 个鸡蛋、2 个包子、1 勺蛋白粉；无法按份表述时为 0 */
    val servings: Float = 0f,
    /** 是否为有包装的商品（决定是否需要按商品查营养） */
    val packaged: Boolean = false
)

/** 结构化解析：未命中本地成分表时的每 100g 补数 */
@Serializable
data class NutritionFillJson(
    val items: List<FillItemJson> = emptyList()
)

@Serializable
data class FillItemJson(
    val name: String = "",
    val kcalPer100g: Float = 0f,
    val proteinPer100g: Float = 0f,
    val carbPer100g: Float = 0f,
    val fatPer100g: Float = 0f,
    /** 数据来源/依据，如「同类食物典型值」；未知留空 */
    val source: String = "",
    /** 0~1，低于 0.3 视为不可用 */
    val confidence: Float = 0f
)

/** 结构化解析：食物推荐 */
@Serializable
data class FoodSuggestionListJson(
    val items: List<FoodSuggestionJson> = emptyList()
)

@Serializable
data class FoodSuggestionJson(
    val name: String = "",
    val caloriesKcal: Int = 0,
    val reason: String = ""
)
