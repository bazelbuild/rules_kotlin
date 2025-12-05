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
import java.io.File
import java.io.FileOutputStream
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.ServiceLoader
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.regex.Pattern
import java.util.zip.ZipFile
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
        ABI_JAR_REMOVE_DEBUG_INFO("--remove_debug_info_in_abi_jar"),
        GENERATED_JAVA_SRC_JAR("--generated_java_srcjar"),
        GENERATED_JAVA_STUB_JAR("--kapt_generated_stub_jar"),
        GENERATED_CLASS_JAR("--kapt_generated_class_jar"),
        BUILD_KOTLIN("--build_kotlin"),
        STRICT_KOTLIN_DEPS("--strict_kotlin_deps"),
        REDUCED_CLASSPATH_MODE("--reduced_classpath_mode"),
        INSTRUMENT_COVERAGE("--instrument_coverage"),
        KSP_GENERATED_JAVA_SRCJAR("--ksp_generated_java_srcjar"),
        KSP_GENERATED_CLASSES_JAR("--ksp_generated_classes_jar"),
        BUILD_TOOLS_API("--build_tools_api"),

        // KSP2 mode flags - simplified: inputs are sources/srcjars, outputs are jars
        KSP2_MODE("--ksp2_mode"),
        KSP2_MODULE_NAME("--ksp2_module_name"),
        KSP2_SOURCES("--ksp2_sources"),
        KSP2_SOURCE_JARS("--ksp2_source_jars"),
        KSP2_LIBRARIES("--ksp2_libraries"),
        KSP2_PROCESSOR_CLASSPATH("--ksp2_processor_classpath"),
        KSP2_GENERATED_SOURCES_OUTPUT("--ksp2_generated_sources_output"),
        KSP2_GENERATED_CLASSES_OUTPUT("--ksp2_generated_classes_output"),
        KSP2_LANGUAGE_VERSION("--ksp2_language_version"),
        KSP2_API_VERSION("--ksp2_api_version"),
        KSP2_JVM_TARGET("--ksp2_jvm_target"),
        KSP2_JDK_HOME("--ksp2_jdk_home"),
      }
    }

    fun build(
      taskContext: WorkerContext.TaskContext,
      args: List<String>,
    ): Int {
      check(args.isNotEmpty()) { "expected at least a single arg got: ${args.joinToString(" ")}" }
      val lines =
        FLAGFILE_RE.matchEntire(args[0])?.groups?.get(1)?.let {
          Files.readAllLines(FileSystems.getDefault().getPath(it.value), StandardCharsets.UTF_8)
        } ?: args

      val argMap = ArgMaps.from(lines)

      // Check if this is a KSP2 mode request
      if (argMap.optionalSingle(KotlinBuilderFlags.KSP2_MODE) == "true") {
        return executeKsp2Task(taskContext, argMap)
      }

      val (_, compileContext) = buildContext(taskContext, argMap)
      var success = false
      var status = 0
      try {
        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        when (compileContext.info.platform) {
          Platform.JVM,
          Platform.ANDROID,
          -> executeJvmTask(compileContext, taskContext.directory, argMap)

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
      argMap: ArgMap,
    ): Pair<ArgMap, CompilationTaskContext> {
      val info = buildTaskInfo(argMap).build()
      val context =
        CompilationTaskContext(info, ctx.asPrintStream())
      return Pair(argMap, context)
    }

    private fun buildTaskInfo(argMap: ArgMap): CompilationTaskInfo.Builder =
      with(CompilationTaskInfo.newBuilder()) {
        addAllDebug(argMap.mandatory(KotlinBuilderFlags.DEBUG))

        label = argMap.mandatorySingle(KotlinBuilderFlags.TARGET_LABEL)
        argMap.mandatorySingle(KotlinBuilderFlags.RULE_KIND).also {
          val splitRuleKind = it.split("_")
          require(splitRuleKind[0] == "kt") { "Invalid rule kind $it" }
          platform = Platform.valueOf(splitRuleKind[1].uppercase())
          ruleKind = RuleKind.valueOf(splitRuleKind.last().uppercase())
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
        argMap.optionalSingle(KotlinBuilderFlags.ABI_JAR_REMOVE_DEBUG_INFO)?.let {
          removeDebugInfo = it == "true"
        }
        argMap.optionalSingle(KotlinBuilderFlags.BUILD_TOOLS_API)?.let {
          buildToolsApi = it == "true"
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
          argMap.optionalSingle(KotlinBuilderFlags.KSP_GENERATED_CLASSES_JAR)?.let {
            generatedKspClassesJar = it
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

    /**
     * Execute KSP2 processing entirely within the worker.
     *
     * This method handles:
     * 1. Staging source files to a temporary directory (for worker isolation)
     * 2. Unpacking srcjars to a temporary directory
     * 3. Running KSP2 via reflection
     * 4. Packaging generated sources/classes into output JARs
     *
     * All temporary directories are created inside the worker's working directory,
     * avoiding tree artifacts and minimizing action count.
     */
    @Suppress("UNCHECKED_CAST")
    private fun executeKsp2Task(
      taskContext: WorkerContext.TaskContext,
      argMap: ArgMap,
    ): Int {
      val workingDir = taskContext.directory
      val moduleName = argMap.mandatorySingle(KotlinBuilderFlags.KSP2_MODULE_NAME)

      // Create temporary directories for KSP2 processing
      val kspWorkDir = workingDir.resolve("_ksp2/$moduleName")
      val stagedSourcesDir = kspWorkDir.resolve("staged_sources")
      val kotlinOutputDir = kspWorkDir.resolve("kotlin_out")
      val javaOutputDir = kspWorkDir.resolve("java_out")
      val classOutputDir = kspWorkDir.resolve("class_out")
      val resourceOutputDir = kspWorkDir.resolve("resource_out")
      val cachesDir = kspWorkDir.resolve("caches")

      listOf(
        stagedSourcesDir,
        kotlinOutputDir,
        javaOutputDir,
        classOutputDir,
        resourceOutputDir,
        cachesDir,
      ).forEach {
        Files.createDirectories(it)
      }

      try {
        // Stage source files to isolated directory
        val sourceRoots = mutableSetOf<String>()
        val javaSourceRoots = mutableSetOf<String>()

        // Stage individual source files
        val sources = argMap.optional(KotlinBuilderFlags.KSP2_SOURCES) ?: emptyList()
        for (source in sources) {
          val sourceFile = File(source)
          val targetFile = stagedSourcesDir.resolve(source).toFile()
          targetFile.parentFile?.mkdirs()
          sourceFile.copyTo(targetFile, overwrite = true)

          // Track source roots (directories containing sources)
          val sourceRoot =
            if (sourceFile.parentFile != null) {
              stagedSourcesDir.resolve(sourceFile.parentFile.path).toString()
            } else {
              stagedSourcesDir.toString()
            }
          sourceRoots.add(sourceRoot)
          if (source.endsWith(".java")) {
            javaSourceRoots.add(sourceRoot)
          }
        }

        // Unpack srcjars directly
        val srcjars = argMap.optional(KotlinBuilderFlags.KSP2_SOURCE_JARS) ?: emptyList()
        for (srcjar in srcjars) {
          ZipFile(srcjar).use { zip ->
            zip.entries().asSequence().forEach { entry ->
              if (!entry.isDirectory) {
                val targetFile = stagedSourcesDir.resolve(entry.name).toFile()
                targetFile.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                  targetFile.outputStream().use { output ->
                    input.copyTo(output)
                  }
                }
                // Track source root for srcjar contents
                val parentDir = targetFile.parentFile?.path ?: stagedSourcesDir.toString()
                sourceRoots.add(parentDir)
                if (entry.name.endsWith(".java")) {
                  javaSourceRoots.add(parentDir)
                }
              }
            }
          }
        }

        // If no sources, add a placeholder source root
        if (sourceRoots.isEmpty()) {
          sourceRoots.add(stagedSourcesDir.toString())
        }

        // Load KSP2 via reflection
        val processorClasspath =
          argMap.optional(KotlinBuilderFlags.KSP2_PROCESSOR_CLASSPATH) ?: emptyList()
        val processorUrls = processorClasspath.map { File(it).toURI().toURL() }.toTypedArray()
        val kspClassLoader = URLClassLoader(processorUrls, ClassLoader.getSystemClassLoader())

        val symbolProcessorProviderClass =
          kspClassLoader.loadClass("com.google.devtools.ksp.processing.SymbolProcessorProvider")
        val kspJvmConfigBuilderClass =
          kspClassLoader.loadClass("com.google.devtools.ksp.processing.KSPJvmConfig\$Builder")
        val kspGradleLoggerClass =
          kspClassLoader.loadClass("com.google.devtools.ksp.processing.KspGradleLogger")
        val kotlinSymbolProcessingClass =
          kspClassLoader.loadClass("com.google.devtools.ksp.impl.KotlinSymbolProcessing")
        val kspConfigClass =
          kspClassLoader.loadClass("com.google.devtools.ksp.processing.KSPConfig")
        val kspLoggerClass =
          kspClassLoader.loadClass("com.google.devtools.ksp.processing.KSPLogger")

        // Find processor implementations
        val processors =
          ServiceLoader.load(symbolProcessorProviderClass, kspClassLoader).toList()

        // Build KSP2 configuration
        val configBuilder = kspJvmConfigBuilderClass.getConstructor().newInstance()

        fun setProperty(
          name: String,
          value: Any?,
        ) {
          if (value == null) return
          val setterName = "set${name.replaceFirstChar { c -> c.uppercase() }}"
          val setter =
            kspJvmConfigBuilderClass.methods.find { it.name == setterName }
              ?: throw NoSuchMethodException("No setter $setterName found")
          setter.invoke(configBuilder, value)
        }

        setProperty("moduleName", moduleName)
        setProperty("sourceRoots", sourceRoots.map { File(it) })
        if (javaSourceRoots.isNotEmpty()) {
          setProperty("javaSourceRoots", javaSourceRoots.map { File(it) })
        }

        argMap.optional(KotlinBuilderFlags.KSP2_LIBRARIES)?.let { libs ->
          setProperty("libraries", libs.map { File(it) })
        }

        setProperty("kotlinOutputDir", kotlinOutputDir.toFile())
        setProperty("javaOutputDir", javaOutputDir.toFile())
        setProperty("classOutputDir", classOutputDir.toFile())
        setProperty("resourceOutputDir", resourceOutputDir.toFile())
        setProperty("cachesDir", cachesDir.toFile())
        // projectBaseDir and outputBaseDir must share the same root as output directories
        // to allow KSP2 to compute relative paths correctly
        setProperty("projectBaseDir", kspWorkDir.toFile())
        setProperty("outputBaseDir", kspWorkDir.toFile())

        argMap.optionalSingle(KotlinBuilderFlags.KSP2_JVM_TARGET)?.let {
          setProperty("jvmTarget", it)
        }
        argMap.optionalSingle(KotlinBuilderFlags.KSP2_LANGUAGE_VERSION)?.let {
          setProperty("languageVersion", it)
        }
        argMap.optionalSingle(KotlinBuilderFlags.KSP2_API_VERSION)?.let {
          setProperty("apiVersion", it)
        }
        argMap.optionalSingle(KotlinBuilderFlags.KSP2_JDK_HOME)?.let {
          setProperty("jdkHome", File(it))
        }
        setProperty("mapAnnotationArgumentsInJava", true)

        // Build and run KSP2
        val buildMethod = kspJvmConfigBuilderClass.getMethod("build")
        val config = buildMethod.invoke(configBuilder)

        val loggerConstructor = kspGradleLoggerClass.getConstructor(Int::class.java)
        val logger = loggerConstructor.newInstance(1) // WARN level

        val kspConstructor =
          kotlinSymbolProcessingClass.getConstructor(
            kspConfigClass,
            List::class.java,
            kspLoggerClass,
          )
        val ksp = kspConstructor.newInstance(config, processors, logger)

        val executeMethod = kotlinSymbolProcessingClass.getMethod("execute")
        val exitCode = executeMethod.invoke(ksp)

        val exitCodeClass = exitCode.javaClass
        val codeField = exitCodeClass.getDeclaredField("code")
        codeField.isAccessible = true
        val code = codeField.getInt(exitCode)

        if (code != 0) {
          taskContext.error { "KSP2 failed with exit code: $code" }
          return code
        }

        // Package generated sources into srcjar
        val generatedSourcesOutput =
          argMap.mandatorySingle(KotlinBuilderFlags.KSP2_GENERATED_SOURCES_OUTPUT)
        packageDirectoriesToJar(
          outputPath = generatedSourcesOutput,
          directories = listOf(kotlinOutputDir, javaOutputDir),
        )

        // Package generated classes/resources into jar
        val generatedClassesOutput =
          argMap.mandatorySingle(KotlinBuilderFlags.KSP2_GENERATED_CLASSES_OUTPUT)
        packageDirectoriesToJar(
          outputPath = generatedClassesOutput,
          directories = listOf(classOutputDir, resourceOutputDir),
        )

        taskContext.info { "KSP2 completed successfully" }
        return 0
      } catch (e: Exception) {
        taskContext.error(e) { "KSP2 execution failed" }
        return 1
      } finally {
        // Clean up temporary directories
        try {
          kspWorkDir.toFile().deleteRecursively()
        } catch (_: Exception) {
          // Ignore cleanup errors
        }
      }
    }

    /**
     * Package files from directories into a JAR file.
     */
    private fun packageDirectoriesToJar(
      outputPath: String,
      directories: List<Path>,
    ) {
      val manifest =
        Manifest().apply {
          mainAttributes.putValue("Manifest-Version", "1.0")
          mainAttributes.putValue("Created-By", "rules_kotlin KSP2")
        }

      JarOutputStream(FileOutputStream(outputPath), manifest).use { jar ->
        val addedEntries = mutableSetOf<String>()

        for (dir in directories) {
          if (!Files.exists(dir)) continue

          Files.walk(dir).use { stream ->
            stream.forEach { path ->
              if (Files.isRegularFile(path)) {
                val relativePath = dir.relativize(path).toString().replace('\\', '/')
                if (relativePath !in addedEntries) {
                  addedEntries.add(relativePath)
                  jar.putNextEntry(JarEntry(relativePath))
                  Files.copy(path, jar)
                  jar.closeEntry()
                }
              }
            }
          }
        }
      }
    }
  }
