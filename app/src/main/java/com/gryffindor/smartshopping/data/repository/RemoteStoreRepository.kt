package com.gryffindor.smartshopping.data.repository

import android.util.Log
import com.gryffindor.smartshopping.data.remote.api.ShoppingApiService
import com.gryffindor.smartshopping.data.remote.mapper.toDomain
import com.gryffindor.smartshopping.domain.model.Store
import com.gryffindor.smartshopping.domain.repository.StoreRepository

/**
 * Real StoreRepository implementation that fetches stores from Backend API.
 */
class RemoteStoreRepository(
    private val apiService: ShoppingApiService
) : StoreRepository {

    override suspend fun getStores(): List<Store> {
        Log.d(TAG, "getStores: GET /api/v1/stores")
        val response = apiService.getStores()
        Log.d(TAG, "getStores: received ${response.stores.size} stores")
        return response.stores.map { it.toDomain() }
    }

    companion object {
        private const val TAG = "RemoteStoreRepo"
    }
}
