package com.gryffindor.smartshopping.data.remote.api

import com.gryffindor.smartshopping.data.remote.dto.ProductListResponseDto
import com.gryffindor.smartshopping.data.remote.dto.RecommendationsResponseDto
import com.gryffindor.smartshopping.data.remote.dto.RecognitionResponseDto
import com.gryffindor.smartshopping.data.remote.dto.RefundChecklistDto
import com.gryffindor.smartshopping.data.remote.dto.ReviewRequestDto
import com.gryffindor.smartshopping.data.remote.dto.ReviewResponseDto
import com.gryffindor.smartshopping.data.remote.dto.SessionCompleteResponseDto
import com.gryffindor.smartshopping.data.remote.dto.SessionCreateRequestDto
import com.gryffindor.smartshopping.data.remote.dto.SessionCreateResponseDto
import com.gryffindor.smartshopping.data.remote.dto.TravelRequestDto
import com.gryffindor.smartshopping.data.remote.dto.TravelResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Retrofit API interface matching Backend API contract.
 * Skeleton only in A0 — no actual network calls are made.
 */
interface ShoppingApiService {

    @POST("sessions")
    suspend fun createSession(
        @Body request: SessionCreateRequestDto
    ): SessionCreateResponseDto

    @POST("sessions/{sessionId}/complete")
    suspend fun completeSession(
        @Path("sessionId") sessionId: String
    ): SessionCompleteResponseDto

    @Multipart
    @POST("sessions/{sessionId}/recognize")
    suspend fun recognize(
        @Path("sessionId") sessionId: String,
        @Part image: MultipartBody.Part,
        @Part("capturedAt") capturedAt: RequestBody,
        @Part("triggerType") triggerType: RequestBody,
        @Part("occupancyRatio") occupancyRatio: RequestBody,
        @Part("dwellMs") dwellMs: RequestBody,
        @Part("trackingId") trackingId: RequestBody?
    ): RecognitionResponseDto

    @GET("sessions/{sessionId}/products")
    suspend fun getProducts(
        @Path("sessionId") sessionId: String
    ): ProductListResponseDto

    @PUT("sessions/{sessionId}/review")
    suspend fun submitReview(
        @Path("sessionId") sessionId: String,
        @Body request: ReviewRequestDto
    ): ReviewResponseDto

    @PUT("sessions/{sessionId}/travel")
    suspend fun submitTravel(
        @Path("sessionId") sessionId: String,
        @Body request: TravelRequestDto
    ): TravelResponseDto

    @GET("sessions/{sessionId}/refund-checklist")
    suspend fun getRefundChecklist(
        @Path("sessionId") sessionId: String
    ): RefundChecklistDto

    @GET("sessions/{sessionId}/recommendations")
    suspend fun getRecommendations(
        @Path("sessionId") sessionId: String
    ): RecommendationsResponseDto
}
