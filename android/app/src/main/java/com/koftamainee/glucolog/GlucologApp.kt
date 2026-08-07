package com.koftamainee.glucolog

import android.app.Application
import com.koftamainee.glucolog.di.AppContainer

class GlucologApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
