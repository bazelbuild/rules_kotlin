/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.kotlin.builder.tasks

import io.bazel.kotlin.builder.tasks.js.Kotlin2JsTaskExecutor
import io.bazel.kotlin.builder.tasks.jvm.KotlinJvmTaskExecutor
import io.bazel.kotlin.builder.toolchain.CompilationStatusException
import io.bazel.kotlin.builder.utils.*
import io.bazel.kotlin.model.*
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
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
    }

    override fun apply(args: List<String>): Int {
        val (argMap, context) = buildContext(args)
        var success = false
        var status = 0
        try {
            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
            when (context.info.platform) {
                Platform.JVM -> executeJvmTask(context, argMap)
                Platform.JS -> executeJsTask(context, argMap)
                Platform.UNRECOGNIZED -> throw IllegalStateException("unrecognized platform: ${context.info}")
            }
            success = true
        } catch (ex: CompilationStatusException) {
            System.err.println("Compilation failure: ${ex.message}")
            status = ex.status
        } catch (throwable: Throwable) {
            context.reportUnhandledException(throwable)
            throw throwable
        } finally {
            context.finalize(success)
        }
        return status
    }

    private fun buildContext(args: List<String>): Pair<ArgMap, CompilationTaskContext> {
        check(args.isNotEmpty()) { "expected at least a single arg got: ${args.joinToString(" ")}" }
        val (flagFileName, primaryOutputPath, _) =
                checkNotNull(FLAGFILE_RE.matchEntire(args[0])) { "invalid flagfile ${args[0]}" }.destructured
        val argMap = Files.readAllLines(Paths.get(flagFileName), StandardCharsets.UTF_8).let(ArgMaps::from)
        val info = buildTaskInfo(argMap).also {
            it.primaryOutputPath = primaryOutputPath
        }.build()
        val context = CompilationTaskContext(info, outputProvider.get())
        return Pair(argMap, context)
    }

    /**
     * Declares the flags used by the java builder.
     */
    private enum class JavaBuilderFlags(override val flag: String) : Flag {
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
        TEST_ONLY("--testonly");
    }

    private enum class KotlinBuilderFlags(override val flag: String) : Flag {
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
        TASK_ID("--kotlin_task_id");
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
            passthroughFlags = argMap.optionalSingle(KotlinBuilderFlags.PASSTHROUGH_FLAGS)
            argMap.optional(KotlinBuilderFlags.FRIEND_PATHS)?.let(::addAllFriendPaths)
            toolchainInfoBuilder.commonBuilder.apiVersion = argMap.mandatorySingle(KotlinBuilderFlags.API_VERSION)
            toolchainInfoBuilder.commonBuilder.languageVersion = argMap.mandatorySingle(KotlinBuilderFlags.LANGUAGE_VERSION)
            this
        }

    private fun executeJsTask(context: CompilationTaskContext, argMap: ArgMap) =
        buildJsTask(context.info, argMap).let { jsTask ->
            context.whenTracing { printProto("js task input", jsTask) }
            jsTaskExecutor.execute(context, jsTask)
        }

    private fun buildJsTask(info: CompilationTaskInfo, argMap: ArgMap): JsCompilationTask =
        with(JsCompilationTask.newBuilder()) {
            this.info = info
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

    private fun executeJvmTask(context: CompilationTaskContext, argMap: ArgMap) {
        val task = buildJvmTask(context.info, argMap)
        context.whenTracing {
            printProto("jvm task message", task)
        }
        jvmTaskExecutor.execute(context, task)
    }

    private fun buildJvmTask(info: CompilationTaskInfo, argMap: ArgMap): JvmCompilationTask =
        JvmCompilationTask.newBuilder().let { root ->
            root.info = info

            with(root.outputsBuilder) {
                jar = argMap.mandatorySingle(JavaBuilderFlags.OUTPUT)
                jdeps = argMap.mandatorySingle(KotlinBuilderFlags.OUTPUT_JDEPS)
                srcjar = argMap.mandatorySingle(KotlinBuilderFlags.OUTPUT_SRCJAR)
            }

            with(root.directoriesBuilder) {
                classes = argMap.mandatorySingle(JavaBuilderFlags.CLASSDIR)
                generatedClasses = argMap.mandatorySingle(KotlinBuilderFlags.GENERATED_CLASSDIR)
                temp = argMap.mandatorySingle(JavaBuilderFlags.TEMPDIR)
                generatedSources = argMap.mandatorySingle(JavaBuilderFlags.SOURCEGEN_DIR)
            }

            with(root.inputsBuilder) {
                addAllClasspath(argMap.mandatory(JavaBuilderFlags.CLASSPATH))
                putAllIndirectDependencies(argMap.labelDepMap(JavaBuilderFlags.DIRECT_DEPENDENCY))
                putAllIndirectDependencies(argMap.labelDepMap(JavaBuilderFlags.INDIRECT_DEPENDENCY))

                addAllProcessors(argMap.optional(JavaBuilderFlags.PROCESSORS) ?: emptyList())
                addAllProcessorpaths(argMap.optional(JavaBuilderFlags.PROCESSOR_PATH) ?: emptyList())

                argMap.optional(JavaBuilderFlags.SOURCES)?.iterator()?.partitionJvmSources(
                    { addKotlinSources(it) },
                    { addJavaSources(it) }
                )
                argMap.optional(JavaBuilderFlags.SOURCE_JARS)?.also {
                    addAllSourceJars(it)
                }
            }

            with(root.infoBuilder) {
                toolchainInfoBuilder.jvmBuilder.jvmTarget = argMap.mandatorySingle(KotlinBuilderFlags.JVM_TARGET)
            }
            root.build()
        }
}
