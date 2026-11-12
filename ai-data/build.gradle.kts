plugins {
    id("braveberry.jvm.library")
    id("braveberry.jvm.hilt")
}
dependencies {
    implementation(project(":domain"))
    implementation(project(":data-resource"))
    implementation(libs.javax.inject)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}