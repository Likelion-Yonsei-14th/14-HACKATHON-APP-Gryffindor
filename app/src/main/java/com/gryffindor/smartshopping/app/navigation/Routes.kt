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

    // 하단 네비게이션 바 SHOP/MY PAGE 탭. SHOPPING("shopping/{sessionId}")은 카메라 기반
    // 실시간 쇼핑 세션 화면이라 이름이 겹치지 않도록 별도로 둔다 — SHOP_TAB(매장 선택)과
    // SHOPPING_SESSION(화면 우선 제작한 목업 실시간 쇼핑 플로우)은 아직 실제 카메라/DAT
    // 파이프라인과는 연결되어 있지 않다.
    const val SHOP_TAB = "shopTab"
    const val SHOPPING_SESSION = "shoppingSession"
    const val MY_PAGE = "myPage"

    fun shopping(sessionId: String) = "shopping/$sessionId"
    fun review(sessionId: String) = "review/$sessionId"
    fun travel(sessionId: String) = "travel/$sessionId"
    fun checklist(sessionId: String) = "checklist/$sessionId"
    fun recommendation(sessionId: String) = "recommendation/$sessionId"
}