plugins {
    `kotlin-dsl`
}

group = "com.braveberry.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(libs.junit)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        // 1. 기존에 만든 안드로이드 라이브러리용
        register("androidLibrary") {
            id = "braveberry.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        // 2. 앱 모듈용
        register("androidApplication") {
            id = "braveberry.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        // 3. 순수 데이터/도메인 모듈용
        register("jvmLibrary") {
            id = "braveberry.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }

        // 4. Hilt 관련
        register("androidHilt") {
            id = "braveberry.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("jvmHilt") {
            id = "braveberry.jvm.hilt"
            implementationClass = "JvmHiltConventionPlugin"
        }

        // 5. Room 관련
        register("androidRoom") {
            id = "braveberry.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }

        // 6. Retrofit 관련 (안드로이드)
        register("androidRetrofit") {
            id = "braveberry.android.retrofit"
            implementationClass = "AndroidRetrofitConventionPlugin"
        }
        register("jvmRetrofit") {
            id = "braveberry.jvm.retrofit"
            implementationClass = "JvmRetrofitConventionPlugin"
        }

        // 7. Compose 관련 (안드로이드)
        register("androidCompose") {
            id = "braveberry.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
    }
}

tasks.test {
    val catalog = rootProject.file("../gradle/libs.versions.toml")
    inputs.file(catalog)
    systemProperty("guide.versionCatalog", catalog.absolutePath)
}
