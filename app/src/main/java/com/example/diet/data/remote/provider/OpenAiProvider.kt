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
    private val apiKey: String,
    private val model: String,
    private val apiKeyStore: ApiKeyStore
) : LLMProvider {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * 老版本自定义提示语里缺少的数据字段（title / source / dataQuality 等）自动补齐，
     * 保证拍照标题与“可溯源、缺失不臆测”的规则对旧配置同样生效。
     */
    private fun analyzePrompt(): String {
        val base = apiKeyStore.getSetting("prompt_analyze", DEFAULT_ANALYZE).ifBlank { DEFAULT_ANALYZE }
        if (base.contains("\"dataQuality\"")) return base
        return base + "\n" + LEGACY_EXTRA_FIELDS
    }

    private fun suggestPrompt(): String =
        apiKeyStore.getSetting("prompt_suggest", DEFAULT_SUGGEST).ifBlank { DEFAULT_SUGGEST }

    override suspend fun analyzeMeal(req: MealAnalysisRequest): Result<MealAnalysis> {
        if (apiKey.isBlank()) return Result.failure(
            IllegalStateException("未配置 ${config.name} 的 API Key")
        )
        val imageIgnored = req.imageBytes != null && !config.supportsImage(model)
        if (imageIgnored && req.description.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "当前模型（${config.name} / ${model.ifBlank { config.defaultModel }}）不支持图片识别，" +
                        "请改用 deepseek-flash、通义千问 qwen-vl-plus、智谱 glm-4v 等视觉模型，或补充文字描述"
                )
            )
        }
        if (req.imageBytes == null && req.description.isBlank()) {
            return Result.failure(IllegalStateException("请描述这餐吃了什么，或使用拍照识别"))
        }
        val userContent = buildUserContent(req)
        val request = ChatCompletionRequest(
            model = model.ifBlank { config.defaultModel },
            messages = listOf(
                ChatMessage("system", JsonPrimitive(analyzePrompt())),
                ChatMessage("user", userContent)
            ),
            temperature = 0.3,
            response_format = ResponseFormat("json_object")
        )
        return runCatching {
            val resp = service.chat("${config.authPrefix} $apiKey", request)
            resp.error?.let { throw IllegalStateException(it.message.ifBlank { "接口返回错误" }) }
            val raw = resp.choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("模型未返回内容（请确认所选模型支持图片识别，或改用文字描述）")
            val parsed = json.decodeFromString<MealAnalysisJson>(stripFences(raw))
            // 模型标注数据缺失时，强制清零，避免任何臆测数值进入统计
            val missing = parsed.dataQuality.equals("missing", ignoreCase = true)
            MealAnalysis(
                caloriesKcal = if (missing) 0 else parsed.caloriesKcal.coerceAtLeast(0),
                proteinG = if (missing) 0f else parsed.proteinG.coerceAtLeast(0f),
                carbG = if (missing) 0f else parsed.carbG.coerceAtLeast(0f),
                fatG = if (missing) 0f else parsed.fatG.coerceAtLeast(0f),
                summary = buildAnalysisSummary(parsed, missing, imageIgnored),
                title = sanitizeTitle(parsed.title)
            )
        }
    }

    /**
     * 标题净化（与提示词规范对应）：
     * 去引号/emoji/句末标点、合并空白、剔除占位词、截断到 14 字；不合格返回空串由调用方兜底。
     */
    private fun sanitizeTitle(raw: String): String {
        var title = raw.trim()
            .trim('"', '\'', '“', '”', '‘', '’', '「', '」', '『', '』', '《', '》')
            .replace(Regex("[\\p{So}\\p{Cs}\\p{Cn}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('。', '，', ',', '.', '；', ';', '、', '…')
            .trim()
        val placeholders = setOf("图片", "照片", "一餐", "餐食", "食物", "美食", "外卖", "未知")
        if (title in placeholders) title = ""
        return title.take(14)
    }

    /**
     * 把「份量 / 每 100g 对照 / 数据来源」合并进一条可持久化的 summary：
     * MealRecord 只存 summary，这样重装或重启后溯源信息仍在。
     */
    private fun buildAnalysisSummary(
        parsed: MealAnalysisJson,
        missing: Boolean,
        imageIgnored: Boolean
    ): String {
        val lines = mutableListOf<String>()
        if (missing) {
            lines += "⚠ 未找到可靠的营养成分数据，本次不给出数值，避免臆测"
        } else if (parsed.summary.isNotBlank()) {
            lines += parsed.summary.trim()
        }

        if (!missing) {
            val basis = parsed.basis.trim()
            val per100 = parsed.per100g?.let {
                "每 100g：${it.caloriesKcal} kcal / 蛋白 ${format1(it.proteinG)} g / " +
                    "碳水 ${format1(it.carbG)} g / 脂肪 ${format1(it.fatG)} g"
            }.orEmpty()
            when {
                basis.isNotBlank() && per100.isNotBlank() -> lines += "份量：$basis；$per100"
                basis.isNotBlank() -> lines += "份量：$basis"
                per100.isNotBlank() -> lines += per100
            }
        }

        if (parsed.source.isNotBlank()) lines += "来源：${parsed.source.trim()}"
        if (imageIgnored) lines += "（当前模型不支持图片，已按文字描述估算）"
        return lines.joinToString("\n")
    }

    private fun format1(value: Float): String =
        String.format(java.util.Locale.CHINA, "%.1f", value)

    override suspend fun suggestFoods(req: FoodSuggestionRequest): Result<List<FoodSuggestion>> {
        if (apiKey.isBlank()) return Result.failure(
            IllegalStateException("未配置 ${config.name} 的 API Key")
        )
        val userText = buildString {
            append("今日剩余可用热量约 ${req.remainingCalories} kcal。")
            if (req.context.isNotBlank()) append("\n用户背景：${req.context}")
        }
        val request = ChatCompletionRequest(
            model = model.ifBlank { config.defaultModel },
            messages = listOf(
                ChatMessage("system", JsonPrimitive(suggestPrompt())),
                ChatMessage("user", JsonPrimitive(userText))
            ),
            temperature = 0.6,
            response_format = ResponseFormat("json_object")
        )
        return runCatching {
            val resp = service.chat("${config.authPrefix} $apiKey", request)
            resp.error?.let { throw IllegalStateException(it.message.ifBlank { "接口返回错误" }) }
            val raw = resp.choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("模型未返回内容")
            val parsed = json.decodeFromString<FoodSuggestionListJson>(stripFences(raw))
            parsed.items.map {
                FoodSuggestion(name = it.name, caloriesKcal = it.caloriesKcal, reason = it.reason)
            }
        }
    }

    private fun buildUserContent(req: MealAnalysisRequest): JsonElement {
        return if (req.imageBytes != null && config.supportsImage(model)) {
            val b64 = Base64.getEncoder().encodeToString(req.imageBytes)
            val imageUrl = if (config.imageDataUri) "data:image/jpeg;base64,$b64" else b64
            buildJsonArray {
                add(buildJsonObject {
                    put("type", JsonPrimitive("text"))
                    put(
                        "text",
                        JsonPrimitive(
                            buildString {
                                append("请根据这张照片估算这餐食物的热量与营养成分，按常见分量合理估算。")
                                if (req.description.isNotBlank()) {
                                    append("补充说明：${req.description}")
                                } else {
                                    append("用户未填写文字描述，照片即全部信息，请直接按图片判断。")
                                }
                                append("\n若为包装食品，请先联网查证包装上的营养成分表再换算。")
                                if (req.userContext.isNotBlank()) append("\n用户目标：${req.userContext}")
                            }
                        )
                    )
                })
                add(buildJsonObject {
                    put("type", JsonPrimitive("image_url"))
                    put(
                        "image_url",
                        buildJsonObject { put("url", JsonPrimitive(imageUrl)) }
                    )
                })
            }
        } else {
            JsonPrimitive(
                buildString {
                    append("请分析以下餐食：")
                    append(req.description.ifBlank { "（用户未填写文字描述，请按常见分量合理估算）" })
                    append("\n若为包装食品，请先联网查证包装上的营养成分表再换算。")
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
            你是一名严谨的营养师，负责识别用户提供的餐食（文字或照片），并给出准确、可溯源的营养数据。

            【包装/预包装食品】（有品牌、包装、条码或明确商品名，如「某某牌薯片 40g」「无糖酸奶 200ml」）
            1. 必须先联网搜索该商品的营养成分表，优先品牌官网、电商详情页的营养标签、权威食品数据库，不得凭记忆作答。
            2. 找到可靠数据后：按用户实际食用份量换算 caloriesKcal/proteinG/carbG/fatG，basis 写明份量（如「1 包 40g」「整瓶 500ml」），per100g 给出每 100 克的对照值，source 写明来源（如「XX 品牌官网营养成分表」），dataQuality 填 "searched"。
            3. 搜索不到权威数据时：dataQuality 填 "missing"，caloriesKcal/proteinG/carbG/fatG 全部填 0，basis 写实际份量，source 写「未找到可靠来源」，并在 summary 说明缺少哪项数据。严禁臆测或编造任何数值。

            【普通餐食】（家常菜、餐馆菜品、无明显包装）
            按常见分量合理估算，dataQuality 填 "estimated"，per100g 可为 null，source 填空字符串。

            【图片与文字描述同时存在时的标题判定】
            作用范围：
            - 图片负责：识别主体食材、烹饪方式、份量体积、包装与品牌外观、餐食类型（正餐/饮品/甜点）。
            - 文字负责：确定名称、品牌、口味、规格与份量；主观感受（好吃、少油、外卖、加班餐）不作为命名依据。
            优先级（高 → 低）：
            1) 文字中显式且具体的食品名称/品牌/菜品名；
            2) 图片中占画面主导（体积最大或最显眼）的主体食物；
            3) 文字中的份量/规格（如「半份」「中杯」「约 200g」）——只写入 basis，不写入 title；
            4) 其余泛化描述（「午饭」「外卖」「随便吃点」）——忽略，标题由图片决定。
            冲突处理：
            - 图文一致：取更具体的一方，去掉重复词，不要叠加同义词（如「西红柿番茄炒蛋」）。
            - 文字更具体且图片可佐证：以文字命名（如文字「番茄炒蛋盖饭」，图片为炒蛋盖饭）。
            - 文字与图片明显不符（如文字写「沙拉」，图片是汉堡）：以图片主体命名，并在 summary 追加「描述与图片不一致，已按图片识别」。
            - 文字笼统、图片清晰：以图片主体命名。
            - 图片含主菜 + 明显配菜/独立饮品：主项在前，最多组合两项，用「+」连接（如「牛肉面+冰美式」）；配菜占比很小时不写。
            - 两者都不可辨：使用最保守的类别名（如「家常炒菜」「混合沙拉」），不得使用「图片」「照片」「食物」「一餐」等占位词。
            标题规范：
            - ≤12 个汉字，中文名词短语；不加标点、emoji、引号，不写份量与热量。
            - 结构：[品牌]+[菜品/品名]+（可选）[关键口味或主料]，如「麦当劳板烧鸡腿堡」「番茄牛腩面」「瑞幸生椰拿铁」。
            - 品牌只在文字或包装上可辨识时出现，严禁臆造；不使用「疑似」「可能」等不确定词，不确定性写在 summary。

            【通用要求】
            - title 为具体菜名，不超过 12 个字；只有照片没有文字时也必须给出菜名，禁止使用「图片」「照片」「一餐」等占位词。
            - summary 为一句话点评与建议，不要重复数值。
            - 整体简洁清晰、结构统一，仅返回一个 JSON 对象，不要包含任何解释或 markdown 代码块：
            {"title":"菜名","caloriesKcal":整数,"proteinG":浮点数,"carbG":浮点数,"fatG":浮点数,"basis":"份量说明","per100g":{"caloriesKcal":整数,"proteinG":浮点数,"carbG":浮点数,"fatG":浮点数},"source":"数据来源","dataQuality":"searched|estimated|missing","summary":"一句话点评"}
        """.trimIndent()

        /** 旧版自定义提示语缺少字段时的补齐说明 */
        val LEGACY_EXTRA_FIELDS = """
            在 JSON 中额外返回以下字段：
            "title"（不超过 12 个字的菜名，只有照片也必须给出，禁止用「图片」「照片」等占位词）、
            "basis"（本次份量说明，如「1 包 50g」）、
            "per100g"（每 100 克的 caloriesKcal/proteinG/carbG/fatG）、
            "source"（数据来源）、
            "dataQuality"（searched / estimated / missing）。
            标题规则：图文同时存在时，优先采用文字中显式写出的菜名/品牌；文字笼统则用图片主体食物命名；
            两者明显冲突时以图片为准，并在 summary 注明「描述与图片不一致」；标题 ≤12 字，不加标点与份量。
            包装食品必须先联网查证营养标签；查不到可靠数据时 dataQuality 填 missing、各项数值填 0 并在 summary 说明，严禁猜测或编造。
        """.trimIndent()

        val DEFAULT_SUGGEST = """
            你是一名专业的营养师。请根据用户当日的剩余热量额度与饮食目标，推荐合适的食物。
            仅返回一个 JSON 对象，不要包含任何解释或 markdown 代码块，格式如下：
            {"items":[{"name":"食物名","caloriesKcal":整数,"reason":"推荐理由"}]}
            推荐 4-6 项，优先低热量高营养的密度食物。
        """.trimIndent()
    }
}
