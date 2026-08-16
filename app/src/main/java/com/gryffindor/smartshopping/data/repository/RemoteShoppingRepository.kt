package com.gryffindor.smartshopping.data.repository

import android.util.Log
import com.gryffindor.smartshopping.data.remote.api.ShoppingApiService
import com.gryffindor.smartshopping.data.remote.dto.ReviewRequestDto
import com.gryffindor.smartshopping.data.repository.mapper.toDomain
import com.gryffindor.smartshopping.domain.model.AttentionCandidate
import com.gryffindor.smartshopping.domain.model.RecognitionResult
import com.gryffindor.smartshopping.domain.model.SessionProduct
import com.gryffindor.smartshopping.domain.repository.ShoppingRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Real ShoppingRepository implementation that communicates with the Backend API.
 * A4: Handles recognition multipart requests and product list fetching.
 */
class RemoteShoppingRepository(
    private val apiService: ShoppingApiService
) : ShoppingRepository {

    override suspend fun getProducts(sessionId: String): List<SessionProduct> {
        Log.d(TAG, "getProducts: GET /sessions/$sessionId/products")
        val response = apiService.getProducts(sessionId)
        return response.items.map { it.toDomain() }
    }

    override suspend fun submitReview(
        sessionId: String,
        purchasedProductIds: List<String>,
        interestedProductIds: List<String>
    ) {
        Log.d(TAG, "submitReview: PUT /sessions/$sessionId/review")
        apiService.submitReview(
            sessionId = sessionId,
            request = ReviewRequestDto(
                purchasedProductIds = purchasedProductIds,
                interestedProductIds = interestedProductIds
            )
        )
    }

    override suspend fun recognize(
        sessionId: String,
        candidate: AttentionCandidate
    ): RecognitionResult {
        Log.d(TAG, buildString {
            append("recognize: POST /sessions/$sessionId/recognize ")
            append("trackingId=${candidate.trackingId} ")
            append("triggerType=${candidate.triggerType.name} ")
            append("occupancy=${"%.3f".format(candidate.occupancyRatio)} ")
            append("dwell=${candidate.dwellMs}ms ")
            append("jpegSize=${candidate.jpegBytes.size}")
        })

        val imagePart = MultipartBody.Part.createFormData(
            "image",
            "crop.jpg",
            candidate.jpegBytes.toRequestBody("image/jpeg".toMediaType())
        )

        val capturedAtPart = candidate.capturedAt
            .toRequestBody("text/plain".toMediaType())

        val triggerTypePart = candidate.triggerType.name
            .toRequestBody("text/plain".toMediaType())

        val occupancyRatioPart = candidate.occupancyRatio.toString()
            .toRequestBody("text/plain".toMediaType())

        val dwellMsPart = candidate.dwellMs.toString()
            .toRequestBody("text/plain".toMediaType())

        val trackingIdPart = candidate.trackingId
            ?.toRequestBody("text/plain".toMediaType())

        val response = apiService.recognize(
            sessionId = sessionId,
            image = imagePart,
            capturedAt = capturedAtPart,
            triggerType = triggerTypePart,
            occupancyRatio = occupancyRatioPart,
            dwellMs = dwellMsPart,
            trackingId = trackingIdPart
        )

        val result = response.toDomain()

        Log.d(TAG, buildString {
            append("recognize result: ${response.recognitionStatus}")
            if (result is RecognitionResult.Matched) {
                append(" productId=${result.observedProduct.product.productId}")
                append(" name=${result.observedProduct.product.name}")
                append(" isNew=${result.isNew}")
            }
        })

        return result
    }

    companion object {
        private const val TAG = "RemoteShoppingRepo"
    }
}
