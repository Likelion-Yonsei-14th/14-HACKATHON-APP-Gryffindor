package com.gryffindor.smartshopping.feature.shopping

import com.gryffindor.smartshopping.core.common.UiState
import com.gryffindor.smartshopping.domain.attention.AttentionCandidateProvider
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.detection.DetectionPipelineState
import com.gryffindor.smartshopping.domain.detection.DetectionResultProvider
import com.gryffindor.smartshopping.domain.model.AttentionCandidate
import com.gryffindor.smartshopping.domain.model.CameraFrame
import com.gryffindor.smartshopping.domain.model.CameraState
import com.gryffindor.smartshopping.domain.model.DetectionFrameResult
import com.gryffindor.smartshopping.domain.model.Observation
import com.gryffindor.smartshopping.domain.model.ObservedProduct
import com.gryffindor.smartshopping.domain.model.Pricing
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.model.RecognitionResult
import com.gryffindor.smartshopping.domain.model.Session
import com.gryffindor.smartshopping.domain.model.SessionProduct
import com.gryffindor.smartshopping.domain.model.SessionStatus
import com.gryffindor.smartshopping.domain.model.TriggerType
import com.gryffindor.smartshopping.domain.repository.SessionRepository
import com.gryffindor.smartshopping.domain.repository.ShoppingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingViewModelRecognitionConcurrencyTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `two distinct candidates run concurrently and a third is dropped`() = runTest {
        val fixture = createFixture()

        fixture.candidates.emit(candidate("A"))
        fixture.candidates.emit(candidate("B"))
        fixture.candidates.emit(candidate("C"))
        runCurrent()

        assertEquals(listOf("A", "B"), fixture.shoppingRepository.startedTrackingIds)
        assertEquals(2, fixture.shoppingRepository.activeRequests)
        assertEquals(2, fixture.shoppingRepository.maxActiveRequests)
        assertFalse(fixture.shoppingRepository.hasStarted("C"))

        fixture.viewModel.endShopping(SESSION_ID)
        runCurrent()
    }

    @Test
    fun `ending session cancels both active requests and returns both slots`() = runTest {
        val fixture = createFixture()
        fixture.candidates.emit(candidate("A"))
        fixture.candidates.emit(candidate("B"))
        runCurrent()
        assertEquals(2, fixture.shoppingRepository.activeRequests)

        fixture.viewModel.endShopping(SESSION_ID)
        runCurrent()

        assertEquals(setOf("A", "B"), fixture.shoppingRepository.cancelledTrackingIds.toSet())
        assertEquals(0, fixture.shoppingRepository.activeRequests)
        assertTrue(fixture.sessionRepository.completeSessionCalled)

        fixture.viewModel.loadProducts("session-2", "USD")
        runCurrent()
        fixture.candidates.emit(candidate("C"))
        fixture.candidates.emit(candidate("D"))
        runCurrent()

        assertTrue(fixture.shoppingRepository.hasStarted("C"))
        assertTrue(fixture.shoppingRepository.hasStarted("D"))
        assertEquals(2, fixture.shoppingRepository.activeRequests)

        fixture.viewModel.endShopping("session-2")
        runCurrent()
    }

    @Test
    fun `concurrent matches for the same product add one Product Card`() = runTest {
        val fixture = createFixture()
        fixture.candidates.emit(candidate("A"))
        fixture.candidates.emit(candidate("B"))
        runCurrent()

        fixture.shoppingRepository.complete("A", matched("product-1"))
        fixture.shoppingRepository.complete("B", matched("product-1"))
        runCurrent()

        val state = fixture.viewModel.uiState.value as UiState.Success
        assertEquals(1, state.data.products.size)
        assertEquals("product-1", state.data.products.single().product.productId)

        fixture.viewModel.endShopping(SESSION_ID)
        runCurrent()
    }

    @Test
    fun `same tracking object is not recognized concurrently`() = runTest {
        val fixture = createFixture()

        fixture.candidates.emit(candidate("same-object"))
        fixture.candidates.emit(candidate("same-object"))
        runCurrent()

        assertEquals(listOf("same-object"), fixture.shoppingRepository.startedTrackingIds)
        assertEquals(1, fixture.shoppingRepository.activeRequests)

        fixture.viewModel.endShopping(SESSION_ID)
        runCurrent()
    }

    @Test
    fun `failed request returns its slot immediately for an independent candidate`() = runTest {
        val fixture = createFixture()
        fixture.candidates.emit(candidate("A"))
        fixture.candidates.emit(candidate("B"))
        runCurrent()

        fixture.shoppingRepository.fail("A")
        runCurrent()

        fixture.candidates.emit(candidate("C"))
        runCurrent()

        assertTrue(fixture.shoppingRepository.hasStarted("C"))
        assertEquals(2, fixture.shoppingRepository.activeRequests)
        assertEquals(2, fixture.shoppingRepository.maxActiveRequests)

        fixture.viewModel.endShopping(SESSION_ID)
        runCurrent()
    }

    @Test
    fun `completed request frees its slot without a global cooldown`() = runTest {
        val fixture = createFixture()
        fixture.candidates.emit(candidate("A"))
        fixture.candidates.emit(candidate("B"))
        runCurrent()

        fixture.shoppingRepository.complete("A", RecognitionResult.Unknown)
        runCurrent()
        fixture.candidates.emit(candidate("C"))
        runCurrent()

        assertEquals(listOf("A", "B", "C"), fixture.shoppingRepository.startedTrackingIds)
        assertEquals(2, fixture.shoppingRepository.activeRequests)
        assertEquals(2, fixture.shoppingRepository.maxActiveRequests)

        fixture.viewModel.endShopping(SESSION_ID)
        runCurrent()
    }

    private suspend fun createFixture(): Fixture {
        val candidates = FakeAttentionCandidateProvider()
        val shoppingRepository = ControlledShoppingRepository()
        val sessionRepository = FakeSessionRepository()
        val viewModel = ShoppingViewModel(
            shoppingRepository = shoppingRepository,
            sessionRepository = sessionRepository,
            cameraFrameProvider = NoOpCameraFrameProvider(),
            detectionResultProvider = NoOpDetectionResultProvider(),
            attentionCandidateProvider = candidates
        )

        // Start collectors before emitting and put the ViewModel in an active loaded session.
        kotlinx.coroutines.yield()
        viewModel.loadProducts(SESSION_ID, "USD")
        kotlinx.coroutines.yield()
        return Fixture(viewModel, candidates, shoppingRepository, sessionRepository)
    }

    private data class Fixture(
        val viewModel: ShoppingViewModel,
        val candidates: FakeAttentionCandidateProvider,
        val shoppingRepository: ControlledShoppingRepository,
        val sessionRepository: FakeSessionRepository
    )

    private class FakeAttentionCandidateProvider : AttentionCandidateProvider {
        private val flow = MutableSharedFlow<AttentionCandidate>(extraBufferCapacity = 16)
        override val candidates: Flow<AttentionCandidate> = flow

        fun emit(candidate: AttentionCandidate) {
            check(flow.tryEmit(candidate))
        }
    }

    private class ControlledShoppingRepository : ShoppingRepository {
        val startedTrackingIds = mutableListOf<String>()
        val cancelledTrackingIds = mutableListOf<String>()
        private val responses = mutableMapOf<String, CompletableDeferred<RecognitionResult>>()
        var activeRequests: Int = 0
            private set
        var maxActiveRequests: Int = 0
            private set

        override suspend fun getProducts(sessionId: String): List<SessionProduct> = emptyList()

        override suspend fun submitReview(
            sessionId: String,
            purchasedProductIds: List<String>,
            interestedProductIds: List<String>
        ) = Unit

        override suspend fun recognize(
            sessionId: String,
            candidate: AttentionCandidate
        ): RecognitionResult {
            val trackingId = requireNotNull(candidate.trackingId)
            val response = CompletableDeferred<RecognitionResult>()
            responses[trackingId] = response
            startedTrackingIds += trackingId
            activeRequests++
            maxActiveRequests = maxOf(maxActiveRequests, activeRequests)

            return try {
                response.await()
            } catch (e: CancellationException) {
                cancelledTrackingIds += trackingId
                throw e
            } finally {
                activeRequests--
            }
        }

        fun complete(trackingId: String, result: RecognitionResult) {
            checkNotNull(responses[trackingId]).complete(result)
        }

        fun fail(trackingId: String) {
            checkNotNull(responses[trackingId]).completeExceptionally(IllegalStateException("failed"))
        }

        fun hasStarted(trackingId: String): Boolean = trackingId in startedTrackingIds
    }

    private class FakeSessionRepository : SessionRepository {
        var completeSessionCalled: Boolean = false
            private set

        override suspend fun createSession(currency: String): Session = Session(
            sessionId = SESSION_ID,
            status = SessionStatus.ACTIVE,
            currency = currency,
            startedAt = "2026-08-17T00:00:00Z"
        )

        override suspend fun completeSession(sessionId: String) {
            completeSessionCalled = true
        }
    }

    private class NoOpCameraFrameProvider : CameraFrameProvider {
        override val frames: Flow<CameraFrame> = emptyFlow()
        override val cameraState: StateFlow<CameraState> =
            MutableStateFlow(CameraState.NotConnected)

        override suspend fun startCamera() = Unit
        override suspend fun stopCamera() = Unit
    }

    private class NoOpDetectionResultProvider : DetectionResultProvider {
        override val detections: Flow<DetectionFrameResult> = emptyFlow()
        override val pipelineState: StateFlow<DetectionPipelineState> =
            MutableStateFlow(DetectionPipelineState.Idle)
    }

    private fun candidate(trackingId: String) = AttentionCandidate(
        jpegBytes = byteArrayOf(1, 2, 3),
        capturedAt = "2026-08-17T00:00:00Z",
        triggerType = TriggerType.DWELL,
        occupancyRatio = 0.25f,
        dwellMs = 1_500L,
        trackingId = trackingId,
        cropWidth = 100,
        cropHeight = 100
    )

    private fun matched(productId: String) = RecognitionResult.Matched(
        isNew = true,
        observedProduct = ObservedProduct(
            product = Product(
                productId = productId,
                sku = null,
                brand = "Test Brand",
                name = "Test Product",
                category = "test",
                imageUrl = null
            ),
            pricing = Pricing(
                retailPriceKrw = 10_000,
                estimatedRefundKrw = 1_000,
                estimatedRefundPriceKrw = 9_000,
                convertedRetailPrice = null,
                convertedEstimatedRefund = null,
                convertedEstimatedRefundPrice = null,
                convertedAmount = null,
                convertedCurrency = null,
                instantRefundEligible = false,
                pricingMode = null
            ),
            observation = Observation(
                triggerType = TriggerType.DWELL,
                occupancyRatio = 0.25,
                dwellMs = 1_500L,
                firstObservedAt = "2026-08-17T00:00:00Z",
                lastObservedAt = "2026-08-17T00:00:01Z"
            )
        )
    )

    private companion object {
        const val SESSION_ID = "session-1"
    }
}
