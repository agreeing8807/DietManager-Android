package com.example.diet.data.remote

import com.example.diet.data.remote.model.ChatCompletionRequest
import com.example.diet.data.remote.model.ChatMessage
import com.example.diet.data.remote.model.ResponseFormat
import com.example.diet.data.remote.provider.OpenAiProvider
import com.example.diet.data.remote.provider.VendorConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType

/**
 * 管理可切换的厂商配置，并按需构建对应的 LLMProvider。
 */
@Singleton
class LLMManager @Inject constructor(
    private val apiKeyStore: ApiKeyStore
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val vendors: List<VendorConfig> = listOf(
        VendorConfig(
            id = "tongyi", name = "通义千问",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            defaultModel = "qwen-vl-plus", supportsVision = true
        ),
        VendorConfig(
            id = "zhipu", name = "智谱 GLM",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            defaultModel = "glm-4v", supportsVision = true
        ),
        VendorConfig(
            id = "deepseek", name = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1",
            defaultModel = "deepseek-chat", supportsVision = false
        )
    )

    private val client by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // BASIC 不打印请求/响应体与鉴权头，避免泄露 API Key
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder().addInterceptor(logging).build()
    }

    private val serviceCache = mutableMapOf<String, OpenAiCompatibleService>()

    private fun serviceFor(config: VendorConfig): OpenAiCompatibleService {
        val base = if (config.baseUrl.endsWith("/")) config.baseUrl else config.baseUrl + "/"
        return serviceCache.getOrPut(base) {
            Retrofit.Builder()
                .baseUrl(base)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(OpenAiCompatibleService::class.java)
        }
    }

    fun getVendor(id: String): VendorConfig =
        vendors.firstOrNull { it.id == id } ?: vendors.first()

    /** 当前用于分析的 API（最后添加/选中一条），未配置返回 null */
    fun getActiveProvider(): OpenAiProvider? {
        val entry = apiKeyStore.getSelectedEntry() ?: return null
        val config = getVendor(entry.vendorId)
        return OpenAiProvider(
            id = config.id,
            displayName = config.name,
            config = config,
            service = serviceFor(config),
            apiKey = entry.apiKey,
            model = entry.model.ifBlank { config.defaultModel },
            apiKeyStore = apiKeyStore
        )
    }

    /** 测试单条 API 连接 */
    suspend fun testEntry(entry: ApiEntry): Result<Unit> = runCatching {
        if (entry.apiKey.isBlank()) throw IllegalStateException("API Key 为空")
        val config = getVendor(entry.vendorId)
        val resp = serviceFor(config).chat(
            "${config.authPrefix} ${entry.apiKey}",
            ChatCompletionRequest(
                model = entry.model.ifBlank { config.defaultModel },
                messages = listOf(ChatMessage("user", JsonPrimitive("ping"))),
                temperature = 0.1
            )
        )
        if (resp.error != null) throw IllegalStateException(resp.error.message)
    }
}
