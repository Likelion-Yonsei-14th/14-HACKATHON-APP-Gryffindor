package com.gryffindor.smartshopping.app.navigation

/**
 * Production UI navigation routes.
 * Matches the target IA:
 *   Splash -> Login -> Onboarding -> MainShell (Home/Shop/MyPage)
 */
object ProductionRoutes {
    const val SPLASH = "production_splash"
    const val LOGIN = "production_login"

    // Onboarding flow
    const val ONBOARDING_PERMISSION = "onboarding_permission"
    const val ONBOARDING_USER_INFO = "onboarding_user_info"
    const val ONBOARDING_TERMS = "onboarding_terms"
    const val ONBOARDING_DEVICE_REGISTRATION = "onboarding_device_registration"

    // Main Shell (contains bottom navigation with nested NavHost)
    const val MAIN_SHELL = "main_shell"

    // Trip flight registration (from Home)
    const val TRIP_FLIGHT_REGISTER = "trip_flight_register"
    const val TRIP_FLIGHT_CONFIRM = "trip_flight_confirm/{flightId}"

    // Shopping flow (from Shop tab, navigated via root)
    const val SHOP_STORE_SELECTION = "production_shop_store_selection"
    const val SHOP_DEVICE_CONNECTION = "production_shop_device_connection"
    const val SHOP_READY = "production_shop_ready/{sessionId}/{currency}"
    const val SHOP_LIVE = "production_shop_live/{sessionId}/{currency}"
    const val SHOP_LIST = "production_shop_list/{sessionId}/{currency}"

    // MyPage sub-screens
    const val MY_PAGE_WISHLIST = "production_mypage_wishlist"
    const val MY_PAGE_RECENT = "production_mypage_recent"

    // Helper functions for parameterized routes
    fun tripFlightConfirm(flightId: String) = "trip_flight_confirm/$flightId"
    fun shopReady(sessionId: String, currency: String) = "production_shop_ready/$sessionId/$currency"
    fun shopLive(sessionId: String, currency: String) = "production_shop_live/$sessionId/$currency"
    fun shopList(sessionId: String, currency: String) = "production_shop_list/$sessionId/$currency"
}
