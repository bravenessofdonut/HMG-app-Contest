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
    id("braveberry.android.library")
    id("braveberry.android.hilt")
    id("braveberry.android.test")
}
android {
    namespace = "com.braveberry.ai.remote"
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
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

}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.junit.ktx)
    implementation(libs.firebase.ai)
    implementation(project(":ai-data"))
    androidTestImplementation(
        "com.google.firebase:firebase-appcheck"
    )
    androidTestImplementation(
        "com.google.firebase:firebase-appcheck-debug"
    )
}