package com.gryffindor.smartshopping.data.meta

import com.gryffindor.smartshopping.domain.camera.GlassesUpdateResult

/**
 * Abstraction for opening the DAT glasses app update flow from an Activity context.
 *
 * MetaCameraSource calls this when the DAT glasses app needs updating.
 * The Activity implements this using [Wearables.openDATGlassesAppUpdate].
 *
 * Mirrors the existing [WearablePermissionRequester] pattern for Activity-bound SDK calls.
 *
 * All Meta SDK types remain inside data/meta/ boundary.
 */
fun interface WearableUpdateRequester {
    fun openDatGlassesUpdate(): GlassesUpdateResult
}
