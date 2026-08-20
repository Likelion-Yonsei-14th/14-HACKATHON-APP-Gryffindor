package com.gryffindor.smartshopping.data.repository

import android.util.Log
import com.gryffindor.smartshopping.data.remote.api.PersonalizationApiService
import com.gryffindor.smartshopping.data.remote.dto.FlightPatchRequestDto
import com.gryffindor.smartshopping.data.remote.dto.PurchaseRefundMethodPatchRequestDto
import com.gryffindor.smartshopping.data.repository.mapper.toDomain
import com.gryffindor.smartshopping.data.repository.mapper.toProduct
import com.gryffindor.smartshopping.domain.model.Flight
import com.gryffindor.smartshopping.domain.model.MyPage
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.model.Purchase
import com.gryffindor.smartshopping.domain.model.Receipt
import com.gryffindor.smartshopping.domain.model.RefundMethod
import com.gryffindor.smartshopping.domain.repository.PersonalizationRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Real PersonalizationRepository — connects to B5/B6 Backend APIs.
 * Handles MyPage, Wishlist, Receipt, Purchase, and Flight endpoints.
 */
class RemotePersonalizationRepository(
    private val apiService: PersonalizationApiService
) : PersonalizationRepository {

    override suspend fun getMyPage(): MyPage {
        Log.d(TAG, "getMyPage: GET /api/v1/me")
        return apiService.getMyPage().toDomain()
    }

    override suspend fun getWishlist(): List<Product> {
        Log.d(TAG, "getWishlist: GET /api/v1/me/wishlist")
        val response = apiService.getWishlist()
        return response.items.map { it.toProduct() }
    }

    override suspend fun addWishlist(productId: String): Product {
        Log.d(TAG, "addWishlist: POST /api/v1/me/wishlist/$productId")
        return apiService.addWishlist(productId).toProduct()
    }

    override suspend fun deleteWishlist(productId: String) {
        Log.d(TAG, "deleteWishlist: DELETE /api/v1/me/wishlist/$productId")
        apiService.deleteWishlist(productId)
    }

    override suspend fun analyzeReceipt(imageBytes: ByteArray, tripId: String?): Receipt {
        Log.d(TAG, "analyzeReceipt: POST /api/v1/me/receipts/analyze imageBytes=${imageBytes.size} tripId=$tripId")
        val imagePart = MultipartBody.Part.createFormData(
            "image",
            "receipt.jpg",
            imageBytes.toRequestBody("image/jpeg".toMediaType())
        )
        val tripIdPart = tripId?.toRequestBody("text/plain".toMediaType())
        return apiService.analyzeReceipt(imagePart, tripIdPart).toDomain()
    }

    override suspend fun getPurchases(): List<Purchase> {
        Log.d(TAG, "getPurchases: GET /api/v1/me/purchases")
        return apiService.getPurchases().map { it.toDomain() }
    }

    override suspend fun updatePurchaseRefundMethod(purchaseId: String, refundMethod: RefundMethod): Purchase {
        Log.d(TAG, "updatePurchaseRefundMethod: PATCH /api/v1/me/purchases/$purchaseId refundMethod=$refundMethod")
        val request = PurchaseRefundMethodPatchRequestDto(refundMethod = refundMethod.name)
        return apiService.updatePurchaseRefundMethod(purchaseId, request).toDomain()
    }

    override suspend fun analyzeFlight(imageBytes: ByteArray, tripId: String?): Flight {
        Log.d(TAG, "analyzeFlight: POST /api/v1/me/flights/analyze imageBytes=${imageBytes.size} tripId=$tripId")
        val imagePart = MultipartBody.Part.createFormData(
            "image",
            "flight.jpg",
            imageBytes.toRequestBody("image/jpeg".toMediaType())
        )
        val tripIdPart = tripId?.toRequestBody("text/plain".toMediaType())
        try {
            return apiService.analyzeFlight(imagePart, tripIdPart).toDomain()
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "analyzeFlight failed: HTTP ${e.code()} body=$errorBody", e)
            throw e
        }
    }

    override suspend fun updateFlight(
        flightId: String,
        departureAirport: String?,
        arrivalAirport: String?,
        terminal: String?,
        flightNumber: String?,
        departureAt: String?,
        arrivalAt: String?,
        airportArrivalAt: String?,
        tripId: String?
    ): Flight {
        Log.d(TAG, "updateFlight: PATCH /api/v1/me/flights/$flightId tripId=$tripId")
        val request = FlightPatchRequestDto(
            departureAirport = departureAirport,
            arrivalAirport = arrivalAirport,
            terminal = terminal,
            flightNumber = flightNumber,
            departureAt = departureAt,
            arrivalAt = arrivalAt,
            airportArrivalAt = airportArrivalAt,
            tripId = tripId
        )
        return apiService.updateFlight(flightId, request).toDomain()
    }

    companion object {
        private const val TAG = "RemotePersonalizationRepo"
    }
}
