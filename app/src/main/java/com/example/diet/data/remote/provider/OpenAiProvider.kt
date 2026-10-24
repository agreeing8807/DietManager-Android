package com.example.diet.data.remote.provider

import com.example.diet.data.remote.ApiKeyStore
import com.example.diet.data.remote.LLMProvider
import com.example.diet.data.remote.OpenAiCompatibleService
import com.example.diet.data.remote.model.ChatCompletionRequest
import com.example.diet.data.remote.model.ChatMessage
import com.example.diet.data.remote.model.FoodSuggestionJson
import com.example.diet.data.remote.model.FoodSuggestionListJson
import com.example.diet.data.remote.model.MealAnalysisJson
import com.example.diet.data.remote.model.ResponseFormat
import com.example.diet.domain.model.FoodSuggestion
import com.example.diet.domain.model.FoodSuggestionRequest
import com.example.diet.domain.model.MealAnalysis
import com.example.diet.domain.model.MealAnalysisRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.util.Base64

/**
 * 单一 OpenAI 兼容实现，通过 VendorConfig 适配不同国内厂商。
 */
class OpenAiProvider(
    override val id: String,
    override val displayName: String,
    private val config: VendorConfig,
    private val service: OpenAiCompatibleService,
    private val apiKeyStore: ApiKeyStore
) : LLMProvider {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun analyzePrompt(): String =
        apiKeyStore.getSetting("prompt_analyze", DEFAULT_ANALYZE).ifBlank { DEFAULT_ANALYZE }

    private fun suggestPrompt(): String =
        apiKeyStore.getSetting("prompt_suggest", DEFAULT_SUGGEST).ifBlank { DEFAULT_SUGGEST }

    override suspend fun analyzeMeal(req: MealAnalysisRequest): Result<MealAnalysis> {
        val key = apiKeyStore.getApiKey(id) ?: return Result.failure(
            IllegalStateException("未配置 ${config.name} 的 API Key")
        )
        val userContent = buildUserContent(req)
        val request = ChatCompletionRequest(
            model = config.defaultModel,
            messages = listOf(
                ChatMessage("system", JsonPrimitive(analyzePrompt())),
                ChatMessage("user", userContent)
            ),
            temperature = 0.3,
            response_format = ResponseFormat("json_object")
        )
        return runCatching {
            val resp = service.chat("${config.authPrefix} $key", request)
            val raw = resp.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("模型未返回内容")
            val parsed = json.decodeFromString<MealAnalysisJson>(stripFences(raw))
            MealAnalysis(
                caloriesKcal = parsed.caloriesKcal,
                proteinG = parsed.proteinG,
                carbG = parsed.carbG,
                fatG = parsed.fatG,
                summary = parsed.summary
            )
        }
    }

    override suspend fun suggestFoods(req: FoodSuggestionRequest): Result<List<FoodSuggestion>> {
        val key = apiKeyStore.getApiKey(id) ?: return Result.failure(
            IllegalStateException("未配置 ${config.name} 的 API Key")
        )
        val userText = buildString {
            append("今日剩余可用热量约 ${req.remainingCalories} kcal。")
            if (req.context.isNotBlank()) append("\n用户背景：${req.context}")
        }
        val request = ChatCompletionRequest(
            model = config.defaultModel,
            messages = listOf(
                ChatMessage("system", JsonPrimitive(suggestPrompt())),
                ChatMessage("user", JsonPrimitive(userText))
            ),
            temperature = 0.6,
            response_format = ResponseFormat("json_object")
        )
        return runCatching {
            val resp = service.chat("${config.authPrefix} $key", request)
            val raw = resp.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("模型未返回内容")
            val parsed = json.decodeFromString<FoodSuggestionListJson>(stripFences(raw))
            parsed.items.map {
                FoodSuggestion(name = it.name, caloriesKcal = it.caloriesKcal, reason = it.reason)
            }
        }
    }

    private fun buildUserContent(req: MealAnalysisRequest): JsonElement {
        return if (req.imageBytes != null && config.supportsVision) {
            val b64 = Base64.getEncoder().encodeToString(req.imageBytes)
            buildJsonArray {
                add(buildJsonObject {
                    put("type", JsonPrimitive("text"))
                    put(
                        "text",
                        JsonPrimitive(
                            "请分析这餐食物的营养。${
                                if (req.description.isNotBlank()) "补充说明：${req.description}" else ""
                            }${if (req.userContext.isNotBlank()) "\n用户目标：${req.userContext}" else ""}"
                        )
                    )
                })
                add(buildJsonObject {
                    put("type", JsonPrimitive("image_url"))
                    put(
                        "image_url",
                        buildJsonObject {
                            put("url", JsonPrimitive("data:image/jpeg;base64,$b64"))
                        }
                    )
                })
            }
        } else {
            JsonPrimitive(
                buildString {
                    append("请分析以下餐食：")
                    append(req.description)
                    if (req.userContext.isNotBlank()) append("\n用户目标：${req.userContext}")
                }
            )
        }
    }

    private fun stripFences(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("```")) {
            trimmed.removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
        } else trimmed
    }

    companion object {
        val DEFAULT_ANALYZE = """
            你是一名专业的营养师。请根据用户提供的餐食信息，估算该餐的营养成分。
            仅返回一个 JSON 对象，不要包含任何解释或 markdown 代码块，格式如下：
            {"caloriesKcal":整数,"proteinG":浮点数,"carbG":浮点数,"fatG":浮点数,"summary":"一句话点评与建议"}
            若信息不足，请基于常见分量合理估算并在 summary 中说明。
        """.trimIndent()

        val DEFAULT_SUGGEST = """
            你是一名专业的营养师。请根据用户当日的剩余热量额度与饮食目标，推荐合适的食物。
            仅返回一个 JSON 对象，不要包含任何解释或 markdown 代码块，格式如下：
            {"items":[{"name":"食物名","caloriesKcal":整数,"reason":"推荐理由"}]}
            推荐 4-6 项，优先低热量高营养的密度食物。
        """.trimIndent()
    }
}
