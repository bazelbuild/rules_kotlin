/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package io.bazel.kotlin.builder.tasks

import io.bazel.kotlin.builder.tasks.js.Kotlin2JsTaskExecutor
import io.bazel.kotlin.builder.tasks.jvm.KotlinJvmTaskExecutor
import io.bazel.kotlin.builder.toolchain.CompilationStatusException
import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.builder.utils.ArgMap
import io.bazel.kotlin.builder.utils.ArgMaps
import io.bazel.kotlin.builder.utils.Flag
import io.bazel.kotlin.builder.utils.partitionJvmSources
import io.bazel.kotlin.builder.utils.resolveNewDirectories
import io.bazel.kotlin.model.CompilationTaskInfo
import io.bazel.kotlin.model.JsCompilationTask
import io.bazel.kotlin.model.JvmCompilationTask
import io.bazel.kotlin.model.Platform
import io.bazel.kotlin.model.RuleKind
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
@Suppress("MemberVisibilityCanBePrivate")
class KotlinBuilder @Inject internal constructor(
  private val outputProvider: Provider<PrintStream>,
  private val jvmTaskExecutor: KotlinJvmTaskExecutor,
  private val jsTaskExecutor: Kotlin2JsTaskExecutor
) : CommandLineProgram {
  companion object {
    @JvmStatic
    private val FLAGFILE_RE = Pattern.compile("""^--flagfile=((.*)-(\d+).params)$""").toRegex()

    /**
     * Declares the flags used by the java builder.
     */
    enum class JavaBuilderFlags(override val flag: String) : Flag {
      TARGET_LABEL("--target_label"),
      CLASSPATH("--classpath"),
      JAVAC_OPTS("--javacopts"),
      DEPENDENCIES("--dependencies"),
      DIRECT_DEPENDENCIES("--direct_dependencies"),
      DIRECT_DEPENDENCY("--direct_dependency"),
      INDIRECT_DEPENDENCY("--indirect_dependency"),
      STRICT_JAVA_DEPS("--strict_java_deps"),
      OUTPUT_DEPS_PROTO("--output_deps_proto"),
      DEPS_ARTIFACTS("--deps_artifacts"),
      REDUCE_CLASSPATH("--reduce_classpath"),
      SOURCEGEN_DIR("--sourcegendir"),
      GENERATED_SOURCES_OUTPUT("--generated_sources_output"),
      OUTPUT_MANIFEST_PROTO("--output_manifest_proto"),
      SOURCES("--sources"),
      SOURCE_ROOTS("--source_roots"),
      SOURCE_JARS("--source_jars"),
      SOURCE_PATH("--sourcepath"),
      BOOT_CLASSPATH("--bootclasspath"),
      PROCESSOR_PATH("--processorpath"),
      PROCESSORS("--processors"),
      STUBS_PLUGIN_PATH("--stubs_plugin"),
      STUBS_PLUGIN_OPTIONS("--stubs_plugin_options"),
      STUBS_PLUGIN_CLASS_PATH("--stubs_plugin_classpath"),
      COMPILER_PLUGIN_PATH("--compiler_plugin"),
      COMPILER_PLUGIN_OPTIONS("--compiler_plugin_options"),
      COMPILER_PLUGIN_CLASS_PATH("--compiler_plugin_classpath"),
      EXT_CLASSPATH("--extclasspath"),
      EXT_DIR("--extdir"),
      OUTPUT("--output"),
      NATIVE_HEADER_OUTPUT("--native_header_output"),
      CLASSDIR("--classdir"),
      TEMPDIR("--tempdir"),
      GENDIR("--gendir"),
      POST_PROCESSOR("--post_processor"),
      COMPRESS_JAR("--compress_jar"),
      RULE_KIND("--rule_kind"),
      TEST_ONLY("--testonly"),
      BUILD_JAVA("--build_java");
    }

    enum class KotlinBuilderFlags(override val flag: String) : Flag {
      MODULE_NAME("--kotlin_module_name"),
      PASSTHROUGH_FLAGS("--kotlin_passthrough_flags"),
      API_VERSION("--kotlin_api_version"),
      LANGUAGE_VERSION("--kotlin_language_version"),
      JVM_TARGET("--kotlin_jvm_target"),
      OUTPUT_SRCJAR("--kotlin_output_srcjar"),
      GENERATED_CLASSDIR("--kotlin_generated_classdir"),
      FRIEND_PATHS("--kotlin_friend_paths"),
      OUTPUT_JDEPS("--kotlin_output_jdeps"),
      OUTPUT_JS_JAR("--kotlin_output_js_jar"),
      JS_PASSTHROUGH_FLAGS("--kotlin_js_passthrough_flags"),
      JS_LIBRARIES("--kotlin_js_libraries"),
      DEBUG("--kotlin_debug_tags"),
      TASK_ID("--kotlin_task_id"),
      ABI_JAR("--abi_jar"),
      GENERATED_JAVA_SRC_JAR("--generated_java_srcjar"),
      GENERATED_JAVA_STUB_JAR("--kapt_generated_stub_jar"),
      GENERATED_CLASS_JAR("--kapt_generated_class_jar"),
      BUILD_KOTLIN("--build_kotlin"),
      STRICT_KOTLIN_DEPS("--strict_kotlin_deps"),
      REDUCED_CLASSPATH_MODE("--reduced_classpath_mode"),
    }
  }

  override fun apply(workingDir: Path, args: List<String>): Int {
    val (argMap, context) = buildContext(args)
    var success = false
    var status = 0
    try {
      @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
      when (context.info.platform) {
        Platform.JVM -> executeJvmTask(context, workingDir, argMap)
        Platform.JS -> executeJsTask(context, workingDir, argMap)
        Platform.UNRECOGNIZED -> throw IllegalStateException(
            "unrecognized platform: ${context.info}")
      }
      success = true
    } catch (ex: CompilationStatusException) {
      System.err.println("Compilation failure: ${ex.message}")
      status = ex.status
    } catch (throwable: Throwable) {
      context.reportUnhandledException(throwable)
    } finally {
      context.finalize(success)
    }
    return status
  }

  private fun buildContext(args: List<String>): Pair<ArgMap, CompilationTaskContext> {
    check(args.isNotEmpty()) { "expected at least a single arg got: ${args.joinToString(" ")}" }
    val lines = FLAGFILE_RE.matchEntire(args[0])?.groups?.get(1)?.let {
      Files.readAllLines(FileSystems.getDefault().getPath(it.value), StandardCharsets.UTF_8)
    } ?: args

    val argMap = ArgMaps.from(lines)
    val info = buildTaskInfo(argMap).build()
    val context =
        CompilationTaskContext(info, outputProvider.get())
    return Pair(argMap, context)
  }

  private fun buildTaskInfo(argMap: ArgMap): CompilationTaskInfo.Builder =
    with(CompilationTaskInfo.newBuilder()) {
      addAllDebug(argMap.mandatory(KotlinBuilderFlags.DEBUG))

      label = argMap.mandatorySingle(JavaBuilderFlags.TARGET_LABEL)
      argMap.mandatorySingle(JavaBuilderFlags.RULE_KIND).split("_").also {
        check(it.size == 3 && it[0] == "kt") { "invalid rule kind $it" }
        platform = checkNotNull(Platform.valueOf(it[1].toUpperCase())) {
          "unrecognized platform ${it[1]}"
        }
        ruleKind = checkNotNull(RuleKind.valueOf(it[2].toUpperCase())) {
          "unrecognized rule kind ${it[2]}"
        }
      }
      moduleName = argMap.mandatorySingle(KotlinBuilderFlags.MODULE_NAME).also {
        check(it.isNotBlank()) { "--kotlin_module_name should not be blank" }
      }
      addAllPassthroughFlags(argMap.optional(KotlinBuilderFlags.PASSTHROUGH_FLAGS) ?: emptyList())

      argMap.optional(KotlinBuilderFlags.FRIEND_PATHS)?.let(::addAllFriendPaths)
      toolchainInfoBuilder.commonBuilder.apiVersion =
        argMap.mandatorySingle(KotlinBuilderFlags.API_VERSION)
      toolchainInfoBuilder.commonBuilder.languageVersion =
        argMap.mandatorySingle(KotlinBuilderFlags.LANGUAGE_VERSION)
      strictKotlinDeps = argMap.mandatorySingle(KotlinBuilderFlags.STRICT_KOTLIN_DEPS)
      this
    }

  private fun executeJsTask(
    context: CompilationTaskContext,
    workingDir: Path,
    argMap: ArgMap
  ) =
      buildJsTask(context.info, workingDir, argMap).let { jsTask ->
      context.whenTracing { printProto("js task input", jsTask) }
      jsTaskExecutor.execute(context, jsTask)
    }

  private fun buildJsTask(
    info: CompilationTaskInfo,
    workingDir: Path,
    argMap: ArgMap
  ): JsCompilationTask =
    with(JsCompilationTask.newBuilder()) {
      this.info = info

        with(directoriesBuilder) {
          temp = workingDir.toString()
        }

      with(inputsBuilder) {
        addAllLibraries(argMap.mandatory(KotlinBuilderFlags.JS_LIBRARIES))
        addAllKotlinSources(argMap.mandatory(JavaBuilderFlags.SOURCES))
      }
      with(outputsBuilder) {
        js = argMap.mandatorySingle(JavaBuilderFlags.OUTPUT)
        jar = argMap.mandatorySingle(KotlinBuilderFlags.OUTPUT_JS_JAR)
        srcjar = argMap.mandatorySingle(KotlinBuilderFlags.OUTPUT_SRCJAR)
      }
      addAllPassThroughFlags(argMap.mandatory(KotlinBuilderFlags.JS_PASSTHROUGH_FLAGS))
      build()
    }

  private fun executeJvmTask(context: CompilationTaskContext, workingDir: Path, argMap: ArgMap) {
    val task = buildJvmTask(context.info, workingDir, argMap)
    context.whenTracing {
      printProto("jvm task message:", task)
    }
    jvmTaskExecutor.execute(context, task)
  }

  private fun buildJvmTask(
    info: CompilationTaskInfo,
    workingDir: Path,
    argMap: ArgMap
  ): JvmCompilationTask =
    JvmCompilationTask.newBuilder().let { root ->
      root.info = info

      root.compileJava = argMap.mandatorySingle(JavaBuilderFlags.BUILD_JAVA).toBoolean()
      root.compileKotlin = argMap.mandatorySingle(KotlinBuilderFlags.BUILD_KOTLIN).toBoolean()

      with(root.outputsBuilder) {
          argMap.optionalSingle(JavaBuilderFlags.OUTPUT)?.let { jar = it }
          argMap.optionalSingle(KotlinBuilderFlags.OUTPUT_SRCJAR)?.let { srcjar = it }

          argMap.optionalSingle(KotlinBuilderFlags.OUTPUT_JDEPS)?.apply { jdeps = this }
          argMap.optionalSingle(JavaBuilderFlags.OUTPUT_DEPS_PROTO)?.apply { javaJdeps = this }
          argMap.optionalSingle(KotlinBuilderFlags.GENERATED_JAVA_SRC_JAR)?.apply {
            generatedJavaSrcJar = this
          }
          argMap.optionalSingle(KotlinBuilderFlags.GENERATED_JAVA_STUB_JAR)?.apply {
            generatedJavaStubJar = this
          }
          argMap.optionalSingle(KotlinBuilderFlags.ABI_JAR)?.let { abijar = it }
          argMap.optionalSingle(KotlinBuilderFlags.GENERATED_CLASS_JAR)?.let {
            generatedClassJar = it
          }
      }

      with(root.directoriesBuilder)
        {
          val moduleName = argMap.mandatorySingle(KotlinBuilderFlags.MODULE_NAME)
          classes =
            workingDir.resolveNewDirectories(getOutputDirPath(moduleName, "classes")).toString()
          javaClasses =
            workingDir.resolveNewDirectories(getOutputDirPath(moduleName, "java_classes")).toString()
          if (argMap.hasAll(KotlinBuilderFlags.ABI_JAR)) abiClasses = workingDir.resolveNewDirectories(getOutputDirPath(moduleName, "abi_classes")).toString()
          generatedClasses = workingDir.resolveNewDirectories(getOutputDirPath(moduleName, "generated_classes")).toString()
          temp = workingDir.resolveNewDirectories(getOutputDirPath(moduleName, "temp")).toString()
          generatedSources = workingDir.resolveNewDirectories(getOutputDirPath(moduleName, "generated_sources")).toString()
          generatedJavaSources = workingDir.resolveNewDirectories(getOutputDirPath(moduleName, "generated_java_sources")).toString()
          generatedStubClasses = workingDir.resolveNewDirectories(getOutputDirPath(moduleName, "stubs")).toString()
      }

      with(root.inputsBuilder) {
        addAllClasspath(argMap.mandatory(JavaBuilderFlags.CLASSPATH))
        addAllDepsArtifacts(
          argMap.optional(JavaBuilderFlags.DEPS_ARTIFACTS) ?: emptyList()
        )
        addAllDirectDependencies(argMap.mandatory(JavaBuilderFlags.DIRECT_DEPENDENCIES))

        addAllProcessors(argMap.optional(JavaBuilderFlags.PROCESSORS) ?: emptyList())
        addAllProcessorpaths(argMap.optional(JavaBuilderFlags.PROCESSOR_PATH) ?: emptyList())

        addAllStubsPlugins(argMap.optional(JavaBuilderFlags.STUBS_PLUGIN_PATH) ?: emptyList())
        addAllStubsPluginOptions(
          argMap.optional(JavaBuilderFlags.STUBS_PLUGIN_OPTIONS) ?: emptyList()
        )
        addAllStubsPluginClasspath(
          argMap.optional(JavaBuilderFlags.STUBS_PLUGIN_CLASS_PATH) ?: emptyList()
        )

        addAllCompilerPlugins(
          argMap.optional(JavaBuilderFlags.COMPILER_PLUGIN_PATH) ?: emptyList()
        )
        addAllCompilerPluginOptions(
          argMap.optional(JavaBuilderFlags.COMPILER_PLUGIN_OPTIONS) ?: emptyList()
        )
        addAllCompilerPluginClasspath(
          argMap.optional(JavaBuilderFlags.COMPILER_PLUGIN_CLASS_PATH) ?: emptyList()
        )

        argMap.optional(JavaBuilderFlags.SOURCES)
          ?.iterator()
          ?.partitionJvmSources(
            { addKotlinSources(it) },
            { addJavaSources(it) }
          )
        argMap.optional(JavaBuilderFlags.SOURCE_JARS)
          ?.also {
            addAllSourceJars(it)
          }
      }

      with(root.infoBuilder) {
        toolchainInfoBuilder.jvmBuilder.jvmTarget =
          argMap.mandatorySingle(KotlinBuilderFlags.JVM_TARGET)
      }
      root.build()
    }

  private fun getOutputDirPath(moduleName: String, dirName: String) = "_kotlinc/${moduleName}_jvm/$dirName"
}
