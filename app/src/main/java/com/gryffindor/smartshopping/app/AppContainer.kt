package com.gryffindor.smartshopping.app

import android.content.Context
import com.gryffindor.smartshopping.data.detection.DetectionPipeline
import com.gryffindor.smartshopping.data.meta.MetaCameraSource
import com.gryffindor.smartshopping.data.repository.FakeChecklistRepository
import com.gryffindor.smartshopping.data.repository.FakeRecommendationRepository
import com.gryffindor.smartshopping.data.repository.FakeSessionRepository
import com.gryffindor.smartshopping.data.repository.FakeShoppingRepository
import com.gryffindor.smartshopping.data.repository.FakeTravelRepository
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.detection.DetectionResultProvider
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
class AppContainer(private val applicationContext: Context) {

    // Repositories — swap these lines to switch Fake → Real in later stages.
    val sessionRepository: SessionRepository = FakeSessionRepository()
    val shoppingRepository: ShoppingRepository = FakeShoppingRepository()
    val checklistRepository: ChecklistRepository = FakeChecklistRepository()
    val recommendationRepository: RecommendationRepository = FakeRecommendationRepository()
    val travelRepository: TravelRepository = FakeTravelRepository()

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
}
