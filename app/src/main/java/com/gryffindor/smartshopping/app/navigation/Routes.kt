package com.gryffindor.smartshopping.app.navigation

object Routes {
    const val HOME = "home"
    const val STORE_SELECTION = "store_selection/{currency}"
    const val SHOPPING = "shopping/{sessionId}/{currency}"
    const val REVIEW = "review/{sessionId}"
    const val TRAVEL = "travel/{sessionId}"
    const val CHECKLIST = "checklist/{sessionId}"
    const val RECOMMENDATION = "recommendation/{sessionId}"

    fun storeSelection(currency: String) = "store_selection/$currency"
    fun shopping(sessionId: String, currency: String) = "shopping/$sessionId/$currency"
    fun review(sessionId: String) = "review/$sessionId"
    fun travel(sessionId: String) = "travel/$sessionId"
    fun checklist(sessionId: String) = "checklist/$sessionId"
    fun recommendation(sessionId: String) = "recommendation/$sessionId"
}
