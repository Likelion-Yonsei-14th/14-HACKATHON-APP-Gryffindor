package com.gryffindor.smartshopping.app.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"

    // 온보딩 화면들
    const val ONBOARDING_PERMISSION = "onboarding/permission"
    const val ONBOARDING_USER_INFO = "onboarding/userInfo"
    const val ONBOARDING_FLIGHT_REGISTER = "onboarding/flightRegister"
    const val ONBOARDING_FLIGHT_CONFIRM = "onboarding/flightConfirm"
    const val ONBOARDING_RECEIPT_REGISTER = "onboarding/receiptRegister"
    const val ONBOARDING_PURCHASE_CONFIRM = "onboarding/purchaseConfirm"

    const val HOME = "home"
    const val SHOPPING = "shopping/{sessionId}"
    const val REVIEW = "review/{sessionId}"
    const val TRAVEL = "travel/{sessionId}"
    const val CHECKLIST = "checklist/{sessionId}"
    const val RECOMMENDATION = "recommendation/{sessionId}"

    fun shopping(sessionId: String) = "shopping/$sessionId"
    fun review(sessionId: String) = "review/$sessionId"
    fun travel(sessionId: String) = "travel/$sessionId"
    fun checklist(sessionId: String) = "checklist/$sessionId"
    fun recommendation(sessionId: String) = "recommendation/$sessionId"
}