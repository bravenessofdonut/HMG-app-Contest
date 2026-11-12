import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")

    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun firebaseProperty(key: String): String =
    localProperties.getProperty(key)
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv(key.uppercase().replace('.', '_'))
        ?: ""

plugins {
    id("braveberry.android.application")
    id("braveberry.android.compose")
    id("braveberry.android.hilt")
    alias(libs.plugins.google.services)
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
    implementation(project(":voice-remote"))

    implementation(platform(libs.firebase.bom))
    // 디버그 provider는 디버그 토큰으로 App Check를 우회하므로 release APK에 포함하지 않음.
    debugImplementation(libs.firebase.appcheck.debug)

    implementation(project(":navi-data"))
    implementation(project(":navi-local"))

    // AI 모듈: 실제 앱에도 포함
    implementation(project(":ai-data"))
    implementation(project(":ai-remote"))

    implementation(project(":location-data"))
    implementation(project(":location-local"))
    implementation(libs.androidx.hilt.navigation.compose)

    // 기존 Android 테스트
    androidTestImplementation(project(":domain"))
    androidTestImplementation(libs.hilt.testing)
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation(libs.kotlinx.coroutines.core)
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

