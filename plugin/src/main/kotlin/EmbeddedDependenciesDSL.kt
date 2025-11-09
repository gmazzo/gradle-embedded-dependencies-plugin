package org.gradle.kotlin.dsl

import io.github.gmazzo.dependencies.embedded.EmbeddedDependenciesSpec
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ExtensionAware

public val Configuration.embedding: EmbeddedDependenciesSpec
    get() = (this as ExtensionAware).the()

public fun Configuration.embedding(configure: Action<EmbeddedDependenciesSpec>): Configuration = apply {
    configure.execute(embedding)
}

public fun NamedDomainObjectProvider<Configuration>.embedding(configure: Action<EmbeddedDependenciesSpec>): Unit = configure {
    configure.execute(embedding)
}
