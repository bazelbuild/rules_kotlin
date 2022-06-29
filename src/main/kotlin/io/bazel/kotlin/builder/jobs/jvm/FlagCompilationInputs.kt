package io.bazel.kotlin.builder.jobs.jvm

import io.bazel.kotlin.builder.jobs.jvm.configurations.BaseConfiguration
import io.bazel.kotlin.builder.jobs.jvm.flags.FilesSystemInputs
import io.bazel.kotlin.builder.utils.Arguments
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

class FlagCompilationInputs(
  argument: Arguments,
  override val fileSystem: FileSystem = FileSystems.getDefault(),
) : CompilationInputs, FilesSystemInputs, BaseConfiguration.Inputs, CompilationOutputs {
  override val jdkHome by argument.artifact("jdk_home", "", System.getenv("JAVA_HOME"))
  override val useIr: Boolean by lazy { "-Xuse-ir" in passthroughFlags }
  override val classpath by argument.artifactList("classpath", "")
  override val directDependencies by argument.artifactList("direct_dependencies", "")
  override val depsArtifacts by argument.artifactList("deps_artifacts", "")
  override val sources by argument.artifactList("source", "")
  override val sourcesJars by argument.artifactList("source_jars", "")
  override val processorPath by argument.artifactList("processorpath", "")
  override val processors by argument.artifactList("processors", "")
  override val stubsPluginOptions by argument.artifactList("stubs_plugin_options", "")
  override val stubsPluginClassPath by argument.artifactList(
    "stubs_plugin_classpath",
    "",
  )
  override val compilerPluginOptions by argument.artifactList(
    "compiler_plugin_options",
    "",
  )
  override val compilerPluginClassPath by argument.artifactList(
    "compiler_plugin_classpath",
    "",
  )
  override val ruleKind by argument.flag("rule_kind", "")
  override val moduleName by argument.flag<String>(
    "kotlin_module_name",
    "",
    required = true,
    default = "required"
  ) { toString() }

  override val passthroughFlags by argument.flag<List<String>>(
    "kotlin_passthrough_flags",
    "",
    emptyList()
  ) {
    split(",")
  }
  override val apiVersion by argument.flag(
    "kotlin_api_version",
    "",
    required = true
  )

  override val languageVersion by argument.flag(
    "kotlin_language_version",
    "",
    required = true
  )

  override val jvmTarget by argument.flag(
    "kotlin_jvm_target",
    "",
    required = true
  )

  override val friendsPaths by argument.artifactList(
    "kotlin_friend_paths",
    "",
    required = true
  )

  override val jsPassthroughFlags by argument.flag(
    "kotlin_js_passthrough_flags",
    "",
    emptyList<String>()
  ) {
    split(",")
  }

  override val jsLibraries by argument.artifactList("kotlin_js_libraries", "")

  override val debug by argument.flag("kotlin_debug_tags", "", emptyList<String>()) {
    split(",")
  }

  override val taskId by argument.flag("kotlin_task_id", "")

  override val abiJar by argument.artifact("abi_jar", "")

  override val generatedJavaSrcJar by argument.artifact("generated_java_srcjar", "")

  override val generatedJavaStubJar by argument.artifact("kapt_generated_stub_jar", "")

  override val generatedClassJar by argument.artifact("kapt_generated_class_jar", "")

  override val buildKotlin by argument.flag("build_kotlin", "")
  override val targetLabel by argument.flag<String>("target_label", "", "") { toString() }

  override val depsPaths: List<Path> by argument.artifactList("deps", "")

  override val strictKotlinDeps by argument.flag<Boolean>(
    "strict_kotlin_deps",
    "",
    default = false
  ) { toBooleanStrict() }

  override val reducedClasspathMode by argument.flag<Boolean>(
    "reduced_classpath_mode",
    "",
    default = false
  ) { toBooleanStrict() }

  override val instrumentCoverage by argument.flag<Boolean>(
    "instrument_coverage",
    "",
    default = false
  ) { toBooleanStrict() }


  override val outputSrcJar by argument.artifact("kotlin_output_srcjar", "", required = true)
  override val output by argument.artifact("output", "", required = true)
  override val outputJdeps by argument.artifact("kotlin_output_jdeps", "")
  override val outputJsJar by argument.artifact("kotlin_output_js_jar", "")
}
