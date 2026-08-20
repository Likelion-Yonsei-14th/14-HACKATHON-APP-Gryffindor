package com.gryffindor.smartshopping.app

import android.content.Context
import com.gryffindor.smartshopping.core.network.NetworkConfig
import com.gryffindor.smartshopping.data.attention.AttentionPipeline
import com.gryffindor.smartshopping.data.detection.DetectionPipeline
import com.gryffindor.smartshopping.data.meta.MetaCameraSource
import com.gryffindor.smartshopping.data.remote.api.ShoppingApiService
import com.gryffindor.smartshopping.data.repository.FakeChecklistRepository
import com.gryffindor.smartshopping.data.repository.FakeRecommendationRepository
import com.gryffindor.smartshopping.data.repository.FakeTravelRepository
import com.gryffindor.smartshopping.data.repository.RemoteSessionRepository
import com.gryffindor.smartshopping.data.repository.RemoteShoppingRepository
import com.gryffindor.smartshopping.domain.attention.AttentionCandidateProvider
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.detection.DetectionResultProvider
import com.gryffindor.smartshopping.domain.model.TripDates
import com.gryffindor.smartshopping.domain.repository.ChecklistRepository
import com.gryffindor.smartshopping.domain.repository.RecommendationRepository
import com.gryffindor.smartshopping.domain.repository.SessionRepository
import com.gryffindor.smartshopping.domain.repository.ShoppingRepository
import com.gryffindor.smartshopping.domain.repository.TravelRepository

/**
 * Manual DI container.
 * Provides all repository implementations to ViewModels.
 */
class AppContainer(private val applicationContext: Context) {

    // Network — Retrofit + API service
    private val retrofit by lazy { NetworkConfig.createRetrofit() }
    private val apiService: ShoppingApiService by lazy {
        retrofit.create(ShoppingApiService::class.java)
    }

    // Repositories — SessionRepository and ShoppingRepository now use real Backend (A4).
    val sessionRepository: SessionRepository by lazy { RemoteSessionRepository(apiService) }
    val shoppingRepository: ShoppingRepository by lazy { RemoteShoppingRepository(apiService) }
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

    // A3: Attention pipeline — auto-starts/stops with camera streaming state.
    private val attentionPipeline: AttentionPipeline by lazy {
        AttentionPipeline(
            cameraFrameProvider = cameraFrameProvider,
            detectionResultProvider = detectionResultProvider
        )
    }
    val attentionCandidateProvider: AttentionCandidateProvider get() = attentionPipeline

    // 온보딩에서 확인한 여행 날짜 — 체크리스트 화면 날짜 네비게이터 초기값으로 쓴다.
    // TODO: 실제 세션/서버에 여행 정보가 영속화되면 이 임시 필드는 제거한다.
    var tripDates: TripDates = TripDates()
}
