package io.github.gmazzo.dependencies.embedded

import org.gradle.api.Named
import org.gradle.api.provider.SetProperty

@JvmDefaultWithoutCompatibility
interface EmbeddedDependenciesSpec : Named {

    /**
     * Controls what classes and resources gets imported.
     */
    val includes: SetProperty<String>

    fun include(vararg pattern: String) = apply {
        includes.addAll(*pattern)
    }

    /**
     * Controls what classes and resources gets excluded.
     * Defaults to known build specific resources:
     * ```
     * META-INF/LICENSE.txt
     * META-INF/MANIFEST.MF
     * META-INF/*.kotlin_module
     * META-INF/*.SF
     * META-INF/*.DSA
     * META-INF/*.RSA
     * META-INF/maven/**
     * META-INF/versions/*/module-info.class
     * ```
     */*/*/*/*/*/*/
    val excludes: SetProperty<String>

    fun exclude(vararg pattern: String) = apply {
        excludes.addAll(*pattern)
    }

}
