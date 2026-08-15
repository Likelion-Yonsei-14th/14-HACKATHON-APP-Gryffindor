package com.gryffindor.smartshopping.app

import com.gryffindor.smartshopping.domain.repository.ChecklistRepository
import com.gryffindor.smartshopping.domain.repository.RecommendationRepository
import com.gryffindor.smartshopping.domain.repository.SessionRepository
import com.gryffindor.smartshopping.domain.repository.ShoppingRepository
import com.gryffindor.smartshopping.domain.repository.TravelRepository

/**
 * Manual DI container.
 * Provides all repository implementations to ViewModels.
 *
 * In A0, Retrofit is defined in NetworkConfig but not instantiated here since
 * all repositories use fake implementations. Uncomment when switching to real
 * repositories in A3:
 *
 * private val retrofit: Retrofit by lazy { NetworkConfig.createRetrofit() }
 * val apiService: ShoppingApiService by lazy { retrofit.create(ShoppingApiService::class.java) }
 */
class AppContainer {

    // Repositories — swap these lines to switch Fake → Real in later stages.
    // Fake implementations will be wired in Task 5.1.
    lateinit var sessionRepository: SessionRepository
    lateinit var shoppingRepository: ShoppingRepository
    lateinit var checklistRepository: ChecklistRepository
    lateinit var recommendationRepository: RecommendationRepository
    lateinit var travelRepository: TravelRepository
}
