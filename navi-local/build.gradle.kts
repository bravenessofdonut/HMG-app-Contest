plugins {
    id("braveberry.android.library")
    id("braveberry.android.hilt")
}

android { namespace = "com.braveberry.navi.local" }

dependencies {
    implementation(project(":navi-data"))
    implementation(libs.pleos.navi)
    implementation(libs.kotlinx.coroutines.core)
}
