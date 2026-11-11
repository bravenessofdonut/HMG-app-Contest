plugins {
    id("braveberry.android.library")
    id("braveberry.android.hilt")
}

android { namespace = "com.braveberry.location.local" }

dependencies {
    implementation(project(":location-data"))
    implementation(libs.pleos.fused.location)
    implementation(libs.kotlinx.coroutines.core)
}
