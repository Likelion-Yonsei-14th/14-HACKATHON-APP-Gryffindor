package com.gryffindor.smartshopping.domain.camera

/**
 * Result of attempting to open the DAT glasses app update flow.
 *
 * No Meta SDK types are exposed through this sealed class.
 */
sealed class GlassesUpdateResult {
    /** Update flow launched successfully. */
    data object Success : GlassesUpdateResult()

    /** Update flow failed to launch. [reason] describes the navigation error. */
    data class Failed(val reason: String) : GlassesUpdateResult()

    /** Update flow not supported (no Activity context or requester available). */
    data object Unsupported : GlassesUpdateResult()
}
