plugins {
    id("braveberry.android.library") // 기본 안드로이드 설정 (core.ktx, junit 포함)
    id("braveberry.android.compose") // Compose 관련 의존성 싹 다 포함
    id("braveberry.android.hilt")    // Hilt 관련 설정 포함
}

android {
    namespace = "com.braveberry.presentation"
}

dependencies {
    // Hilt 네비게이션
    implementation(libs.androidx.hilt.navigation.compose)
    // Orbit
    implementation(libs.orbit.core)
    implementation(libs.orbit.viewmodel)

    implementation(project(":domain"))
    implementation(project(":data-resource"))
}
