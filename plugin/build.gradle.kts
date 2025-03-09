plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samReceiver)
    alias(libs.plugins.gradle.pluginPublish)
    alias(libs.plugins.publicationsReport)
    signing
    jacoco
}

group = "io.github.gmazzo.dependencies.embedded"
description = "Gradle Embedded Dependencies Plugin"
version = providers
    .exec { commandLine("git", "describe", "--tags", "--always") }
    .standardOutput.asText.get().trim().removePrefix("v")

java.toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
samWithReceiver.annotation(HasImplicitReceiver::class.qualifiedName!!)
kotlin.compilerOptions.freeCompilerArgs = listOf("-Xjvm-default=all")

gradlePlugin {
    website.set("https://github.com/gmazzo/gradle-embedded-dependencies-plugin")
    vcsUrl.set("https://github.com/gmazzo/gradle-embedded-dependencies-plugin")

    plugins {
        create("embedded-dependencies") {
            id = "io.github.gmazzo.dependencies.embedded"
            displayName = name
            implementationClass = "io.github.gmazzo.dependencies.embedded.EmbeddedDependenciesPlugin"
            description = "Embed dependencies (A.K.A. `fat` or `uber` jar) in the produced `jar`"
            tags.addAll("fat", "uber", "embedded", "dependencies")
        }
    }
}

dependencies {
    fun plugin(plugin: Provider<PluginDependency>) =
        plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }

    compileOnly(gradleKotlinDsl())

    testImplementation(gradleKotlinDsl())
    testImplementation(gradleTestKit())
    testImplementation(plugin(libs.plugins.kotlin.jvm))
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
    isRequired = signingKey != null || providers.environmentVariable("GRADLE_PUBLISH_KEY").isPresent
}

testing.suites.withType<JvmTestSuite> {
    useJUnitJupiter()
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports.xml.required = true
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
