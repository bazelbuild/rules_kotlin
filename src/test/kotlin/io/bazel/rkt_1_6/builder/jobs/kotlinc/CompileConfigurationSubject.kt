package io.bazel.rkt_1_6.builder.jobs.kotlinc

import com.google.common.truth.CustomSubjectBuilder
import com.google.common.truth.FailureMetadata
import com.google.common.truth.MapSubject
import com.google.common.truth.StringSubject
import com.google.common.truth.Subject
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.CompilerConfiguration
import io.bazel.kotlin.builder.jobs.kotlinc.JobContext
import io.bazel.kotlin.builder.jobs.kotlinc.KotlinToJvm
import io.bazel.worker.Status
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.EXPAND_FRAMES
import org.objectweb.asm.util.TraceClassVisitor
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import java.util.stream.StreamSupport
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists
import kotlin.io.path.readBytes
import kotlin.streams.toList

/** [CompileConfigurationSubject] simplifies the process of testing [CompilerConfiguration]s */
class CompileConfigurationSubject<IN, OUT>(
  private val metadata: FailureMetadata,
  private val actual: List<CompilerConfiguration<IN, OUT>>,
  private val workingDirectory: Path
) : Subject<CompileConfigurationSubject<IN, OUT>, List<CompilerConfiguration<IN, OUT>>>(
  metadata,
  actual
) {

  companion object {

    val configurations = Companion::CompileConfigurationSubjectBuilder

    class CompileConfigurationSubjectBuilder(metadata: FailureMetadata) :
      CustomSubjectBuilder(metadata) {

      fun <IN, OUT> that(
        vararg configurations: CompilerConfiguration<IN, OUT>,
        inDirectory: Path,
        inContext: CompileConfigurationSubject<IN, OUT>.() -> Unit = {}
      ) = CompileConfigurationSubject(
        metadata(),
        configurations.toList(),
        inDirectory
      ).apply(inContext)
    }
  }

  class CompilationResultSubject<OUT>(
    private val failureMetadata: FailureMetadata,
    val out: OUT,
    val scope: TestScope,
    val status: Status
  ) : Subject<CompilationResultSubject<OUT>, OUT>(failureMetadata, out) {
    fun successfully(): CompilationResultSubject<OUT> {
      check("compile").that(status).isEqualTo(Status.SUCCESS)
      return this
    }

    fun isError(): CompilationResultSubject<OUT> {
      check("compile").that(status).isEqualTo(Status.ERROR)
      return this
    }

    fun assertAboutLogs(): MapSubject = check("logs").that(scope.logs)

    fun and(outputAssertions: CompilationResultSubject<OUT>.(OUT) -> Unit) {
      outputAssertions(out)
    }

    fun <OUT> Path?.onFiles(execute: Stream<Path>.() -> OUT): OUT {
      return when {
        this == null -> {
          failWithActual("Expected output was not created", null)
          error("Expected output was not created")
        }
        notExists() -> {
          failWithActual("expected output was not written", this)
          error("Expected output was not created")
        }
        isDirectory() -> Files.walk(this).map(this::relativize).use(execute)
        fileName.toString().run {
          endsWith(".jar") or endsWith(".zip") or endsWith("srcjar")
        } ->
          FileSystems.newFileSystem(toAbsolutePath(), null).use { zip ->
            StreamSupport.stream(
              zip.rootDirectories.spliterator(),
              false
            ).flatMap { root -> Files.walk(root).map(root::relativize) }.execute()
          }
        else -> Stream.of(this).use(execute)
      }
    }

    fun assertAboutByteCodeIn(byteCode: Path, assertions: ClassFileSubject.() -> Unit = {}) =
      ClassFileSubject(failureMetadata, byteCode).apply(assertions)

    fun Path?.streamPaths(): Stream<String> = this
      ?.onFiles { map(Path::toString).toList().stream().filter(String::isNotEmpty) }
      ?: Stream.empty()

    fun Path?.asListOf(vararg others: Path): List<Path> = this
      ?.run(::listOf)
      ?.plus(others)
      ?: emptyList()

  }

  class ClassFileSubject(metadata: FailureMetadata, val actual: Path) :
    Subject<ClassFileSubject, Path>(metadata, actual) {
    private val bytes by lazy {
      actual.readBytes()
    }

    fun thatAsmText(): StringSubject {
      val reader = ClassReader(bytes)
      val textByteCode = ByteArrayOutputStream()
      reader.accept(TraceClassVisitor(PrintWriter(textByteCode)), EXPAND_FRAMES)
      return check("asm").that(textByteCode.toString(UTF_8))
    }
  }


  fun canCompile(inputs: IN, outputs: OUT): CompilationResultSubject<OUT> {
    val intermediate = Files.createTempDirectory(workingDirectory, "compile")
    val scope = TestScope(intermediate);
    return CompilationResultSubject(
      metadata,
      outputs,
      scope,
      KotlinToJvm().run(
        JobContext.of(
          scope,
          inputs,
          outputs
        ),
        actual,
      )
    )
  }
}
