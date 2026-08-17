package com.gryffindor.smartshopping.domain.attention

import com.gryffindor.smartshopping.domain.model.AttentionCandidate
import kotlinx.coroutines.flow.Flow

/**
 * SDK-independent domain interface for attention candidate output.
 *
 * Downstream components (A4 Backend integration, ViewModel) depend only on this interface.
 * No Meta DAT, TFLite, or Retrofit types are exposed.
 */
interface AttentionCandidateProvider {
    /** Stream of attention candidates ready for Backend recognition. */
    val candidates: Flow<AttentionCandidate>
}
