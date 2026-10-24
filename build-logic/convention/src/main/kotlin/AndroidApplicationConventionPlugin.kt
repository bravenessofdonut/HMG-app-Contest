import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = 37

                defaultConfig {
                    minSdk = 34
                    targetSdk = 37
                    testInstrumentationRunner =
                        "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
            }

            val libs = extensions
                .getByType<VersionCatalogsExtension>()
                .named("libs")

            dependencies {
                add(
                    "implementation",
                    libs.findLibrary("androidx.core.ktx").get()
                )
                add(
                    "testImplementation",
                    libs.findLibrary("junit").get()
                )
                add(
                    "androidTestImplementation",
                    libs.findLibrary("androidx.junit").get()
                )
                add(
                    "androidTestImplementation",
                    libs.findLibrary("androidx.espresso.core").get()
                )
            }
        }
    }
}