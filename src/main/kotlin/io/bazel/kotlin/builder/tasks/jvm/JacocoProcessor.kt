package io.bazel.kotlin.builder.tasks.jvm

import io.bazel.kotlin.builder.toolchain.KotlinToolchain
import org.jacoco.core.instr.Instrumenter
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import io.bazel.kotlin.model.JvmCompilationTask
import com.google.devtools.build.lib.view.proto.Deps
import javax.inject.Inject


/** Implements Jacoco instrumentation. */
class JacocoProcessor @Inject constructor(val compiler: KotlinToolchain.KotlincInvoker) {
    fun instrument(command: JvmCompilationTask) {
        val classDir = Paths.get(command.directories.classes)
        val instr = Instrumenter(OfflineInstrumentationAccessGenerator())

        // Runs Jacoco instrumentation processor over all .class files.
        Files.walkFileTree(
            classDir,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!file.fileName.toString().endsWith(".class")) {
                        return FileVisitResult.CONTINUE
                    }

                    val uninstrumentedCopy = Paths.get(file.toString() + ".uninstrumented")
                    Files.move(file, uninstrumentedCopy)
                    BufferedInputStream(Files.newInputStream(uninstrumentedCopy)).use { input ->
                        BufferedOutputStream(Files.newOutputStream(file)).use { output ->
                            instr.instrument(input, output, file.toString())
                        }
                    }
                    return FileVisitResult.CONTINUE
                }
            })
    }
}

