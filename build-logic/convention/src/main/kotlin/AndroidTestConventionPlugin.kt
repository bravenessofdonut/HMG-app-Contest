import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = extensions
                .getByType<VersionCatalogsExtension>()
                .named("libs")

            dependencies {
                // Unit Test
                add("testImplementation", libs.findLibrary("junit").get())

                // Instrumented Test
                add("androidTestImplementation", libs.findLibrary("androidx-junit").get())
                add("androidTestImplementation", libs.findLibrary("androidx-test-runner").get())
            }
        }
    }
}