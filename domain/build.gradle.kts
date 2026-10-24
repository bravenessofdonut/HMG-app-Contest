plugins {
    id("braveberry.jvm.library") // java-library, kotlin.jvm, 설정
    id("braveberry.jvm.hilt")    // ksp, javax.inject, dagger, dagger-compiler 설정
}

dependencies{
    implementation(project(":data-resource"))
}