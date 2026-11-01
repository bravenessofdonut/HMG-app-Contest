import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ConventionCatalogTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun retrofitConventionAddsItsDeclaredClientToJvmConsumer() {
        val root = temp.root
        val catalog = File(root, "gradle/libs.versions.toml")
        catalog.parentFile.mkdirs()
        File(System.getProperty("guide.versionCatalog")).copyTo(catalog)
        File(root, "settings.gradle").writeText("rootProject.name = 'consumer'")
        File(root, "build.gradle").writeText(
            """
            plugins {
                id 'java-library'
                id 'braveberry.jvm.retrofit'
            }
            tasks.register('verifyClient') {
                doLast {
                    assert configurations.implementation.dependencies.any {
                        it.group == 'com.squareup.retrofit2' && it.name == 'retrofit'
                    }
                }
            }
            """.trimIndent()
        )
        GradleRunner.create().withProjectDir(root).withPluginClasspath()
            .withArguments("verifyClient", "--offline").build()
    }
}
