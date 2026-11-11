package com.braveberry.drivingapplication

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DrivingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // FirebaseApp은 FirebaseInitProvider가 onCreate 전에 자동 초기화함.
        // App Check provider는 빌드 타입별 소스 세트(src/debug, src/release)에서 결정함.
        AppCheckSetup.install()
    }
}
