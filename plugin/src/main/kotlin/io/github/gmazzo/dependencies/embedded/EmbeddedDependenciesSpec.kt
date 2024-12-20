package io.github.gmazzo.dependencies.embedded

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

@JvmDefaultWithoutCompatibility
interface EmbeddedDependenciesSpec : Named {

    val transitive: Property<Boolean>

    val includes: SetProperty<String>

    fun include(vararg pattern: String) = apply {
        includes.addAll(*pattern)
    }

    val excludes: SetProperty<String>

    fun exclude(vararg pattern: String) = apply {
        excludes.addAll(*pattern)
    }

}
