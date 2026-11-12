import java.util.Properties

plugins {
    id("braveberry.android.library")
    id("braveberry.android.hilt")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val googlePlacesApiKey =
    localProperties.getProperty("google.places.api.key")
        ?: System.getenv("GOOGLE_PLACES_API_KEY")
        ?: ""

android {
    namespace = "com.braveberry.poi.remote"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField(
            "String",
            "GOOGLE_PLACES_API_KEY",
            "\"$googlePlacesApiKey\""
        )
    }
}

dependencies {
    implementation(project(":poi-data"))
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.kotlin)
}