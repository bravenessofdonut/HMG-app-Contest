plugins {
    id("braveberry.android.library")
    id("braveberry.android.hilt")
}

android { namespace = "com.braveberry.vehicle.local" }

dependencies {
    implementation(project(":vehicle-data"))
    implementation(project(":data-resource"))
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation(libs.kotlinx.coroutines.core)
}
