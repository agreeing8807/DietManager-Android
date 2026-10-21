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
    val caloriesKcal: Int = 0,
    val proteinG: Float = 0f,
    val carbG: Float = 0f,
    val fatG: Float = 0f,
    val summary: String = ""
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
