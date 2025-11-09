package io.github.gmazzo.dependencies.embedded

import io.github.gmazzo.dependencies.embedded.EmbeddedDependenciesSpec.Repackage
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.EXPAND_FRAMES
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.SimpleRemapper

@CacheableTransform
public abstract class EmbeddedDependenciesTransform : TransformAction<EmbeddedDependenciesTransform.Params> {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    public abstract val inputArtifact: Provider<FileSystemLocation>

    private val mappings by lazy { parameters.mappings.get().asSequence() }

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile.toPath()
        val output = outputs.file("${input.nameWithoutExtension}-repackaged.${input.extension}").toPath()

        FileSystems.newFileSystem(input).use { inFS ->
            FileSystems.newFileSystem(output, mapOf("create" to "true")).use { outFS ->
                val includes = parameters.includes.get().map { inFS.getPathMatcher("glob:/$it") }
                val excludes = parameters.excludes.get().map { inFS.getPathMatcher("glob:/$it") }

                for (root in inFS.rootDirectories) {
                    for (file in Files.walk(root)) {
                        if (!Files.isRegularFile(file)) continue
                        if (includes.isNotEmpty() && !includes.any { it.matches(file) }) continue
                        if (excludes.any { it.matches(file) }) continue

                        val outFile = outFS.outputFile(file.relativeTo(root))

                        outFile.parent?.let(Files::createDirectories)
                        when {
                            file.extension == "class" -> repackageClass(file, outFile)
                            file.pathString.matches("^META-INF/(MANIFEST.MF$|services/|gradle-plugins/)".toRegex()) ->
                                repackageResources(file, outFile)

                            else -> Files.copy(file, outFile)
                        }
                    }
                }
            }
        }
    }

    private fun FileSystem.outputFile(path: Path) = when {
        path.pathString.startsWith("META-INF") -> getPath(path.pathString)
        else -> when (val parent = path.parent) {
            null -> getPath(path.name)
            else -> getPath(repackageClassName(parent.pathString)).resolve(path.name)
        }
    }

    private fun repackageClass(from: Path, into: Path) = Files.newInputStream(from).use { input ->
        val remapped = ClassWriter(0)

        ClassReader(input).accept(Visitor(remapped), EXPAND_FRAMES)

        Files.write(into, remapped.toByteArray())
    }

    private fun repackageResources(from: Path, into: Path) {
        val isManifest =
            if (from.pathString == "META-INF/MANIFEST.MF") Repackage::forManifest else Repackage::forResources

        val lines = Files.readAllLines(from)
        val newLines = lines.map { repackage(it, isManifest) }

        Files.write(into, newLines)
    }

    private fun repackage(value: String, forTarget: Repackage.() -> Boolean) = mappings
        .filter(forTarget)
        .map { it.regex.replaceFirst(value, it.replacement) }
        .firstOrNull { it != value }
        ?: value

    private fun repackageClassName(value: String) = repackage(
        value = value.replace('/', '.'),
        forTarget = Repackage::forClasses
    ).replace('.', '/')

    private inner class Visitor(output: ClassVisitor) : ClassRemapper(output, Mapper()) {

        override fun visitField(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            value: Any?
        ) = super.visitField(access, name, descriptor, signature, value.remapped)

        override fun createMethodRemapper(visitor: MethodVisitor) =
            object : MethodRemapper(api, visitor, remapper) {

                override fun visitLdcInsn(value: Any?) {
                    super.visitLdcInsn(value.remapped)
                }

            }

        private val Any?.remapped
            get() = if (this is String) repackage(this) { forClasses || forResources } else this

    }

    private inner class Mapper : SimpleRemapper(emptyMap()) {

        override fun map(key: String): String? {
            if (key.contains(".")) return null // do not rename fields and methods
            val newKey = repackageClassName(key)

            return if (newKey == key) null else newKey
        }

    }

    public interface Params : TransformParameters {

        @get:Input
        public val includes: SetProperty<String>

        @get:Input
        public val excludes: SetProperty<String>

        @get:Input
        public val mappings: ListProperty<Repackage>

    }

}
