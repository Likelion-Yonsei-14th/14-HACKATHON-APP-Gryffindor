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
    const val STORE_SELECTION = "store_selection/{currency}"
    const val SHOPPING = "shopping/{sessionId}/{currency}"
    const val REVIEW = "review/{sessionId}"
    const val TRAVEL = "travel/{sessionId}"
    const val CHECKLIST = "checklist/{sessionId}"
    const val RECOMMENDATION = "recommendation/{sessionId}"

    // 하단 네비게이션 바 SHOP/MY PAGE 탭. SHOPPING("shopping/{sessionId}/{currency}")은 카메라 기반
    // 실시간 쇼핑 세션 화면이라 이름이 겹치지 않도록 별도로 둔다 — SHOP_TAB(매장 선택)과
    // SHOPPING_SESSION(화면 우선 제작한 목업 실시간 쇼핑 플로우)은 아직 실제 카메라/DAT
    // 파이프라인과는 연결되어 있지 않다. STORE_SELECTION(백엔드가 만든 실제 매장 선택 화면)과
    // 역할이 겹치는지는 다음 단계에서 다시 정리한다.
    const val SHOP_TAB = "shopTab"
    const val SHOPPING_SESSION = "shoppingSession"
    const val MY_PAGE = "myPage"
    const val WISHLIST = "wishlist"

    // Trip flow
    const val TRIP_LIST = "trip_list"
    const val TRIP_CREATE = "trip_create"
    const val TRIP_DETAIL = "trip_detail/{tripId}"
    const val FLIGHT_EDIT = "flight_edit/{tripId}/{flightId}"
    const val HOTEL_EDIT = "hotel_edit/{tripId}"

    // Visit reservation flow
    const val VISIT_RESERVATION = "visit_reservation/{tripId}/{storeId}/{storeName}"
    const val RESERVATION_LIST = "reservation_list/{tripId}"

    fun storeSelection(currency: String) = "store_selection/$currency"
    fun shopping(sessionId: String) = "shopping/$sessionId"
    fun shopping(sessionId: String, currency: String) = "shopping/$sessionId/$currency"
    fun review(sessionId: String) = "review/$sessionId"
    fun travel(sessionId: String) = "travel/$sessionId"
    fun checklist(sessionId: String) = "checklist/$sessionId"
    fun recommendation(sessionId: String) = "recommendation/$sessionId"
    fun tripDetail(tripId: String) = "trip_detail/$tripId"
    fun flightEdit(tripId: String, flightId: String) = "flight_edit/$tripId/$flightId"
    fun hotelEdit(tripId: String) = "hotel_edit/$tripId"
    fun visitReservation(tripId: String, storeId: String, storeName: String) =
        "visit_reservation/$tripId/$storeId/${java.net.URLEncoder.encode(storeName, "UTF-8")}"
    fun reservationList(tripId: String) = "reservation_list/$tripId"
}
