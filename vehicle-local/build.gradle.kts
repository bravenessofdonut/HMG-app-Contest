plugins {
    id("braveberry.android.library")
    id("braveberry.android.hilt")
}

android { namespace = "com.braveberry.vehicle.local" }

dependencies {
    implementation(project(":vehicle-data"))
    implementation(libs.pleos.vehicle)
    implementation(libs.kotlinx.coroutines.core)
}
