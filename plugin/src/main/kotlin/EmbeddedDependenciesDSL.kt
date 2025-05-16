package org.gradle.kotlin.dsl

import io.github.gmazzo.dependencies.embedded.EmbeddedDependenciesSpec
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ExtensionAware

val Configuration.embedding: EmbeddedDependenciesSpec
    get() = (this as ExtensionAware).the()

fun Configuration.embedding(configure: Action<EmbeddedDependenciesSpec>) = apply {
    configure.execute(embedding)
}

fun NamedDomainObjectProvider<Configuration>.embedding(configure: Action<EmbeddedDependenciesSpec>) = configure {
    configure.execute(embedding)
}
