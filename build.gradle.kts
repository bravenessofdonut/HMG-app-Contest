// 프로젝트 루트 build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false

    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}