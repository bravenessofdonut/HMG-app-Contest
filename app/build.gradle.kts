plugins {
    id("braveberry.android.application")
    id("braveberry.android.compose")
    id("braveberry.android.hilt")
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

    // 기존 모듈
    implementation(project(":vehicle-data"))
    implementation(project(":vehicle-local"))
    implementation(project(":voice-data"))
    implementation(project(":voice-local"))

    // AI 모듈: 실제 앱에도 포함
    implementation(project(":ai-data"))
    implementation(project(":ai-remote"))

    implementation(libs.androidx.hilt.navigation.compose)

    // 기존 Android 테스트
    androidTestImplementation(project(":domain"))
    androidTestImplementation(libs.hilt.testing)
    kspAndroidTest(libs.hilt.compiler)

    // Firebase AI 테스트
    androidTestImplementation(platform(libs.firebase.bom))
    androidTestImplementation(libs.firebase.ai)

    // Firebase App Check
    androidTestImplementation("com.google.firebase:firebase-appcheck")
    androidTestImplementation("com.google.firebase:firebase-appcheck-debug")

    // AndroidX JUnit
    androidTestImplementation(libs.androidx.junit.ktx)
}

