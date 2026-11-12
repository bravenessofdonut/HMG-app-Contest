package com.braveberry.drivingapplication

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DrivingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // FirebaseApp은 FirebaseInitProvider가 onCreate 전에 자동 초기화함.
        // App Check provider는 빌드 타입별 소스 세트(src/debug, src/release)에서 결정함.
        AppCheckSetup.install()

        if (FirebaseApp.getApps(this).isNotEmpty()) return

        val appId = BuildConfig.FIREBASE_APP_ID
        val apiKey = BuildConfig.FIREBASE_API_KEY
        val projectId = BuildConfig.FIREBASE_PROJECT_ID

        if (appId.isBlank() || apiKey.isBlank() || projectId.isBlank()) {
            return
        }

        val options = FirebaseOptions.Builder()
            .setApplicationId(appId)
            .setApiKey(apiKey)
            .setProjectId(projectId)
            .build()

        FirebaseApp.initializeApp(this, options)
    }
}
