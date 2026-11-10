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
    id("com.google.gms.google-services")
}

android {
    namespace = "com.braveberry.drivingapplication"

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
    buildFeatures {
        buildConfig = true
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
    implementation(project(":ai-remote"))
    implementation(libs.androidx.hilt.navigation.compose)
    androidTestImplementation(project(":domain"))
    androidTestImplementation(libs.hilt.testing)
    kspAndroidTest(libs.hilt.compiler)



    implementation(platform(libs.firebase.bom))

    // 실제 앱의 Debug 빌드에 포함
    debugImplementation(
        "com.google.firebase:firebase-appcheck-debug"
    )

    // Instrumentation Test
    androidTestImplementation(platform(libs.firebase.bom))
    androidTestImplementation(libs.firebase.ai)

    androidTestImplementation(
        "com.google.firebase:firebase-appcheck-debug"
    )
}
