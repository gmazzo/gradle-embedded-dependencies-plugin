![GitHub](https://img.shields.io/github/license/gmazzo/gradle-embedded-dependencies-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.gmazzo.dependencies.embedded)](https://plugins.gradle.org/plugin/io.github.gmazzo.dependencies.embedded)
[![Build Status](https://github.com/gmazzo/gradle-embedded-dependencies-plugin/actions/workflows/build.yaml/badge.svg)](https://github.com/gmazzo/gradle-embedded-dependencies-plugin/actions/workflows/build.yaml)
[![Coverage](https://codecov.io/gh/gmazzo/gradle-embedded-dependencies-plugin/branch/main/graph/badge.svg?token=D5cDiPWvcS)](https://codecov.io/gh/gmazzo/gradle-embedded-dependencies-plugin)
[![Users](https://img.shields.io/badge/users_by-Sourcegraph-purple)](https://sourcegraph.com/search?q=content:io.github.gmazzo.dependencies.embedded+-repo:github.com/gmazzo/gradle-embedded-dependencies-plugin)

# gradle-embedded-dependencies-plugin
A Gradle plugin to embed dependencies (A.K.A. `fat` or `uber` jar) in the resulting `jar` outcome.

# Usage
Apply the plugin:
```kotlin
plugins {
    java
    id("io.github.gmazzo.dependencies.embedded") version "<latest>" 
}

dependencies {
    embedded("org.apache.commons:commons-lang3:3.14.0")
}
```

Then the `jar` task will have the classes from `org.apache.commons:commons-lang3:3.14.0` (and it won't be a dependency when published)
