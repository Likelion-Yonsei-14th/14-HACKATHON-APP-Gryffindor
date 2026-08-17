package com.gryffindor.smartshopping.domain.model

import java.time.LocalDate

/**
 * 온보딩(항공편 정보 확인)에서 사용자가 확인한 여행 날짜.
 * 체크리스트 화면의 날짜 네비게이터가 어떤 날짜부터 보여줄지 초기값으로 쓴다.
 * 실제 세션/서버에 여행 정보를 영속화하기 전까지는 화면 간 공유 저장소가
 * 마땅치 않아 [com.gryffindor.smartshopping.app.AppContainer]가 값 하나만 들고 있는 임시 구조다.
 */
data class TripDates(
    val departureDate: LocalDate? = null,
)
