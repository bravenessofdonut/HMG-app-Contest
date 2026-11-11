plugins {
    id("braveberry.android.library")
    id("braveberry.android.hilt")
}

android {
    namespace = "com.braveberry.voice.remote"
}

dependencies {
    implementation(project(":domain"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
