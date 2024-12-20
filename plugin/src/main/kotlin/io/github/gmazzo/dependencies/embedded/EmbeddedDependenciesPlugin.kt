package io.github.gmazzo.dependencies.embedded

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.the
import javax.inject.Inject

class EmbeddedDependenciesPlugin @Inject constructor(
    private val archiveOperations: ArchiveOperations
) : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        apply(plugin = "java-base")

        val extension = objects
            .domainObjectContainer(EmbeddedDependenciesSpec::class)
            .also { extensions.add("embeddedDependencies", it) }

        extension.configureEach {

            includes
                .finalizeValueOnRead()

            excludes
                .finalizeValueOnRead()

            // by default, all build specific resources are excluded
            exclude(
                "META-INF/LICENSE.txt",
                "META-INF/MANIFEST.MF",
                "META-INF/*.kotlin_module",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "META-INF/maven/**",
                "META-INF/versions/*/module-info.class"
            )
        }

        the<SourceSetContainer>().configureEach {
            val spec = extension.maybeCreate(name)

            val config = configurations.maybeCreate(
                when (name) {
                    SourceSet.MAIN_SOURCE_SET_NAME -> "embedded"
                    else -> "${name}Embedded"
                }
            )

            val classes = files()
                .from(provider {
                    config.asFileTree.map { dependency ->
                        archiveOperations.zipTree(dependency).matching {
                            include(spec.includes.get())
                            exclude(spec.excludes.get())
                        }
                    }
                })
                .builtBy(config)
                .apply { finalizeValueOnRead() }

            configurations.named(compileOnlyConfigurationName) { extendsFrom(config) }
            configurations.named(runtimeOnlyConfigurationName) { extendsFrom(config) }

            (output.classesDirs as ConfigurableFileCollection).from(classes)
        }

    }

}
