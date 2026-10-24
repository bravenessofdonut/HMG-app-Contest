import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    buildFeatures {
                        compose = true
                    }
                }

                configureCompose()
            }

            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> {
                    buildFeatures {
                        compose = true
                    }
                }

                configureCompose()
            }
        }
    }

    private fun Project.configureCompose() {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val libs = extensions
            .getByType<VersionCatalogsExtension>()
            .named("libs")

        dependencies {
            val bom = libs.findLibrary("androidx.compose.bom").get()

            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))

            // Compose 기본 UI
            add(
                "implementation",
                libs.findLibrary("androidx.compose.ui").get()
            )
            add(
                "implementation",
                libs.findLibrary("androidx.compose.ui.graphics").get()
            )
            add(
                "implementation",
                libs.findLibrary("androidx.compose.ui.tooling.preview").get()
            )
            add(
                "implementation",
                libs.findLibrary("androidx.compose.material3").get()
            )

            // Activity / ViewModel / Lifecycle
            add(
                "implementation",
                libs.findLibrary("androidx.activity.compose").get()
            )
            add(
                "implementation",
                libs.findLibrary("androidx.lifecycle.viewmodel.compose").get()
            )
            add(
                "implementation",
                libs.findLibrary("androidx.lifecycle.runtime.ktx").get()
            )

            // 디버그 / 미리보기
            add(
                "debugImplementation",
                libs.findLibrary("androidx.compose.ui.tooling").get()
            )
            add(
                "debugImplementation",
                libs.findLibrary("androidx.compose.ui.test.manifest").get()
            )

            // Compose UI 테스트
            add(
                "androidTestImplementation",
                libs.findLibrary("androidx.compose.ui.test.junit4").get()
            )
        }
    }
}