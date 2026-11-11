import java.util.Properties

val localProperties = Properties().apply {
    rootProject.file("local.properties").inputStream().use {
        load(it)
    }
}

fun firebaseProperty(key: String): String {
    return localProperties.getProperty(key)
        ?: error("Missing property: $key")
}

plugins {
    id("braveberry.android.application")
    id("braveberry.android.compose")
    id("braveberry.android.hilt")
}

android {
    namespace = "com.braveberry.drivingapplication"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.braveberry.drivingapplication"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.braveberry.drivingapplication.HiltTestRunner"
        buildConfigField(
            "String",
            "FIREBASE_APP_ID",
            "\"${firebaseProperty("firebase.app.id")}\""
        )

        buildConfigField(
            "String",
            "FIREBASE_API_KEY",
            "\"${firebaseProperty("firebase.api.key")}\""
        )

        buildConfigField(
            "String",
            "FIREBASE_PROJECT_ID",
            "\"${firebaseProperty("firebase.project.id")}\""
        )
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


    // AndroidX JUnit
    androidTestImplementation(libs.androidx.junit.ktx)

    // 실제 앱에서 Firebase 초기화
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.common)
}

