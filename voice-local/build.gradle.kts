plugins {
    id("braveberry.android.library")
    id("braveberry.android.hilt")
}

android { namespace = "com.braveberry.voice.local" }

dependencies {
    implementation(project(":voice-data"))
    implementation(project(":domain"))
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
    implementation(libs.kotlinx.coroutines.core)
}
