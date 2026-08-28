package io.github.gmazzo.dependencies.embedded

import java.nio.file.CopyOption
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.EmptyFileVisitor
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Just I/O operations")
public abstract class ExtractClassesTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val from: ConfigurableFileCollection

    @get:OutputDirectory
    public abstract val destinationDir: DirectoryProperty

    init {
        destinationDir.convention(project.layout.dir(project.provider { temporaryDir }))
    }

    @TaskAction
    @OptIn(ExperimentalPathApi::class)
    internal fun extractClasses() {
        val output = destinationDir.asFile.get().toPath().apply {
            deleteRecursively()
            createDirectories()
        }

        from.asFileTree.visit(object : EmptyFileVisitor() {

            override fun visitFile(fileDetails: FileVisitDetails) {
                val path = fileDetails.file.toPath()

                when (fileDetails.file.extension) {
                    "class" -> copyOrReplace(path, output.resolve(fileDetails.relativePath.pathString))
                    "jar" -> FileSystems.newFileSystem(path).use { jar ->
                        for (root in jar.rootDirectories) {
                            for (file in Files.walk(root)) {
                                if (!Files.isRegularFile(file)) continue

                                copyOrReplace(file, output.resolve(file .relativeTo(root).pathString))
                            }
                        }
                    }
                }
            }

            private fun copyOrReplace(from: Path, to: Path) {
                to.parent.createDirectories()
                from.copyTo(to, StandardCopyOption.REPLACE_EXISTING)
            }

        })
    }

}
