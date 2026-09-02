package com.school.system.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApi {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

data class GeminiRequest(
    @SerializedName("contents") val contents: List<Content>
)

data class Content(
    @SerializedName("parts") val parts: List<Part>
)

data class Part(
    @SerializedName("text") val text: String
)

data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<Candidate>
)

data class Candidate(
    @SerializedName("content") val content: Content
)
