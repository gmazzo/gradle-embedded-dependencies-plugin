package io.github.gmazzo.dependencies.embedded

import java.io.Serializable
import org.gradle.api.Named
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty

@JvmDefaultWithoutCompatibility
interface EmbeddedDependenciesSpec : Named {

    /**
     * Controls what classes and resources get imported.
     *
     * @see org.gradle.api.tasks.util.PatternFilterable.include
     */
    val includes: SetProperty<String>

    /**
     * Adds an ANT-style pattern to the list of included classes and resources.
     *
     * @see org.gradle.api.tasks.util.PatternFilterable.include
     */
    fun include(vararg pattern: String) = apply {
        includes.addAll(*pattern)
    }

    /**
     * Controls what classes and resources get excluded.
     * Defaults to known build-specific resources:
     * ```
     * META-INF/LICENSE*
     * META-INF/NOTICE*
     * META-INF/MANIFEST.MF
     * META-INF/*.kotlin_module
     * META-INF/*.SF
     * META-INF/*.DSA
     * META-INF/*.RSA
     * META-INF/maven/**
     * META-INF/versions/*/module-info.class
     ```
     */*/*/*/*/*/
     *
     * @see org.gradle.api.tasks.util.PatternFilterable.exclude
     */
    val excludes: SetProperty<String>

    /**
     * Adds an ANT-style pattern to the list of excluded classes and resources.
     *
     * @see org.gradle.api.tasks.util.PatternFilterable.exclude
     */
    fun exclude(vararg pattern: String) = apply {
        excludes.addAll(*pattern)
    }

    /**
     * Controls how classes get repackaged.
     */
    val repackages: ListProperty<Repackage>

    /**
     * Repackages classes the given [classNamePattern] to the given [replacement].
     */
    fun repackage(classNamePattern: Regex, replacement: String) = Repackage(
        regex = classNamePattern,
        replacement = replacement,
    ).also(repackages::add)

    /**
     * Repackages classes the given [classNamePrefix] to the given [replacement].
     */
    fun repackage(classNamePrefix: String, replacement: String) =
        repackage("^${Regex.escape(classNamePrefix)}".toRegex(), replacement)

    data class Repackage(
        val regex: Regex,
        val replacement: String,
        var forClasses: Boolean = true,
        var forResources: Boolean = true,
        var forManifest: Boolean = true,
    ) : Serializable

}
