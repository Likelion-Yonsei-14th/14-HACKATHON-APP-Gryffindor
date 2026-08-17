package com.gryffindor.smartshopping.domain.repository

import com.gryffindor.smartshopping.domain.model.Store

/**
 * Repository for fetching available stores from the Backend.
 */
interface StoreRepository {
    suspend fun getStores(): List<Store>
}
