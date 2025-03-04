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

import io.bazel.kotlin.builder.tasks.jvm.KotlinJvmTaskExecutor
import io.bazel.kotlin.builder.toolchain.CompilationStatusException
import io.bazel.kotlin.builder.toolchain.CompilationTaskContext
import io.bazel.kotlin.builder.utils.ArgMap
import io.bazel.kotlin.builder.utils.ArgMaps
import io.bazel.kotlin.builder.utils.Flag
import io.bazel.kotlin.builder.utils.partitionJvmSources
import io.bazel.kotlin.builder.utils.resolveNewDirectories
import io.bazel.kotlin.model.CompilationTaskInfo
import io.bazel.kotlin.model.JvmCompilationTask
import io.bazel.kotlin.model.Platform
import io.bazel.kotlin.model.RuleKind
import io.bazel.worker.WorkerContext
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("MemberVisibilityCanBePrivate")
class KotlinBuilder
  @Inject
  internal constructor(
    private val jvmTaskExecutor: KotlinJvmTaskExecutor,
  ) {
    companion object {
      @JvmStatic
      private val FLAGFILE_RE = Pattern.compile("""^--flagfile=((.*)-(\d+).params)$""").toRegex()

      enum class KotlinBuilderFlags(
        override val flag: String,
      ) : Flag {
        TARGET_LABEL("--target_label"),
        CLASSPATH("--classpath"),
        DIRECT_DEPENDENCIES("--direct_dependencies"),
        DEPS_ARTIFACTS("--deps_artifacts"),
        SOURCES("--sources"),
        SOURCE_JARS("--source_jars"),
        PROCESSOR_PATH("--processorpath"),
        PROCESSORS("--processors"),
        STUBS_PLUGIN_OPTIONS("--stubs_plugin_options"),
        STUBS_PLUGIN_CLASS_PATH("--stubs_plugin_classpath"),
        COMPILER_PLUGIN_OPTIONS("--compiler_plugin_options"),
        COMPILER_PLUGIN_CLASS_PATH("--compiler_plugin_classpath"),
        OUTPUT("--output"),
        RULE_KIND("--rule_kind"),
        MODULE_NAME("--kotlin_module_name"),
        PASSTHROUGH_FLAGS("--kotlin_passthrough_flags"),
        API_VERSION("--kotlin_api_version"),
        LANGUAGE_VERSION("--kotlin_language_version"),
        JVM_TARGET("--kotlin_jvm_target"),
        OUTPUT_SRCJAR("--kotlin_output_srcjar"),
        GENERATED_CLASSDIR("--kotlin_generated_classdir"),
        FRIEND_PATHS("--kotlin_friend_paths"),
        OUTPUT_JDEPS("--kotlin_output_jdeps"),
        DEBUG("--kotlin_debug_tags"),
        TASK_ID("--kotlin_task_id"),
        ABI_JAR("--abi_jar"),
        ABI_JAR_INTERNAL_AS_PRIVATE("--treat_internal_as_private_in_abi_jar"),
        ABI_JAR_REMOVE_PRIVATE_CLASSES("--remove_private_classes_in_abi_jar"),
        GENERATED_JAVA_SRC_JAR("--generated_java_srcjar"),
        GENERATED_JAVA_STUB_JAR("--kapt_generated_stub_jar"),
        GENERATED_CLASS_JAR("--kapt_generated_class_jar"),
        BUILD_KOTLIN("--build_kotlin"),
        STRICT_KOTLIN_DEPS("--strict_kotlin_deps"),
        REDUCED_CLASSPATH_MODE("--reduced_classpath_mode"),
        INSTRUMENT_COVERAGE("--instrument_coverage"),
        KSP_GENERATED_JAVA_SRCJAR("--ksp_generated_java_srcjar"),
      }
    }

    fun build(
      taskContext: WorkerContext.TaskContext,
      args: List<String>,
    ): Int {
      val (argMap, compileContext) = buildContext(taskContext, args)
      var success = false
      var status = 0
      try {
        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        when (compileContext.info.platform) {
          Platform.JVM -> executeJvmTask(compileContext, taskContext.directory, argMap)
          Platform.UNRECOGNIZED -> throw IllegalStateException(
            "unrecognized platform: ${compileContext.info}",
          )
        }
        success = true
      } catch (ex: CompilationStatusException) {
        taskContext.error { "Compilation failure: ${ex.message}" }
        status = ex.status
      } catch (throwable: Throwable) {
        taskContext.error(throwable) { "Uncaught exception" }
      } finally {
        compileContext.finalize(success)
      }
      return status
    }

    private fun buildContext(
      ctx: WorkerContext.TaskContext,
      args: List<String>,
    ): Pair<ArgMap, CompilationTaskContext> {
      check(args.isNotEmpty()) { "expected at least a single arg got: ${args.joinToString(" ")}" }
      val lines =
        FLAGFILE_RE.matchEntire(args[0])?.groups?.get(1)?.let {
          Files.readAllLines(FileSystems.getDefault().getPath(it.value), StandardCharsets.UTF_8)
        } ?: args

      val argMap = ArgMaps.from(lines)
      val info = buildTaskInfo(argMap).build()
      val context =
        CompilationTaskContext(info, ctx.asPrintStream())
      return Pair(argMap, context)
    }

    private fun buildTaskInfo(argMap: ArgMap): CompilationTaskInfo.Builder =
      with(CompilationTaskInfo.newBuilder()) {
        addAllDebug(argMap.mandatory(KotlinBuilderFlags.DEBUG))

        label = argMap.mandatorySingle(KotlinBuilderFlags.TARGET_LABEL)
        argMap.mandatorySingle(KotlinBuilderFlags.RULE_KIND).split("_").also {
          check(it.size == 3 && it[0] == "kt") { "invalid rule kind $it" }
          platform =
            checkNotNull(Platform.valueOf(it[1].uppercase())) {
              "unrecognized platform ${it[1]}"
            }
          ruleKind =
            checkNotNull(RuleKind.valueOf(it[2].uppercase())) {
              "unrecognized rule kind ${it[2]}"
            }
        }
        moduleName =
          argMap.mandatorySingle(KotlinBuilderFlags.MODULE_NAME).also {
            check(it.isNotBlank()) { "--kotlin_module_name should not be blank" }
          }
        addAllPassthroughFlags(argMap.optional(KotlinBuilderFlags.PASSTHROUGH_FLAGS) ?: emptyList())

        argMap.optional(KotlinBuilderFlags.FRIEND_PATHS)?.let(::addAllFriendPaths)
        toolchainInfoBuilder.commonBuilder.apiVersion =
          argMap.mandatorySingle(KotlinBuilderFlags.API_VERSION)
        toolchainInfoBuilder.commonBuilder.languageVersion =
          argMap.mandatorySingle(KotlinBuilderFlags.LANGUAGE_VERSION)
        strictKotlinDeps = argMap.mandatorySingle(KotlinBuilderFlags.STRICT_KOTLIN_DEPS)
        reducedClasspathMode = argMap.mandatorySingle(KotlinBuilderFlags.REDUCED_CLASSPATH_MODE)
        argMap.optionalSingle(KotlinBuilderFlags.ABI_JAR_INTERNAL_AS_PRIVATE)?.let {
          treatInternalAsPrivateInAbiJar = it == "true"
        }
        argMap.optionalSingle(KotlinBuilderFlags.ABI_JAR_REMOVE_PRIVATE_CLASSES)?.let {
          removePrivateClassesInAbiJar = it == "true"
        }
        this
      }

    private fun executeJvmTask(
      context: CompilationTaskContext,
      workingDir: Path,
      argMap: ArgMap,
    ) {
      val task = buildJvmTask(context.info, workingDir, argMap)
      context.whenTracing {
        printProto("jvm task message:", task)
      }
      jvmTaskExecutor.execute(context, task)
    }

    private fun buildJvmTask(
      info: CompilationTaskInfo,
      workingDir: Path,
      argMap: ArgMap,
    ): JvmCompilationTask =
      JvmCompilationTask.newBuilder().let { root ->
        root.info = info

        root.compileKotlin = argMap.mandatorySingle(KotlinBuilderFlags.BUILD_KOTLIN).toBoolean()
        root.instrumentCoverage =
          argMap
            .mandatorySingle(
              KotlinBuilderFlags.INSTRUMENT_COVERAGE,
            ).toBoolean()

        with(root.outputsBuilder) {
          argMap.optionalSingle(KotlinBuilderFlags.OUTPUT)?.let { jar = it }
          argMap.optionalSingle(KotlinBuilderFlags.OUTPUT_SRCJAR)?.let { srcjar = it }

          argMap.optionalSingle(KotlinBuilderFlags.OUTPUT_JDEPS)?.apply { jdeps = this }
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
          argMap.optionalSingle(KotlinBuilderFlags.KSP_GENERATED_JAVA_SRCJAR)?.let {
            generatedKspSrcJar = it
          }
        }

        with(root.directoriesBuilder) {
          val moduleName = argMap.mandatorySingle(KotlinBuilderFlags.MODULE_NAME)
          classes =
            workingDir.resolveNewDirectories(getOutputDirPath(moduleName, "classes")).toString()
          javaClasses =
            workingDir
              .resolveNewDirectories(
                getOutputDirPath(moduleName, "java_classes"),
              ).toString()
          if (argMap.hasAll(KotlinBuilderFlags.ABI_JAR)) {
            abiClasses =
              workingDir
                .resolveNewDirectories(
                  getOutputDirPath(moduleName, "abi_classes"),
                ).toString()
          }
          generatedClasses =
            workingDir
              .resolveNewDirectories(getOutputDirPath(moduleName, "generated_classes"))
              .toString()
          temp =
            workingDir
              .resolveNewDirectories(
                getOutputDirPath(moduleName, "temp"),
              ).toString()
          generatedSources =
            workingDir
              .resolveNewDirectories(getOutputDirPath(moduleName, "generated_sources"))
              .toString()
          generatedJavaSources =
            workingDir
              .resolveNewDirectories(getOutputDirPath(moduleName, "generated_java_sources"))
              .toString()
          generatedStubClasses =
            workingDir.resolveNewDirectories(getOutputDirPath(moduleName, "stubs")).toString()
          coverageMetadataClasses =
            workingDir
              .resolveNewDirectories(getOutputDirPath(moduleName, "coverage-metadata"))
              .toString()
        }

        with(root.inputsBuilder) {
          addAllClasspath(argMap.optional(KotlinBuilderFlags.FRIEND_PATHS) ?: emptyList())
          addAllClasspath(argMap.mandatory(KotlinBuilderFlags.CLASSPATH))
          addAllDepsArtifacts(
            argMap.optional(KotlinBuilderFlags.DEPS_ARTIFACTS) ?: emptyList(),
          )
          addAllDirectDependencies(argMap.mandatory(KotlinBuilderFlags.DIRECT_DEPENDENCIES))

          addAllProcessors(argMap.optional(KotlinBuilderFlags.PROCESSORS) ?: emptyList())
          addAllProcessorpaths(argMap.optional(KotlinBuilderFlags.PROCESSOR_PATH) ?: emptyList())

          addAllStubsPluginOptions(
            argMap.optional(KotlinBuilderFlags.STUBS_PLUGIN_OPTIONS) ?: emptyList(),
          )
          addAllStubsPluginClasspath(
            argMap.optional(KotlinBuilderFlags.STUBS_PLUGIN_CLASS_PATH) ?: emptyList(),
          )

          addAllCompilerPluginOptions(
            argMap.optional(KotlinBuilderFlags.COMPILER_PLUGIN_OPTIONS) ?: emptyList(),
          )
          addAllCompilerPluginClasspath(
            argMap.optional(KotlinBuilderFlags.COMPILER_PLUGIN_CLASS_PATH) ?: emptyList(),
          )

          argMap
            .optional(KotlinBuilderFlags.SOURCES)
            ?.iterator()
            ?.partitionJvmSources(
              { addKotlinSources(it) },
              { addJavaSources(it) },
            )
          argMap
            .optional(KotlinBuilderFlags.SOURCE_JARS)
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

    private fun getOutputDirPath(
      moduleName: String,
      dirName: String,
    ) = "_kotlinc/${moduleName}_jvm/$dirName"
  }
