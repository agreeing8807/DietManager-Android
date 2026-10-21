package com.example.diet.data.remote

import com.example.diet.data.remote.model.ChatCompletionRequest
import com.example.diet.data.remote.model.ChatCompletionResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** OpenAI 兼容的 chat/completions 接口，通义/智谱/DeepSeek 均兼容 */
interface OpenAiCompatibleService {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body body: ChatCompletionRequest
    ): ChatCompletionResponse
}
