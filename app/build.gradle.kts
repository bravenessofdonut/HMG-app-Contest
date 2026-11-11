plugins {
    id("braveberry.android.application")
    id("braveberry.android.compose")
    id("braveberry.android.hilt")
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.braveberry.drivingapplication"

    defaultConfig {
        applicationId = "com.braveberry.drivingapplication"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.braveberry.drivingapplication.HiltTestRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
}

dependencies {
    implementation(project(":presentation"))
    // Composition root: 구현 모듈을 최종 APK/Hilt classpath에 포함함.
    implementation(project(":vehicle-data"))
    implementation(project(":vehicle-local"))
    implementation(project(":voice-data"))
    implementation(project(":voice-local"))
    implementation(project(":voice-remote"))

    implementation(platform(libs.firebase.bom))
    // 디버그 provider는 디버그 토큰으로 App Check를 우회하므로 release APK에 포함하지 않음.
    debugImplementation(libs.firebase.appcheck.debug)

    implementation(libs.androidx.hilt.navigation.compose)
    androidTestImplementation(project(":domain"))
    androidTestImplementation(libs.hilt.testing)
    kspAndroidTest(libs.hilt.compiler)
}
