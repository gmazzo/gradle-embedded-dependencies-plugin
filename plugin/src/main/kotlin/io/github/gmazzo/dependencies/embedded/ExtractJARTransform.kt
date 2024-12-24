package io.github.gmazzo.dependencies.embedded

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Not worth caching")
abstract class ExtractJARTransform @Inject constructor(
    private val archiveOperations: ArchiveOperations,
    private val fileOperations: FileSystemOperations,
) : TransformAction<ExtractJARTransform.Params> {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputJar = inputArtifact.get().asFile
        val suffix = if (parameters.forResources) "resources" else "classes"

        fileOperations.sync {
            from(archiveOperations.zipTree(inputJar).matching {
                include(parameters.includes.get())
                exclude(parameters.excludes.get())
            })
            into(outputs.dir("${inputJar.nameWithoutExtension}-$suffix"))
            if (parameters.forResources) exclude("**.class") else include("**.class")
        }
    }

    interface Params : TransformParameters {

        @get:Input
        var forResources: Boolean

        @get:Input
        val includes: SetProperty<String>

        @get:Input
        val excludes: SetProperty<String>

    }

}
