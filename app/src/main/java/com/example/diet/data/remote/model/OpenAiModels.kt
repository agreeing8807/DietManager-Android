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

/** 结构化解析：单餐分析 */
@Serializable
data class MealAnalysisJson(
    val title: String = "",
    val caloriesKcal: Int = 0,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f,
    /** 本次食用份量说明，如「1 包 50g」 */
    val basis: String = "",
    /** 每 100g 对照值（包装食品） */
    val per100g: Per100gJson? = null,
    /** 数据来源（品牌官网营养标签、食品数据库等） */
    val source: String = "",
    /** searched / estimated / missing */
    val dataQuality: String = "",
    val summary: String = ""
)

/** 每 100g 营养对照 */
@Serializable
data class Per100gJson(
    val caloriesKcal: Int = 0,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f
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
