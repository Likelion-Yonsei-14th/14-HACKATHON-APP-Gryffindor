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
    const val SHOPPING_RECEIPT = "shopping/receipt/{sessionId}"
    const val REVIEW = "review/{sessionId}"
    const val TRAVEL = "travel/{sessionId}"
    const val CHECKLIST = "checklist/{sessionId}"
    const val RECOMMENDATION = "recommendation/{sessionId}"

    // 하단 네비게이션 바 SHOP 탭. 매장 선택(SHOP_TAB) 확인 시 StoreSelectionViewModel이 실제
    // 세션을 만들고 그 sessionId로 SHOPPING(카메라 기반 실시간 쇼핑, LiveShoppingScreen UI)에
    // 진입한다 — 예전에 있었던 목업 전용 SHOPPING_SESSION 플로우는 실제 세션 플로우에
    // 합쳐지면서 제거됨.
    const val SHOP_TAB = "shopTab"
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
    fun shoppingReceipt(sessionId: String) = "shopping/receipt/$sessionId"
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
