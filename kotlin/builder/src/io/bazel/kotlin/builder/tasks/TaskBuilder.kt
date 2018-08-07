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

import com.google.protobuf.util.JsonFormat
import io.bazel.kotlin.builder.tasks.TaskBuilder.JavaBuilderFlags.*
import io.bazel.kotlin.builder.tasks.TaskBuilder.KotlinBuilderFlags.*
import io.bazel.kotlin.builder.utils.*
import io.bazel.kotlin.model.*
import io.bazel.kotlin.model.KotlinModel.getDescriptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskBuilder @Inject internal constructor() {
    companion object {
        @JvmStatic
        private val jsonTypeRegistry = JsonFormat.TypeRegistry.newBuilder()
            .add(getDescriptor().messageTypes).build()

        @JvmStatic
        private val jsonFormat: JsonFormat.Parser = JsonFormat.parser().usingTypeRegistry(jsonTypeRegistry)
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
        PROCESS_PATH("--processorpath"),
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

    enum class KotlinBuilderFlags(override val flag: String) : Flag {
        MODULE_NAME("--kotlin_module_name"),
        PASSTHROUGH_FLAGS("--kotlin_passthrough_flags"),
        API_VERSION("--kotlin_api_version"),
        LANGUAGE_VERSION("--kotlin_language_version"),
        JVM_TARGET("--kotlin_jvm_target"),
        OUTPUT_SRCJAR("--kotlin_output_srcjar"),
        GENERATED_CLASSDIR("--kotlin_generated_classdir"),
        PLUGINS("--kotlin_plugins"),
        FRIEND_PATHS("--kotlin_friend_paths"),
        OUTPUT_JDEPS("--kotlin_output_jdeps"),
        TASK_ID("--kotlin_task_id");
    }

    fun buildTaskInfo(argMap: ArgMap): CompilationTaskInfo =
        with(CompilationTaskInfo.newBuilder()) {
            label = argMap.mandatorySingle(TARGET_LABEL)
            argMap.mandatorySingle(RULE_KIND).split("_").also {
                check(it.size == 3 && it[0] == "kt") { "invalid rule kind $it" }
                platform = checkNotNull(Platform.valueOf(it[1].toUpperCase())) {
                    "unrecognized platform ${it[1]}"
                }
                ruleKind = checkNotNull(RuleKind.valueOf(it[2].toUpperCase())) {
                    "unrecognized rule kind ${it[2]}"
                }
            }
            moduleName = argMap.mandatorySingle(MODULE_NAME).also {
                check(it.isNotBlank()) { "--kotlin_module_name should not be blank" }
            }
            passthroughFlags = argMap.optionalSingle(PASSTHROUGH_FLAGS)
            argMap.optional(FRIEND_PATHS)?.let(::addAllFriendPaths)
            toolchainInfoBuilder.commonBuilder.apiVersion = argMap.mandatorySingle(API_VERSION)
            toolchainInfoBuilder.commonBuilder.languageVersion = argMap.mandatorySingle(LANGUAGE_VERSION)
            build()
        }


    fun buildJvm(info: CompilationTaskInfo, argMap: ArgMap): JvmCompilationTask =
        JvmCompilationTask.newBuilder().let { root ->
            root.info = info

            with(root.outputsBuilder) {
                jar = argMap.mandatorySingle(OUTPUT)
                jdeps = argMap.mandatorySingle(OUTPUT_JDEPS)
                srcjar = argMap.mandatorySingle(OUTPUT_SRCJAR)
            }

            with(root.directoriesBuilder) {
                classes = argMap.mandatorySingle(CLASSDIR)
                generatedClasses = argMap.mandatorySingle(GENERATED_CLASSDIR)
                temp = argMap.mandatorySingle(TEMPDIR)
                generatedSources = argMap.mandatorySingle(SOURCEGEN_DIR)
            }

            with(root.inputsBuilder) {
                addAllClasspath(argMap.mandatory(CLASSPATH))
                putAllIndirectDependencies(argMap.labelDepMap(DIRECT_DEPENDENCY))
                putAllIndirectDependencies(argMap.labelDepMap(INDIRECT_DEPENDENCY))

                argMap.optional(SOURCES)?.iterator()?.partitionSources(
                    { addKotlinSources(it) },
                    { addJavaSources(it) }
                )
                argMap.optional(SOURCE_JARS)?.also {
                    addAllSourceJars(it)
                }
            }

            with(root.infoBuilder) {
                toolchainInfoBuilder.jvmBuilder.jvmTarget = argMap.mandatorySingle(JVM_TARGET)

                argMap.optionalSingle(PLUGINS)?.let { input ->
                    plugins = CompilerPlugins.newBuilder().let {
                        jsonFormat.merge(input, it)
                        it.build()
                    }
                }
            }

            root.build()
        }
}

