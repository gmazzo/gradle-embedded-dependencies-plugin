plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.gmazzo.dependencies.embedded")
    `maven-publish`
    jacoco
}

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.params)

    embedded(libs.demo.commons.lang3)
    testEmbedded(libs.demo.commons.collections4)
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
