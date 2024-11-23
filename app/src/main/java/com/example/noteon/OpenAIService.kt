package com.example.noteon

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface OpenAIService {
    @POST("threads")
    suspend fun createThread(): ThreadResponse

    @POST("threads/{threadId}/messages")
    suspend fun addMessage(
        @Path("threadId") threadId: String,
        @Body message: MessageRequest
    ): MessageResponse

    @POST("threads/{threadId}/runs")
    suspend fun createRun(
        @Path("threadId") threadId: String,
        @Body runRequest: RunRequest
    ): RunResponse

    @GET("threads/{threadId}/runs/{runId}")
    suspend fun getRun(
        @Path("threadId") threadId: String,
        @Path("runId") runId: String
    ): RunResponse

    @GET("threads/{threadId}/messages")
    suspend fun getMessages(
        @Path("threadId") threadId: String,
        @Query("order") order: String = "asc",
        @Query("limit") limit: Int = 100
    ): MessagesResponse

    companion object {
        fun create(): OpenAIService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer ${OpenAIConfig.API_KEY}")
                        .addHeader("OpenAI-Beta", "assistants=v2")
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(loggingInterceptor)
                .build()

            return Retrofit.Builder()
                .baseUrl(OpenAIConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenAIService::class.java)
        }
    }
}