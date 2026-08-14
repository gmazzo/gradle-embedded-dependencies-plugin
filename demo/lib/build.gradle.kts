plugins {
    alias(libs.plugins.kotlin.jvm)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
