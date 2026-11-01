plugins {
    id("braveberry.android.library")
    id("braveberry.android.hilt")
}

android { namespace = "com.braveberry.voice.local" }

dependencies {
    implementation(project(":voice-data"))
    implementation(project(":domain"))
    implementation(libs.pleos.tts)
    implementation(libs.pleos.stt)
    implementation(libs.kotlinx.coroutines.core)
}
