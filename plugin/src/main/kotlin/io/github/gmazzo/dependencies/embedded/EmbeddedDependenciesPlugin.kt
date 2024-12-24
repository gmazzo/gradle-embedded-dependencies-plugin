package io.github.gmazzo.dependencies.embedded

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.LibraryElements.CLASSES
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.RESOURCES
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.registerTransform
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

            val discriminator = "$path-${if (name == MAIN_SOURCE_SET_NAME) "embedded" else "embedded-$name"}"
            val jarElements: LibraryElements = objects.named(JAR)
            val classesElements: LibraryElements = objects.named("$CLASSES+$discriminator")
            val resourcesElements: LibraryElements = objects.named("$RESOURCES+$discriminator")

            dependencies.registerTransform(ExtractJARTransform::class) {
                from.attributes.attribute(LIBRARY_ELEMENTS_ATTRIBUTE, jarElements)
                to.attributes.attribute(LIBRARY_ELEMENTS_ATTRIBUTE, classesElements)
                parameters.forResources = false
                parameters.includes.value(spec.includes)
                parameters.excludes.value(spec.excludes)
            }

            dependencies.registerTransform(ExtractJARTransform::class) {
                from.attributes.attribute(LIBRARY_ELEMENTS_ATTRIBUTE, jarElements)
                to.attributes.attribute(LIBRARY_ELEMENTS_ATTRIBUTE, resourcesElements)
                parameters.forResources = true
                parameters.includes.value(spec.includes)
                parameters.excludes.value(spec.excludes)
            }

            val config = configurations.create(if (name == MAIN_SOURCE_SET_NAME) "embedded" else "${name}Embedded") {
                isCanBeResolved = true
                isCanBeConsumed = false
                isVisible = false
                attributes {
                    attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
                    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, jarElements)
                }
            }

            fun extractedFiles(elements: LibraryElements) = config.incoming
                .artifactView { attributes.attribute(LIBRARY_ELEMENTS_ATTRIBUTE, elements) }
                .files

            dependencies.add(compileOnlyConfigurationName, extractedFiles(jarElements))
            (output.classesDirs as ConfigurableFileCollection).from(extractedFiles(classesElements))
            resources.srcDir(extractedFiles(resourcesElements))
        }

    }

}
