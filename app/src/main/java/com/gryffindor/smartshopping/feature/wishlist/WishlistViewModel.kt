package com.gryffindor.smartshopping.feature.wishlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.repository.PersonalizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for managing wishlist state across Feed and Shopping screens.
 * Loads the full wishlist on init and exposes productId-based lookup for toggle UI.
 */
class WishlistViewModel(
    private val personalizationRepository: PersonalizationRepository
) : ViewModel() {

    companion object {
        private const val TAG = "WishlistVM"
    }

    private val _wishlistIds = MutableStateFlow<Set<String>>(emptySet())
    val wishlistIds: StateFlow<Set<String>> = _wishlistIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Track in-flight operations to prevent double-tap
    private val inFlightOps = mutableSetOf<String>()

    fun loadWishlist() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val products = personalizationRepository.getWishlist()
                _wishlistIds.value = products.map { it.productId }.toSet()
                Log.d(TAG, "Wishlist loaded: ${_wishlistIds.value.size} items")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load wishlist", e)
                _error.value = "위시리스트를 불러오지 못했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleWishlist(productId: String) {
        if (inFlightOps.contains(productId)) return
        inFlightOps.add(productId)

        val isCurrentlyWishlisted = _wishlistIds.value.contains(productId)

        viewModelScope.launch {
            try {
                if (isCurrentlyWishlisted) {
                    personalizationRepository.deleteWishlist(productId)
                    _wishlistIds.update { it - productId }
                    Log.d(TAG, "Removed from wishlist: $productId")
                } else {
                    personalizationRepository.addWishlist(productId)
                    _wishlistIds.update { it + productId }
                    Log.d(TAG, "Added to wishlist: $productId")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Wishlist toggle failed for $productId", e)
                _error.value = if (isCurrentlyWishlisted) {
                    "위시리스트 삭제에 실패했습니다."
                } else {
                    "위시리스트 추가에 실패했습니다."
                }
            } finally {
                inFlightOps.remove(productId)
            }
        }
    }

    fun isWishlisted(productId: String): Boolean {
        return _wishlistIds.value.contains(productId)
    }

    fun clearError() {
        _error.value = null
    }

    class Factory(
        private val personalizationRepository: PersonalizationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WishlistViewModel(personalizationRepository) as T
        }
    }
}
