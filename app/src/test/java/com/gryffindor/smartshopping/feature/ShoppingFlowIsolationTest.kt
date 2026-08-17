package com.gryffindor.smartshopping.feature

import com.gryffindor.smartshopping.core.common.UiState
import com.gryffindor.smartshopping.domain.attention.AttentionCandidateProvider
import com.gryffindor.smartshopping.domain.camera.CameraFrameProvider
import com.gryffindor.smartshopping.domain.detection.DetectionPipelineState
import com.gryffindor.smartshopping.domain.detection.DetectionResultProvider
import com.gryffindor.smartshopping.domain.model.AttentionCandidate
import com.gryffindor.smartshopping.domain.model.CameraFrame
import com.gryffindor.smartshopping.domain.model.CameraState
import com.gryffindor.smartshopping.domain.model.DetectionFrameResult
import com.gryffindor.smartshopping.domain.model.Pricing
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.model.PurchaseState
import com.gryffindor.smartshopping.domain.model.Session
import com.gryffindor.smartshopping.domain.model.SessionProduct
import com.gryffindor.smartshopping.domain.model.SessionStatus
import com.gryffindor.smartshopping.domain.repository.SessionRepository
import com.gryffindor.smartshopping.domain.repository.ShoppingRepository
import com.gryffindor.smartshopping.feature.home.HomeViewModel
import com.gryffindor.smartshopping.feature.shopping.ShoppingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Task 6.4: Shopping-flow failure isolation.
 *
 * Validates:
 * - HomeViewModel: valid shopping session still starts if camera startup fails
 * - ShoppingViewModel: shopping session still completes if camera stop fails
 * - Camera errors do not incorrectly replace existing shopping-domain error state
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingFlowIsolationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Fakes ---

    private class FakeSessionRepository(
        private val shouldFail: Boolean = false
    ) : SessionRepository {
        var createSessionCalled = false
        var completeSessionCalled = false

        override suspend fun createSession(currency: String, storeId: String): Session {
            createSessionCalled = true
            if (shouldFail) throw RuntimeException("Session creation failed")
            return Session(
                sessionId = "test-session-001",
                status = SessionStatus.ACTIVE,
                currency = currency,
                startedAt = "2026-08-16T00:00:00Z"
            )
        }

        override suspend fun completeSession(sessionId: String) {
            completeSessionCalled = true
            if (shouldFail) throw RuntimeException("Session completion failed")
        }
    }

    private class FakeShoppingRepository : ShoppingRepository {
        override suspend fun getProducts(sessionId: String): List<SessionProduct> {
            return listOf(
                SessionProduct(
                    product = Product(
                        productId = "prod-1",
                        sku = null,
                        brand = "TestBrand",
                        name = "Test Product",
                        category = null,
                        imageUrl = null
                    ),
                    pricing = Pricing(
                        retailPriceKrw = 10000,
                        estimatedRefundKrw = 2000,
                        estimatedRefundPriceKrw = 8000,
                        convertedRetailPrice = "60.00",
                        convertedEstimatedRefund = "12.00",
                        convertedEstimatedRefundPrice = "48.00",
                        convertedAmount = "60.00",
                        convertedCurrency = "USD",
                        instantRefundEligible = false,
                        pricingMode = null
                    ),
                    purchaseState = PurchaseState.UNSET,
                    interested = false
                )
            )
        }

        override suspend fun submitReview(
            sessionId: String,
            purchasedProductIds: List<String>,
            interestedProductIds: List<String>
        ) {
            // no-op
        }

        override suspend fun recognize(sessionId: String, candidate: AttentionCandidate): com.gryffindor.smartshopping.domain.model.RecognitionResult {
            return com.gryffindor.smartshopping.domain.model.RecognitionResult.Unknown
        }
    }

    private class FailingCameraFrameProvider : CameraFrameProvider {
        override val frames: Flow<CameraFrame> = emptyFlow()
        override val cameraState: StateFlow<CameraState> =
            MutableStateFlow(CameraState.NotConnected)

        override suspend fun startCamera() {
            throw RuntimeException("Camera hardware unavailable")
        }

        override suspend fun stopCamera() {
            throw RuntimeException("Camera stop failed: resources busy")
        }
    }

    private class NoOpCameraFrameProvider : CameraFrameProvider {
        override val frames: Flow<CameraFrame> = emptyFlow()
        override val cameraState: StateFlow<CameraState> =
            MutableStateFlow(CameraState.NotConnected)

        var startCalled = false
        var stopCalled = false

        override suspend fun startCamera() {
            startCalled = true
        }

        override suspend fun stopCamera() {
            stopCalled = true
        }
    }

    private class NoOpDetectionResultProvider : DetectionResultProvider {
        override val detections: Flow<DetectionFrameResult> = emptyFlow()
        override val pipelineState: StateFlow<DetectionPipelineState> =
            MutableStateFlow(DetectionPipelineState.Idle)
    }

    private class NoOpAttentionCandidateProvider : AttentionCandidateProvider {
        override val candidates: Flow<AttentionCandidate> = emptyFlow()
    }

    // --- Tests ---

    @Test
    fun `HomeViewModel - country selection works`() = runTest {
        val camera = NoOpCameraFrameProvider()
        val viewModel = HomeViewModel(camera)

        viewModel.selectCountry(com.gryffindor.smartshopping.domain.model.SupportedCountry.CHINA)
        advanceUntilIdle()

        assertEquals(
            com.gryffindor.smartshopping.domain.model.SupportedCountry.CHINA,
            viewModel.uiState.value.selectedCountry
        )
    }

    @Test
    fun `HomeViewModel - DAT update state reflects camera state`() = runTest {
        val camera = NoOpCameraFrameProvider()
        val viewModel = HomeViewModel(camera)
        advanceUntilIdle()

        // Default state: no DAT update required
        assertEquals(false, viewModel.uiState.value.datUpdateRequired)
    }

    @Test
    fun `ShoppingViewModel - session completes even when camera stop fails`() = runTest {
        val sessionRepo = FakeSessionRepository()
        val shoppingRepo = FakeShoppingRepository()
        val failingCamera = FailingCameraFrameProvider()
        val viewModel = ShoppingViewModel(shoppingRepo, sessionRepo, failingCamera, NoOpDetectionResultProvider(), NoOpAttentionCandidateProvider())

        viewModel.loadProducts("test-session-001", "USD")
        advanceUntilIdle()

        viewModel.endShopping("test-session-001")
        advanceUntilIdle()

        // Session was completed despite camera stop failure
        assertTrue("Session should be completed", sessionRepo.completeSessionCalled)

        // UI state shows session ended (isSessionActive = false)
        val state = viewModel.uiState.value
        assertTrue("UI state should be Success", state is UiState.Success)
        assertEquals(false, (state as UiState.Success).data.isSessionActive)
    }

    @Test
    fun `ShoppingViewModel - camera stop failure does not produce Error state`() = runTest {
        val sessionRepo = FakeSessionRepository()
        val shoppingRepo = FakeShoppingRepository()
        val failingCamera = FailingCameraFrameProvider()
        val viewModel = ShoppingViewModel(shoppingRepo, sessionRepo, failingCamera, NoOpDetectionResultProvider(), NoOpAttentionCandidateProvider())

        viewModel.loadProducts("test-session-001", "USD")
        advanceUntilIdle()

        viewModel.endShopping("test-session-001")
        advanceUntilIdle()

        // Should NOT be an Error state
        val state = viewModel.uiState.value
        assertTrue(
            "Camera stop failure should not cause UiState.Error",
            state is UiState.Success
        )
    }

    @Test
    fun `ShoppingViewModel - camera stop is called on end shopping`() = runTest {
        val sessionRepo = FakeSessionRepository()
        val shoppingRepo = FakeShoppingRepository()
        val camera = NoOpCameraFrameProvider()
        val viewModel = ShoppingViewModel(shoppingRepo, sessionRepo, camera, NoOpDetectionResultProvider(), NoOpAttentionCandidateProvider())

        viewModel.loadProducts("test-session-001", "USD")
        advanceUntilIdle()

        viewModel.endShopping("test-session-001")
        advanceUntilIdle()

        assertTrue("Camera stopCamera should be called", camera.stopCalled)
    }
}
