package com.gryffindor.smartshopping.app

import android.app.Application

class SmartShoppingApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer()
    }
}
