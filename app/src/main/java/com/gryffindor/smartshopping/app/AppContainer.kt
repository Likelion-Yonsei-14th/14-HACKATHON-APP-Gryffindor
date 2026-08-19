package com.gryffindor.smartshopping.app

import android.content.Context
import com.gryffindor.smartshopping.core.location.LocationProvider
import com.gryffindor.smartshopping.core.network.NetworkConfig
import com.gryffindor.smartshopping.data.attention.AttentionPipeline
import com.gryffindor.smartshopping.data.detection.DetectionPipeline
import com.gryffindor.smartshopping.data.meta.MetaCameraSource
import com.gryffindor.smartshopping.data.remote.api.PersonalizationApiService
import com.gryffindor.smartshopping.data.remote.api.ShoppingApiService
import com.gryffindor.smartshopping.data.repository.FakeChecklistRepository
import com.gryffindor.smartshopping.data.repository.FakeRecommendationRepository
import com.gryffindor.smartshopping.data.repository.FakeTravelRepository
import com.gryffindor.smartshopping.data.repository.RemotePersonalizationRepository
import com.gryffindor.smartshopping.data.repository.RemoteSessionRepository
import com.gryffindor.smartshopping.data.repository.RemoteShoppingRepository
import com.gryffindor.smartshopping.data.repository.RemoteStoreRepository
import com.gryffindor.smartshopping.data.repository.RemoteTripRepository
import com.gryffindor.smartshopping.domain.attention.AttentionCandidateProvider
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.detection.DetectionResultProvider
import com.gryffindor.smartshopping.domain.repository.ChecklistRepository
import com.gryffindor.smartshopping.domain.repository.PersonalizationRepository
import com.gryffindor.smartshopping.domain.repository.RecommendationRepository
import com.gryffindor.smartshopping.domain.repository.SessionRepository
import com.gryffindor.smartshopping.domain.repository.ShoppingRepository
import com.gryffindor.smartshopping.domain.repository.StoreRepository
import com.gryffindor.smartshopping.domain.repository.TravelRepository
import com.gryffindor.smartshopping.domain.repository.TripRepository

/**
 * Manual DI container.
 * Provides all repository implementations to ViewModels.
 */
class AppContainer(private val applicationContext: Context) {

    // Network — Retrofit + API services
    private val retrofit by lazy { NetworkConfig.createRetrofit() }
    private val apiService: ShoppingApiService by lazy {
        retrofit.create(ShoppingApiService::class.java)
    }
    private val personalizationApiService: PersonalizationApiService by lazy {
        retrofit.create(PersonalizationApiService::class.java)
    }

    // Repositories — SessionRepository, ShoppingRepository, and StoreRepository use real Backend.
    val sessionRepository: SessionRepository by lazy { RemoteSessionRepository(apiService) }
    val shoppingRepository: ShoppingRepository by lazy { RemoteShoppingRepository(apiService) }
    val storeRepository: StoreRepository by lazy { RemoteStoreRepository(apiService) }
    val checklistRepository: ChecklistRepository = FakeChecklistRepository()
    val recommendationRepository: RecommendationRepository = FakeRecommendationRepository()
    val travelRepository: TravelRepository = FakeTravelRepository()

    // B5/B6: Personalization & Trip repositories using real Backend.
    val personalizationRepository: PersonalizationRepository by lazy {
        RemotePersonalizationRepository(personalizationApiService)
    }
    val tripRepository: TripRepository by lazy {
        RemoteTripRepository(personalizationApiService)
    }

    // A8: One-shot location provider for Feed API (not stored long-term).
    val locationProvider: LocationProvider by lazy { LocationProvider(applicationContext) }

    // A1: Camera input — SDK-independent boundary exposed to ViewModels.
    val metaCameraSource: MetaCameraSource by lazy { MetaCameraSource() }
    val cameraFrameProvider: CameraFrameProvider get() = metaCameraSource

    // A2: Detection pipeline — auto-starts/stops with camera streaming state.
    private val detectionPipeline: DetectionPipeline by lazy {
        DetectionPipeline(
            cameraFrameProvider = cameraFrameProvider,
            context = applicationContext
        )
    }
    val detectionResultProvider: DetectionResultProvider get() = detectionPipeline

    // A3: Attention pipeline — auto-starts/stops with camera streaming state.
    private val attentionPipeline: AttentionPipeline by lazy {
        AttentionPipeline(
            cameraFrameProvider = cameraFrameProvider,
            detectionResultProvider = detectionResultProvider
        )
    }
    val attentionCandidateProvider: AttentionCandidateProvider get() = attentionPipeline
}
