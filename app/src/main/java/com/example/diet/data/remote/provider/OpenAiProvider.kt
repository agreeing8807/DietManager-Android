package com.example.diet.data.remote.provider

import com.example.diet.data.local.FoodCompositionTable
import com.example.diet.data.remote.ApiKeyStore
import com.example.diet.data.remote.LLMProvider
import com.example.diet.data.remote.OpenAiCompatibleService
import com.example.diet.data.remote.model.ChatCompletionRequest
import com.example.diet.data.remote.model.ChatMessage
import com.example.diet.data.remote.model.FillItemJson
import com.example.diet.data.remote.model.FoodSuggestionListJson
import com.example.diet.data.remote.model.IdentifyItemJson
import com.example.diet.data.remote.model.MealIdentifyJson
import com.example.diet.data.remote.model.NutritionFillJson
import com.example.diet.data.remote.model.NutritionLabelJson
import com.example.diet.data.remote.model.PortionEstimateJson
import com.example.diet.data.remote.model.ResponseFormat
import com.example.diet.domain.MealSummaryFormatter
import com.example.diet.domain.NutritionTools
import com.example.diet.domain.Per100g
import com.example.diet.domain.ResolvedItem
import com.example.diet.domain.model.DataStatus
import com.example.diet.domain.model.FoodSuggestion
import com.example.diet.domain.model.FoodSuggestionRequest
import com.example.diet.domain.model.MealAnalysis
import com.example.diet.domain.model.MealAnalysisItem
import com.example.diet.domain.model.MealAnalysisRequest
import com.example.diet.domain.model.NutritionLabel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.util.Base64
import kotlin.math.roundToInt

