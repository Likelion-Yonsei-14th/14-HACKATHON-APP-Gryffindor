package com.gryffindor.smartshopping.app.navigation

/**
 * Production UI navigation routes.
 * These are the new navigation destinations for the Production UI shell.
 * Existing Routes are preserved and accessible from within Production screens.
 */
object ProductionRoutes {
    const val SPLASH = "production_splash"
    const val LOGIN = "production_login"

    // Onboarding
    const val ONBOARDING_PERMISSION = "onboarding_permission"
    const val ONBOARDING_USER_INFO = "onboarding_user_info"
    const val ONBOARDING_FLIGHT_REGISTER = "onboarding_flight_register"
    const val ONBOARDING_FLIGHT_CONFIRM = "onboarding_flight_confirm/{flightId}"

    // Main Shell (contains bottom navigation)
    const val MAIN_SHELL = "main_shell"

    // Tabs within Main Shell are handled internally, not as top-level routes.
    // Shopping live flow accessed from SHOP tab:
    const val SHOPPING_STORE_SELECT = "production_store_select"
    const val SHOPPING_LIVE = "production_shopping_live/{sessionId}/{currency}"

    fun onboardingFlightConfirm(flightId: String) = "onboarding_flight_confirm/$flightId"
    fun shoppingLive(sessionId: String, currency: String) = "production_shopping_live/$sessionId/$currency"
}
