package com.gryffindor.smartshopping.app

import android.content.Context
import com.gryffindor.smartshopping.core.location.LocationProvider
import com.gryffindor.smartshopping.core.network.NetworkConfig
import com.gryffindor.smartshopping.data.attention.AttentionPipeline
import com.gryffindor.smartshopping.data.detection.DetectionPipeline
import com.gryffindor.smartshopping.data.meta.MetaCameraSource
import com.gryffindor.smartshopping.data.recording.GlassesVideoRecorder
import com.gryffindor.smartshopping.data.remote.api.PersonalizationApiService
import com.gryffindor.smartshopping.data.remote.api.ShoppingApiService
import com.gryffindor.smartshopping.data.repository.RemoteChecklistRepository
import com.gryffindor.smartshopping.data.repository.RemotePersonalizationRepository
import com.gryffindor.smartshopping.data.repository.RemoteRecommendationRepository
import com.gryffindor.smartshopping.data.repository.RemoteSessionRepository
import com.gryffindor.smartshopping.data.repository.RemoteShoppingRepository
import com.gryffindor.smartshopping.data.repository.RemoteStoreRepository
import com.gryffindor.smartshopping.data.repository.RemoteTravelRepository
import com.gryffindor.smartshopping.data.repository.RemoteTripRepository
import com.gryffindor.smartshopping.domain.attention.AttentionCandidateProvider
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.detection.DetectionResultProvider
import com.gryffindor.smartshopping.domain.model.TripDates
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

    // Repositories — all backed by the real Backend API.
    val sessionRepository: SessionRepository by lazy { RemoteSessionRepository(apiService) }
    val shoppingRepository: ShoppingRepository by lazy { RemoteShoppingRepository(apiService) }
    val storeRepository: StoreRepository by lazy { RemoteStoreRepository(apiService) }
    val checklistRepository: ChecklistRepository by lazy { RemoteChecklistRepository(apiService) }
    val recommendationRepository: RecommendationRepository by lazy { RemoteRecommendationRepository(apiService) }
    val travelRepository: TravelRepository by lazy { RemoteTravelRepository(apiService) }

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

    // Recording: Glasses POV session recorder (non-blocking, auxiliary feature).
    val glassesVideoRecorder: GlassesVideoRecorder by lazy {
        GlassesVideoRecorder(applicationContext).also { recorder ->
            metaCameraSource.videoRecorder = recorder
        }
    }

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

    // MainActivity가 앱 시작 시 채워두는 콜백. 로그인 전에 시스템 권한 다이얼로그가 뜨는 문제를
    // 피하려고, MainActivity.onCreate()에서 즉시 권한을 요청하는 대신 온보딩의 접근권한 화면이
    // 실제로 권한을 받은 직후 이 콜백을 호출해서 Wearables SDK 초기화/등록을 진행한다.
    var onRequestWearablesSetup: (() -> Unit)? = null
}