/**
 * 单一 OpenAI 兼容实现，通过 VendorConfig 适配不同国内厂商。
 *
 * 营养计算分三步，模型只承担「识别」与「补数」，不做四则运算：
 * 1. 识别：模型只回答看到了什么食物、品牌、克数、是否包装（[MealIdentifyJson]）；
 * 2. 本地：常见食物直接查 [FoodCompositionTable] 做确定性换算；
 * 3. 补数：本地未命中（或包装商品）时才让模型补「每 100g」数据（[NutritionFillJson]），
 *    并要求给出 source 与 confidence，低置信度数据直接丢弃。
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
     * 提示语使用新的 key（prompt_identify）：
     * 语义已从「估算营养」变为「只识别食物」，旧的 prompt_analyze 内容会让模型输出营养数值，
     * 与本地计算架构冲突，因此不再读取。
     */
    private fun identifyPrompt(): String =
        apiKeyStore.getSetting("prompt_identify", DEFAULT_IDENTIFY).ifBlank { DEFAULT_IDENTIFY }

    private fun fillPrompt(): String =
        apiKeyStore.getSetting("prompt_fill", DEFAULT_FILL).ifBlank { DEFAULT_FILL }

    private fun suggestPrompt(): String =
        apiKeyStore.getSetting("prompt_suggest", DEFAULT_SUGGEST).ifBlank { DEFAULT_SUGGEST }

    /** 营养成分表读取：只 OCR + 结构化，不做换算 */
    private fun labelPrompt(): String =
        apiKeyStore.getSetting("prompt_label", DEFAULT_LABEL).ifBlank { DEFAULT_LABEL }

    /** 份量估算：只回答克数 */
    private fun portionPrompt(): String =
        apiKeyStore.getSetting("prompt_portion", DEFAULT_PORTION).ifBlank { DEFAULT_PORTION }

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

        return runCatching {
            // 1) 只做识别
            val identify = requestIdentify(req)
            val items = identify.items
                .map { it.sanitized() }
                .filter { it.name.isNotBlank() }
            if (items.isEmpty()) {
                throw IllegalStateException("未从这餐中识别出具体食物，请补充文字描述")
            }

            // 2) 本地成分表优先；包装商品跳过通用条目（品牌规格差异大）
            val resolved = mutableListOf<ResolvedItem>()
            val missingNames = mutableListOf<String>()
            val pending = mutableListOf<Pair<Int, IdentifyItemJson>>()
            // 每条识别结果对应的解析结果，null 表示没查到数据，用于落库明细
            val resolvedByIndex = mutableMapOf<Int, ResolvedItem>()

            // 每条识别结果的实际克数（可能由「每份克数 × 份数」推导出来）
            val gramsByIndex = mutableMapOf<Int, Float>()

            items.forEachIndexed { index, item ->
                val local = if (item.packaged) null else FoodCompositionTable.lookup(item.name)
                // 明确给出份数（如「两个包子」）但没给克数时，用成分表的常见份量推导；
                // 两者都缺则保持缺失，不默认按一份处理
                val grams = item.grams.takeIf { it > 0f }
                    ?: local?.servingGrams?.takeIf { it > 0f && item.servings > 0f }
                        ?.times(item.servings)
                    ?: 0f
                gramsByIndex[index] = grams
                if (grams <= 0f) {
                    missingNames += item.name
                    return@forEachIndexed
                }
                if (local != null) {
                    val hit = ResolvedItem(
                        name = item.name,
                        grams = grams,
                        per100 = local.per100,
                        source = FoodCompositionTable.SOURCE,
                        confidence = 1f
                    )
                    resolved += hit
                    resolvedByIndex[index] = hit
                } else {
                    pending += index to item
                }
            }

            // 3) 未命中的项由模型补每 100g 数据（一次请求批量完成）
            if (pending.isNotEmpty()) {
                val fills = requestFill(pending.map { it.second })
                pending.forEach { (index, item) ->
                    val fill = fills[normalizeName(item.name)]
                    val per100 = fill?.toPer100()
                    if (fill != null && per100 != null) {
                        val hit = ResolvedItem(
                            name = item.name,
                            grams = gramsByIndex[index] ?: item.grams,
                            per100 = per100,
                            source = fill.source.trim().ifBlank { "模型补充" },
                            confidence = fill.confidence
                        )
                        resolved += hit
                        resolvedByIndex[index] = hit
                    } else {
                        // 补数不可用时，包装商品退回本地同类条目，其余标记缺失
                        val fallback = FoodCompositionTable.lookup(item.name)
                        if (fallback != null) {
                            val hit = ResolvedItem(
                                name = item.name,
                                grams = gramsByIndex[index] ?: item.grams,
                                per100 = fallback.per100,
                                source = "${FoodCompositionTable.SOURCE}（同类参考）"
                            )
                            resolved += hit
                            resolvedByIndex[index] = hit
                        } else {
                            missingNames += item.name
                        }
                    }
                }
            }

            // 明细：即使没查到数据也保留条目，用户可补克数或直接删除
            val analysisItems = items.mapIndexed { index, item ->
                val hit = resolvedByIndex[index]
                MealAnalysisItem(
                    name = item.name,
                    grams = gramsByIndex[index] ?: item.grams,
                    servings = item.servings,
                    brand = item.brand,
                    per100 = hit?.per100,
                    source = hit?.source.orEmpty(),
                    confidence = hit?.confidence ?: 0f,
                    status = if (hit != null) DataStatus.ESTIMATED else DataStatus.MISSING
                )
            }

            // 数据状态在这里一次确定：全缺 / 部分缺 / 完整（份量仍为估算）
            val status = when {
                resolved.isEmpty() -> DataStatus.MISSING
                missingNames.isNotEmpty() -> DataStatus.PARTIAL
                else -> DataStatus.ESTIMATED
            }
            val totals = NutritionTools.validate(NutritionTools.sum(resolved)).totals
            val gramsSum = resolved.filter { it.per100 != null }.fold(0f) { acc, i -> acc + i.grams }
            val summary = MealSummaryFormatter.build(
                items = resolved,
                totals = totals,
                gramsSum = gramsSum,
                sources = resolved.mapNotNull { it.source.takeIf { s -> s.isNotBlank() } }.distinct(),
                missingNames = missingNames,
                conflict = identify.conflict.trim(),
                imageIgnored = imageIgnored
            )
            MealAnalysis(
                caloriesKcal = totals.kcal,
                proteinG = totals.proteinG,
                carbG = totals.carbG,
                fatG = totals.fatG,
                summary = summary,
                title = sanitizeTitle(identify.title),
                status = status,
                items = analysisItems
            )
        }
    }

    /**
     * 只补缺失的营养基准：不重新识别、不改克数，
     * 一次请求覆盖所有缺数据的条目；补不到的条目原样返回。
     */
    override suspend fun fillNutrition(items: List<MealAnalysisItem>): Result<List<MealAnalysisItem>> {
        val need = items.filter { it.per100 == null && it.name.isNotBlank() }
        if (need.isEmpty()) return Result.success(items)
        if (apiKey.isBlank()) return Result.failure(
            IllegalStateException("未配置 ${config.name} 的 API Key")
        )
        return runCatching {
            val fills = requestFill(
                need.map { IdentifyItemJson(name = it.name, brand = it.brand, grams = it.grams) }
            )
            items.map { item ->
                if (item.per100 != null) return@map item
                val fill = fills[normalizeName(item.name)]
                val per100 = fill?.toPer100()
                if (fill == null || per100 == null) {
                    item
                } else {
                    item.copy(
                        per100 = per100,
                        source = fill.source.trim().ifBlank { "模型补充" },
                        confidence = fill.confidence,
                        status = DataStatus.ESTIMATED
                    )
                }
            }
        }
    }

    /** 第一步：只识别食物，不要求任何营养数值 */
    private suspend fun requestIdentify(req: MealAnalysisRequest): MealIdentifyJson {
        val request = ChatCompletionRequest(
            model = model.ifBlank { config.defaultModel },
            messages = listOf(
                ChatMessage("system", JsonPrimitive(identifyPrompt())),
                ChatMessage("user", buildUserContent(req))
            ),
            temperature = 0.2,
            response_format = ResponseFormat("json_object")
        )
        val raw = complete(
            request,
            "模型未返回内容（请确认所选模型支持图片识别，或改用文字描述）"
        )
        return json.decodeFromString<MealIdentifyJson>(stripFences(raw))
    }

    /** 第三步：为本地未命中的食物补「每 100g」数据；失败不阻断整体，按缺失处理 */
    private suspend fun requestFill(items: List<IdentifyItemJson>): Map<String, FillItemJson> {
        val userText = buildString {
            append("请为下列食物补全每 100g（饮品每 100ml）营养数据，name 必须与输入完全一致：")
            items.forEach {
                append("\n- name=").append(it.name)
                if (it.brand.isNotBlank()) append("；brand=").append(it.brand)
                append("；本次份量=").append(it.grams.roundToInt()).append("g")
                if (it.packaged) append("；属于包装商品")
            }
        }
        val request = ChatCompletionRequest(
            model = model.ifBlank { config.defaultModel },
            messages = listOf(
                ChatMessage("system", JsonPrimitive(fillPrompt())),
                ChatMessage("user", JsonPrimitive(userText))
            ),
            temperature = 0.1,
            response_format = ResponseFormat("json_object")
        )
        return runCatching {
            val raw = complete(request, "营养补全未返回内容")
            json.decodeFromString<NutritionFillJson>(stripFences(raw))
                .items
                .filter { it.name.isNotBlank() }
                .associateBy { normalizeName(it.name) }
        }.getOrElse { emptyMap() }
    }

    private suspend fun complete(request: ChatCompletionRequest, emptyMessage: String): String {
        val resp = service.chat("${config.authPrefix} $apiKey", request)
        resp.error?.let { throw IllegalStateException(it.message.ifBlank { "接口返回错误" }) }
        return resp.choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(emptyMessage)
    }

    private fun IdentifyItemJson.sanitized(): IdentifyItemJson = copy(
        name = name.trim(),
        brand = brand.trim(),
        grams = if (grams > 0f) grams.coerceAtMost(NutritionTools.MAX_GRAMS) else 0f,
        servings = if (servings > 0f) servings.coerceAtMost(50f) else 0f
    )

    /** 置信度过低或四项全 0 时视为不可用；门槛已放宽，同类估算会被保留 */
    private fun FillItemJson.toPer100(): Per100g? {
        if (confidence < MIN_CONFIDENCE) return null
        if (kcalPer100g <= 0f && proteinPer100g <= 0f && carbPer100g <= 0f && fatPer100g <= 0f) return null
        return Per100g(
            kcal = kcalPer100g.coerceAtLeast(0f),
            proteinG = proteinPer100g.coerceAtLeast(0f),
            carbG = carbPer100g.coerceAtLeast(0f),
            fatG = fatPer100g.coerceAtLeast(0f)
        )
    }

    private fun normalizeName(value: String): String =
        value.replace(Regex("[\\s（）()【】\\[\\]·、,，.。/\\-]"), "").lowercase()

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

    override suspend fun suggestFoods(req: FoodSuggestionRequest): Result<List<FoodSuggestion>> {
        if (apiKey.isBlank()) return Result.failure(
            IllegalStateException("未配置 ${config.name} 的 API Key")
        )
        val userText = buildString {
            if (req.currentTime.isNotBlank()) {
                append("当前时间：${req.currentTime}")
                if (req.mealPeriod.isNotBlank()) append("（${req.mealPeriod}时段）")
                append("。")
            }
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
            val raw = complete(request, "模型未返回内容")
            val parsed = json.decodeFromString<FoodSuggestionListJson>(stripFences(raw))
            parsed.items.map {
                FoodSuggestion(name = it.name, caloriesKcal = it.caloriesKcal, reason = it.reason)
            }
        }
    }

    /**
     * 营养成分表：只读标签，不做换算。
     * 归一化到每 100g 与按克算热量都在本地完成，模型算错除法的风险降为零。
     */
    override suspend fun readNutritionLabel(imageBytes: ByteArray): Result<NutritionLabel> {
        if (apiKey.isBlank()) return Result.failure(
            IllegalStateException("未配置 ${config.name} 的 API Key")
        )
        if (!config.supportsImage(model)) return Result.failure(
            IllegalStateException(
                "当前模型（${config.name} / ${model.ifBlank { config.defaultModel }}）不支持图片，" +
                    "读取营养成分表需要视觉模型，如 qwen-vl-plus、glm-4v、deepseek-flash"
            )
        )
        return runCatching {
            val request = ChatCompletionRequest(
                model = model.ifBlank { config.defaultModel },
                messages = listOf(
                    ChatMessage("system", JsonPrimitive(labelPrompt())),
                    ChatMessage(
                        "user",
                        imageContent(imageBytes, "读取这张包装上的营养成分表，按 JSON 返回。")
                    )
                ),
                temperature = 0.1,
                response_format = ResponseFormat("json_object")
            )
            val raw = complete(request, "未识别到营养成分表，请对准标签、保证文字清晰后重拍")
            val parsed = json.decodeFromString<NutritionLabelJson>(stripFences(raw))
            NutritionLabel(
                name = parsed.name.trim().ifBlank { "包装食品" },
                brand = parsed.brand.trim(),
                basisGrams = parsed.basisGrams.takeIf { it > 0f } ?: 100f,
                kcal = parsed.kcal.coerceAtLeast(0f),
                proteinG = parsed.proteinG.coerceAtLeast(0f),
                carbG = parsed.carbG.coerceAtLeast(0f),
                fatG = parsed.fatG.coerceAtLeast(0f),
                liquid = parsed.liquid,
                source = parsed.source.trim().ifBlank { "包装营养标签" }
            )
        }
    }

    /** 只估份量：给营养成分表补克重时使用，不触碰营养数值 */
    override suspend fun estimateGrams(imageBytes: ByteArray, foodName: String): Result<Float> {
        if (apiKey.isBlank()) return Result.failure(
            IllegalStateException("未配置 ${config.name} 的 API Key")
        )
        if (!config.supportsImage(model)) return Result.failure(
            IllegalStateException("当前模型不支持图片，无法按照片估算份量")
        )
        return runCatching {
            val request = ChatCompletionRequest(
                model = model.ifBlank { config.defaultModel },
                messages = listOf(
                    ChatMessage("system", JsonPrimitive(portionPrompt())),
                    ChatMessage(
                        "user",
                        imageContent(
                            imageBytes,
                            "估算这张照片里「${foodName.ifBlank { "食物" }}」的可食用克数，按 JSON 返回。"
                        )
                    )
                ),
                temperature = 0.2,
                response_format = ResponseFormat("json_object")
            )
            val raw = complete(request, "未能估算份量，请换一张能看清分量的照片")
            val parsed = json.decodeFromString<PortionEstimateJson>(stripFences(raw))
            parsed.grams.coerceIn(0f, NutritionTools.MAX_GRAMS)
        }
    }

    /** 图片 + 一句指令的 user content，供读标签与估份量复用 */
    private fun imageContent(imageBytes: ByteArray, text: String): JsonElement {
        val b64 = Base64.getEncoder().encodeToString(imageBytes)
        val imageUrl = if (config.imageDataUri) "data:image/jpeg;base64,$b64" else b64
        return buildJsonArray {
            add(buildJsonObject {
                put("type", JsonPrimitive("text"))
                put("text", JsonPrimitive(text))
            })
            add(buildJsonObject {
                put("type", JsonPrimitive("image_url"))
                put("image_url", buildJsonObject { put("url", JsonPrimitive(imageUrl)) })
            })
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
                                append("识别这张照片里的食物，逐项列出可食用食物/饮品、常见份量与是否包装食品。")
                                append("\n不要估算热量或营养成分，数值由系统计算。")
                                if (req.description.isNotBlank()) {
                                    append("\n用户补充说明：${req.description}")
                                } else {
                                    append("\n用户未填写文字描述，照片即全部信息。")
                                }
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
                    append("识别以下餐食描述里的食物，逐项列出名称、常见份量与是否包装食品。")
                    append("\n不要估算热量或营养成分，数值由系统计算。")
                    append("\n描述：").append(req.description)
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
        /**
         * 低于该置信度的补数结果视为不可用。
         * 门槛从 0.3 降到 0.15：同类食物估算通常落在 0.2~0.4，过严会把合理估值全部丢掉。
         */
        private const val MIN_CONFIDENCE = 0.15f

        val DEFAULT_IDENTIFY = """
            你是食物识别助手。唯一任务是判断用户这一餐吃了什么、大约多少克，不要计算任何营养数值。

            【字段要求】
            1. items：每项对应一种可食用食物/饮品，同一食物不重复；复合菜品（如「番茄炒蛋」）作为一个条目。
            2. name：具体名称，不含品牌与份量，如「番茄炒蛋」「板烧鸡腿堡」「生椰拿铁」。
            3. brand：仅当品牌可从图片包装或文字确认时填写，否则空串，严禁臆测。
            4. servings：可数单位的份数，如 1 个鸡蛋、2 个包子、1 勺蛋白粉、1 瓶饮料；无法按份表述（如一份炒菜）时填 0。
            5. grams：可食用部分总克数（饮品按 ml 近似为 g），必须给出估计值，不要填 0。
               参考常见份量：一份米饭约 200g、一份炒菜 250g、一个鸡蛋 50g、一个包子 80g、
               一瓶饮料 500ml、一包薯片 40g；看不清时按同类食物的常见份量取值，
               再按图片里的大小、深浅、堆叠程度做增减，宁可略保守也不要留空。
            6. packaged：有品牌包装、条码或明确商品名时为 true；家常菜、散装食材、餐馆现制菜品为 false。

            【title 规则】
            - ≤12 个汉字的中文名词短语，不加标点、emoji、引号，不写份量与热量。
            - 结构：[品牌]+[菜品/品名]+（可选）[关键口味或主料]。
            - 主菜在前，最多用「+」连接两项（如「牛肉面+冰美式」）；占比很小的配菜不写。
            - 图片与文字同时存在：文字给出明确名称/品牌/规格时以文字为准；文字笼统则以图片主体为准。
            - 禁止使用「图片」「照片」「食物」「一餐」「外卖」等占位词。

            【conflict 规则】
            - 文字描述与图片明显不一致时（如文字「沙拉」而图片是汉堡），用一句话说明差异；
            - 一致、只有图片或只有文字时为空串。

            【输出】仅返回一个 JSON 对象，不要解释、不要 markdown 代码块：
            {"title":"菜名","items":[{"name":"食物","brand":"","servings":1,"grams":200,"packaged":false}],"conflict":""}
        """.trimIndent()

        val DEFAULT_FILL = """
            你是食物营养成分数据补全工具。用户会给出若干食物名称（可能附带品牌与份量），
            请为每个食物给出每 100 克（饮品每 100ml）的典型营养值。

            【要求】
            1. 只补数据，不点评、不合并条目；返回的 name 必须与输入完全一致，条数与输入一致。
            2. 包装商品：按该商品营养标签的每 100g 值填写，source 写「品牌/商品营养标签」。
            3. 普通食物：按权威食物成分表典型值填写，source 写「中国食物成分表典型值」等依据。
            4. 即使不完全确定，也必须给出该食物最接近的同类典型值，不要因为"拿不准"就填 0：
               先找同名或近义食物的成分表数据，其次按类别（主食/肉类/蔬菜/水果/饮品/零食）取典型值；
               source 写清依据，例如「同类食物估算：炒面」。只有完全无法归类（如不明成分的自制酱料）
               才四项填 0、confidence 填 0。
            5. confidence 为 0~1 的可靠程度，按把握如实填写：有明确成分表数据填 0.8~1.0，
               按同类食物推断填 0.3~0.6，纯类别估计填 0.15~0.3。
            6. 单位：热量 kcal，蛋白质/碳水/脂肪 g，均为每 100g。

            【输出】仅返回一个 JSON 对象，不要解释、不要 markdown 代码块：
            {"items":[{"name":"食物","kcalPer100g":0,"proteinPer100g":0,"carbPer100g":0,"fatPer100g":0,"source":"","confidence":0.8}]}
        """.trimIndent()

        val DEFAULT_LABEL = """
            你是包装食品营养成分表的读取工具。只读标签上的原文，不做任何换算、不做任何估算。

            【字段要求】
            1. name：商品名，取自包装正面或标签上的产品名称，如「希腊风味酸奶」「全麦吐司」；
               不要带「营养成分表」「配料表」等字样。
            2. brand：包装上的品牌名；看不清就留空串，严禁臆测。
            3. basisGrams：标签数值对应的基准克数。
               标注「每 100g」填 100；标注「每份 30g」「每袋 25g」时填该克数；无法确认填 100。
            4. kcal / proteinG / carbG / fatG：严格照抄标签在该基准下的数值，单位 kcal 与 g。
               标签用 kJ 时按 1 kcal = 4.184 kJ 换算后填入，并在 source 注明「由 kJ 换算」。
               标签只给「每份」时，直接填每份数值，不要把每份换算成每 100g。
            5. liquid：饮料、乳品等按毫升标注时为 true，否则 false。
            6. source：读取依据，如「包装营养成分表 · 每 100g」。

            【注意】
            - 某一行看不清时该字段填 0，不要猜。
            - 只要能看到产品名，name 就不要留空。
            - 仅返回一个 JSON 对象，不要解释、不要 markdown 代码块：
            {"name":"商品名","brand":"","basisGrams":100,"kcal":250,"proteinG":5.2,"carbG":30,"fatG":10,"liquid":false,"source":"包装营养成分表"}
        """.trimIndent()

        val DEFAULT_PORTION = """
            你是份量估算工具。根据照片估计指定食物可食用部分的重量（克）。

            【要求】
            1. 用照片里的参照物推断体积：餐具、手掌、包装规格、标准餐盘。
            2. 常见参照：一碗米饭约 200g、一个鸡蛋约 50g、一个包子约 80g、
               一份炒菜约 250g、一片吐司约 30g、一杯饮品约 300ml（按 1ml ≈ 1g 计）。
            3. 只给重量，不判断营养、不点评、不建议。
            4. 完全看不清时 grams 填 0，并在 note 说明原因。

            【输出】仅返回一个 JSON 对象，不要解释、不要 markdown 代码块：
            {"grams":250,"note":"标准餐盘中的一份"}
        """.trimIndent()

        val DEFAULT_SUGGEST = """
            你是一名熟悉中国高校校园饮食的营养师助手，服务对象是【在校大学生】。
            请严格依据用户消息给出的「当前时间」「今日剩余热量」与「用户背景」，
            推荐此刻真正买得到、吃得上、能带回宿舍的食物。

            【时间判断（最高优先级）】
            - 时段只以「当前时间」为准，不要自行假设日期或钟点；消息中附有时段标签时以标签为准。
            - 早餐 5:00-9:00、午餐 10:00-14:00、下午加餐 15:00-16:00、晚餐 17:00-20:00、夜宵 21:00-次日 4:00。
            - 注意星期：工作日按上课作息推荐；周六周日食堂窗口减少、起床较晚，早午餐常合并，
              优先推荐供应稳定、随时能买到的品类。
            - 处于时段交界（如 9:50、14:10、20:30）时，按「此刻还买得到什么」而不是「名义上该吃什么」推荐，
              并在 reason 中点明时间原因。

            【获取渠道（按可达性优先）】
            1. 正在营业的食堂窗口（早/午/晚餐时段）；
            2. 校园超市、便利店、宿舍楼下自助售卖机（全天可用，错过饭点与夜宵首选）；
            3. 校园面包房、咖啡店、奶茶店（现制饮品与烘焙）；
            4. 外卖（仅前三者都不合适时，选配送快的常见连锁）。
            - 食堂已过供应时间（如 14:00 后、20:30 后）不要推荐现炒菜、现打套餐，
              改推荐超市/便利店的现成食品并修正分量预期。
            - 只推荐无需复杂烹饪、无需自备餐具、能边走边吃或打包带回宿舍的选项。
            - 每项的获取渠道必须写进 reason，例如「三食堂早餐窗口」「宿舍楼下便利店」「校门口超市」「外卖：XX 快餐」。

            【品类与营养】
            - 早餐：包子、粥、鸡蛋、豆浆、牛奶、全麦面包，保证蛋白质与主食，避免重油重盐。
            - 午餐：食堂套餐（一荤两素）、盖饭、面食，兼顾蛋白质与蔬菜，热量可略高。
            - 下午加餐：水果、无糖酸奶、坚果、牛奶，高蛋白低糖、小份。
            - 晚餐：清淡为主，蒸煮类、杂粮、蔬菜配优质蛋白，避免接近夜宵的重口味。
            - 夜宵：尽量少吃，可选温牛奶、无糖酸奶、小份水果；避免油炸、高糖、奶茶与含咖啡因饮品。
            - 兼顾学生预算，单份尽量控制在 15 元以内，价格敏感时在 reason 中说明。
            - 若剩余热量低于 300 kcal，优先低热量高蛋白小份选项；剩余充足可含一份主食；
              深夜仍剩大量额度时优先低负担小份并在 reason 中说明。

            【输出要求】
            - 按「此刻最合适 + 最容易买到」综合排序，推荐 4-6 项；
            - name 具体到可直接购买（如「食堂鸡腿饭套餐」「便利店无糖酸奶 200g」「超市全麦面包 2 片」），
              不要使用「健康餐」「营养搭配」「轻食」这类无法直接购买的模糊词；
            - reason 用一句话说明「为什么适合现在吃」以及「在哪里能买到」，不重复热量数值；
            - 仅返回一个 JSON 对象，不要任何解释或 markdown 代码块：
            {"items":[{"name":"食物名","caloriesKcal":整数,"reason":"推荐理由"}]}
        """.trimIndent()
    }
}
