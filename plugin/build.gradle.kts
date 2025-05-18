plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samReceiver)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.gitVersion)
    alias(libs.plugins.gradle.pluginPublish)
    alias(libs.plugins.publicationsReport)
    jacoco
}

group = "io.github.gmazzo.dependencies.embedded"
description = "Embed dependencies (A.K.A. `fat` or `uber` jar) in the produced `jar`"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
samWithReceiver.annotation(HasImplicitReceiver::class.qualifiedName!!)
kotlin.compilerOptions.freeCompilerArgs = listOf("-Xjvm-default=all")

val originUrl = providers
    .exec { commandLine("git", "remote", "get-url", "origin") }
    .standardOutput.asText.map { it.trim() }

gradlePlugin {
    website = originUrl
    vcsUrl = originUrl

    plugins {
        create("embedded-dependencies") {
            id = "io.github.gmazzo.dependencies.embedded"
            displayName = name
            implementationClass = "io.github.gmazzo.dependencies.embedded.EmbeddedDependenciesPlugin"
            description = project.description
            tags.addAll("fat", "uber", "embedded", "dependencies")
        }
    }
}

mavenPublishing {
    publishToMavenCentral("CENTRAL_PORTAL", automaticRelease = true)

    pom {
        name = "${rootProject.name}-${project.name}"
        description = provider { project.description }
        url = originUrl

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit/"
            }
        }

        developers {
            developer {
                id = "gmazzo"
                name = id
                email = "gmazzo65@gmail.com"
            }
        }

        scm {
            connection = originUrl
            developerConnection = originUrl
            url = originUrl
        }
    }
}

dependencies {
    fun plugin(plugin: Provider<PluginDependency>) =
        plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }

    compileOnly(gradleKotlinDsl())
    implementation(libs.asm)

    testImplementation(gradleKotlinDsl())
    testImplementation(gradleTestKit())
    testImplementation(plugin(libs.plugins.kotlin.jvm))
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

afterEvaluate {
    tasks.named<Jar>("javadocJar") {
        from(tasks.dokkaGeneratePublicationJavadoc)
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    mustRunAfter(tasks.publishPlugins)
}

tasks.publishPlugins {
    enabled = "$version".matches("\\d+(\\.\\d+)+".toRegex())
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
