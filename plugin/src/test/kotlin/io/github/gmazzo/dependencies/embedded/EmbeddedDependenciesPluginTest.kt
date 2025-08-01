package io.github.gmazzo.dependencies.embedded

import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

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

}
