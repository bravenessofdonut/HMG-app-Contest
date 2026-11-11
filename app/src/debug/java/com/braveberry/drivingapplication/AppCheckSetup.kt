package com.braveberry.drivingapplication

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/** 디버그 빌드: 디버그 토큰 provider. 첫 실행 시 logcat에 찍히는 토큰을 Firebase 콘솔에 등록해야 함. */
internal object AppCheckSetup {
    fun install() {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
