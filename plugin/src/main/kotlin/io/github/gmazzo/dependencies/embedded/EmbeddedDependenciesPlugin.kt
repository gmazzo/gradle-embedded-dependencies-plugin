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
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.registerTransform
import org.gradle.kotlin.dsl.the

public class EmbeddedDependenciesPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        apply(plugin = "java-base")

        val specs = objects.domainObjectContainer(EmbeddedDependenciesSpec::class)

        specs.configureEach {

            includes
                .finalizeValueOnRead()

            excludes
                .finalizeValueOnRead()

            // by default, all build-specific resources are excluded
            exclude(
                "META-INF/LICENSE**",
                "META-INF/NOTICE**",
                "META-INF/MANIFEST.MF",
                "META-INF/*.kotlin_module",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "META-INF/maven/**",
                "META-INF/versions/*/module-info.class"
            )

            repackages
                .finalizeValueOnRead()

        }

        the<SourceSetContainer>().configureEach {
            val spec = specs.maybeCreate(name)

            val discriminator = if (name == MAIN_SOURCE_SET_NAME) "embedded" else "embedded-$name"
            val repackagedClassesElements: LibraryElements = objects.named("$CLASSES+repackaged+$discriminator")
            val repackagedResourcesElements: LibraryElements = objects.named("$RESOURCES+repackaged+$discriminator")

            dependencies.registerTransform(EmbeddedDependenciesTransform::class) {
                from.attributes.attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
                to.attributes.attribute(LIBRARY_ELEMENTS_ATTRIBUTE, repackagedClassesElements)
                parameters.forResources.value(false)
                parameters.includes.value(spec.includes)
                parameters.excludes.value(spec.excludes)
                parameters.mappings.value(spec.repackages)
            }

            dependencies.registerTransform(EmbeddedDependenciesTransform::class) {
                from.attributes.attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
                to.attributes.attribute(LIBRARY_ELEMENTS_ATTRIBUTE, repackagedResourcesElements)
                parameters.forResources.value(true)
                parameters.includes.value(spec.includes)
                parameters.excludes.value(spec.excludes)
                parameters.mappings.value(spec.repackages)
            }

            val config =
                configurations.create(if (name == MAIN_SOURCE_SET_NAME) "embedded" else "${name}Embedded") config@{
                    isCanBeResolved = true
                    isCanBeConsumed = false
                    (this@config as ExtensionAware).extensions.add(
                        publicType = EmbeddedDependenciesSpec::class,
                        name = "embedding",
                        extension = spec,
                    )
                }

            val embeddedClasses = config.incoming
                .artifactView {
                    attributes {
                        attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
                        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, repackagedClassesElements)
                    }
                }
                .files

            val embeddedResources = config.incoming
                .artifactView {
                    attributes {
                        attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
                        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, repackagedResourcesElements)
                    }
                }
                .files

            dependencies.add(compileOnlyConfigurationName, embeddedClasses)
            (output.classesDirs as ConfigurableFileCollection).from(embeddedClasses)
            tasks.named<AbstractCopyTask>(processResourcesTaskName) {
                from(embeddedResources)
            }
        }

    }

}
