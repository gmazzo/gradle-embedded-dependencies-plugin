package io.github.gmazzo.dependencies.embedded

import java.io.File
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
class EmbeddedDependenciesPluginTest {

    @CsvSource(
        ", ",
        "java, embedded|testEmbedded",
        "java-library, embedded|testEmbedded",
        "groovy, embedded|testEmbedded",
        "kotlin, embedded|testEmbedded",
    )
    @ParameterizedTest
    fun `plugin can be applied`(plugin: String?, expectedConfigs: String?): Unit =
        with(ProjectBuilder.builder().build()) {
            apply(plugin = "io.github.gmazzo.dependencies.embedded")
            if (plugin != null) {
                apply(plugin = plugin)
            }

            the<SourceSetContainer>().maybeCreate("customSS")

            val expectedConfigs = expectedConfigs?.split('|')?.toSet().orEmpty() + "customSSEmbedded"

            assertEquals(expectedConfigs, configurations.names.intersect(expectedConfigs))
        }

    fun args() = listOf(
        arguments(GradleVersion.version("8.2"), null),
        arguments(GradleVersion.current(), null),
        arguments(GradleVersion.current(), "--isolated-projects"),
    )

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("args")
    fun `demo integration test`(gradleVersion: GradleVersion, buildArg: String?) {
        val projectDir = File(System.getProperty("TEMP_DIR"), gradleVersion.version)
            .resolve(buildArg ?: "default")
            .apply { deleteRecursively(); mkdirs() }

        File("../demo").copyRecursively(projectDir.resolve("demo"))
        File("../gradle/libs.versions.toml").copyTo(projectDir.resolve("gradle/libs.versions.toml"))

        projectDir.resolve("settings.gradle.kts").writeText(
            File("../settings.gradle.kts")
                .readText()
                .replace("(?<=rootProject.name = \").*?(?=\")".toRegex(), "demo")
                .replace("includeBuild\\(\".*?\"\\)".toRegex(), "")
        )

        val result = GradleRunner.create()
            .withGradleVersion(gradleVersion.version)
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments(listOfNotNull("-s", buildArg, "build"))
            .forwardOutput()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":demo:build")?.outcome)
    }

}
