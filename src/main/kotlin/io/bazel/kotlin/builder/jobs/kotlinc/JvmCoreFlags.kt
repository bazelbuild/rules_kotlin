package io.bazel.kotlin.builder.jobs.kotlinc.flags

import io.bazel.kotlin.builder.jobs.kotlinc.FilesSystemInputs
import io.bazel.kotlin.builder.jobs.kotlinc.RepositoryLocations
import io.bazel.kotlin.builder.jobs.kotlinc.ZipArchive
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.CompileKotlinForJvm
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.CompileWithAssociates
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.CompileWithPlugins
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.GenerateAbi
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.GenerateJDeps
import io.bazel.kotlin.builder.jobs.kotlinc.configurations.GenerateStubs
import io.bazel.kotlin.builder.utils.Arguments
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * [FlagValues] for all [io.bazel.kotlin.builder.jobs.kotlinc.configurations.CompilerConfiguration]s.
 */
abstract class FlagValues(
  argument: Arguments,
  workingDirectory: Path,
  override val fileSystem: FileSystem = FileSystems.getDefault(),
) : FilesSystemInputs,
  CompileKotlinForJvm.In,
  CompileKotlinForJvm.Out,
  CompileWithAssociates.In,
  CompileWithAssociates.Out,
  GenerateJDeps.In,
  GenerateJDeps.Out,
  GenerateAbi.In,
  GenerateAbi.Out,
  GenerateStubs.In,
  GenerateStubs.Out,
  CompileWithPlugins.In,
  CompileWithPlugins.Out {
  override val jdkHome by argument.path("jdk_home", "", System.getenv("JAVA_HOME"))
  override val useIr: Boolean by lazy { "-Xuse-ir" in passthroughFlags }
  override val classpath by argument.paths("classpath", "")
  override val directDependencies by argument.paths("direct_dependencies", "")
  override val depsArtifacts by argument.paths("deps_artifacts", "")
  override val sources by argument.flagOf<List<Path>>(
    "source",
    "sources to compile",
    emptyList(),
  ) { current ->
    split(",").map { fileSystem.getPath(it) } + current
  }

  private val archive by lazy {
    ZipArchive(workingDirectory.resolve("src_jars"))
  }

  override val sourcesFromJars by argument.flagOf<List<Path>>(
    "source_jars",
    "",
    default = emptyList(),
  ) { current ->
    split(",").map(fileSystem::getPath).flatMap(archive::extract) + current
  }
  override val processorPath by argument.paths("processorpath", "")
  override val processors by argument.flagOf<List<String>>("processors", "", emptyList()) {
    split(",")
  }
  override val stubsPluginOptions by argument.flagOf<List<String>>(
    "stubs_plugin_options",
    "",
    emptyList(),
  ) {
    split(",")
  }
  override val stubsPluginClassPath by argument.paths(
    "stubs_plugin_classpath",
    "",
  )
  override val compilerPluginOptions by argument.flagOf<List<String>>(
    "compiler_plugin_options",
    "",
    emptyList(),
  ) {
    split(",")
  }
  override val compilerPluginClasspath by argument.paths(
    "compiler_plugin_classpath",
    "",
  )
  val ruleKind by argument.flagOf("rule_kind", "")
  override val moduleName by argument.flagOf<String>(
    "kotlin_module_name",
    "",
    required = true,
    default = "required",
  ) { toString() }

  override val passthroughFlags by argument.flagOf<List<String>>(
    "kotlin_passthrough_flags",
    "",
    emptyList(),
  ) {
    split(",")
  }
  override val associatePaths: List<Path> by argument.paths(
    "kotlin_friend_paths",
    "Modules to associate with the compilation of this module.",
  )

  override val apiVersion by argument.flagOf(
    "kotlin_api_version",
    "",
    required = true,
  )

  override val languageVersion by argument.flagOf(
    "kotlin_language_version",
    "",
    required = true,
  )

  override val jvmTarget by argument.flagOf(
    "kotlin_jvm_target",
    "",
    required = true,
  )

  val friendsPaths by argument.paths(
    "kotlin_friend_paths",
    "",
    required = true,
  )

  val jsPassthroughFlags by argument.flagOf(
    "kotlin_js_passthrough_flags",
    "",
    emptyList<String>(),
  ) {
    split(",")
  }

  override val debug by argument.flagOf("kotlin_debug_tags", "", emptyList<String>()) {
    split(",")
  }

  val taskId by argument.flagOf("kotlin_task_id", "")

  override val abiJar by argument.path("abi_jar", "")

  override val generatedJavaSrcJar by argument.path("generated_java_srcjar", "")

  override val generatedJavaStubJar by argument.path("kapt_generated_stub_jar", "")

  override val generatedClassJar by argument.path("kapt_generated_class_jar", "")

  val buildKotlin by argument.flagOf("build_kotlin", "") // used for?

  override val targetLabel by argument.flagOf<String>("target_label", "", "") { toString() }

  override val strictKotlinDeps by argument.flagOf<Boolean>(
    "strict_kotlin_deps",
    "",
    default = false,
  ) { toBooleanStrict() }

  override val reducedClasspathMode by argument.flagOf<Boolean>(
    "reduced_classpath_mode",
    "",
    default = false,
  ) { toBooleanStrict() }

  val instrumentCoverage by argument.flagOf<Boolean>(
    "instrument_coverage",
    "",
    default = false,
  ) { toBooleanStrict() }


  override val jdeps: Path = RepositoryLocations.JDEPS_GEN_PLUGIN
  override val abi: Path = RepositoryLocations.DEFAULT_JVM_ABI_PATH
  override val kapt: Path = RepositoryLocations.KAPT

  override val outputSrcJar by argument.path("kotlin_output_srcjar", "", required = true)
  override val output by argument.path("output", "", required = true)
  override val outputJdeps by argument.path("kotlin_output_jdeps", "")

  override val verbose: Boolean
    get() = TODO("Not yet implemented")

  // TODO(): Implement passing kapt annotation options.
  override val processorOptions: Map<String, String> = emptyMap()

  val depsPaths: List<Path> by argument.paths("deps", "")

  val jsLibraries by argument.paths("kotlin_js_libraries", "")

  val outputJsJar by argument.path("kotlin_output_js_jar", "")
}
