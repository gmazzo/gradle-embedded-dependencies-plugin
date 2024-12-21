package io.github.gmazzo.dependencies.embedded

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.the

class EmbeddedDependenciesPlugin : Plugin<Project> {

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

            val classesDir = layout.buildDirectory.dir("embedded-dependencies/$name")
            val classes = files()
                .from(provider {
                    sync {
                        duplicatesStrategy = DuplicatesStrategy.WARN
                        config.asFileTree.forEach { from(zipTree(it)) }
                        into(classesDir)
                        include(spec.includes.get())
                        exclude(spec.excludes.get())
                    }
                    classesDir
                })
                .builtBy(config)
                .apply { finalizeValueOnRead() }

            configurations.named(compileOnlyConfigurationName) { extendsFrom(config) }

            (output.classesDirs as ConfigurableFileCollection).from(classes)
        }

    }

}
