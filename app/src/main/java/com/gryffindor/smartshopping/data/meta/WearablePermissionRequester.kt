package com.gryffindor.smartshopping.data.meta

/**
 * Abstraction for requesting DAT wearable permissions from an Activity context.
 *
 * MetaCameraSource calls this before starting a stream if camera permission is not granted.
 * The Activity implements this using [Wearables.RequestPermissionContract].
 *
 * This keeps the ActivityResultLauncher lifecycle inside the Activity
 * while allowing MetaCameraSource to trigger permission requests.
 *
 * All Meta SDK types remain inside data/meta/ boundary.
 */
interface WearablePermissionRequester {

    /**
     * Request DAT camera permission.
     * Suspends until the user grants or denies permission in the Meta AI app.
     *
     * @return true if permission was granted, false if denied or failed
     */
    suspend fun requestCameraPermission(): Boolean
}
