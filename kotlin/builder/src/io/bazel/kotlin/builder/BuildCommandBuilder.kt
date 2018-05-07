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
package io.bazel.kotlin.builder

import com.google.inject.ImplementedBy
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.protobuf.util.JsonFormat
import io.bazel.kotlin.builder.utils.ArgMap
import io.bazel.kotlin.builder.utils.DefaultKotlinCompilerPluginArgsEncoder
import io.bazel.kotlin.model.KotlinModel
import io.bazel.kotlin.model.KotlinModel.BuilderCommand


@ImplementedBy(DefaultBuildCommandBuilder::class)
interface BuildCommandBuilder {
    fun fromInput(argMap: ArgMap): BuilderCommand
    fun withSources(command: BuilderCommand, sources: Iterator<String>): BuilderCommand
    fun withGeneratedSources(command: BuilderCommand, sources: Iterator<String>): BuilderCommand
}

@Singleton
private class DefaultBuildCommandBuilder @Inject constructor(
    private val pluginEncoder: DefaultKotlinCompilerPluginArgsEncoder
) : BuildCommandBuilder {
    companion object {
        @JvmStatic
        private val jsonTypeRegistry = JsonFormat.TypeRegistry.newBuilder()
            .add(KotlinModel.getDescriptor().messageTypes).build()


        @JvmStatic
        private val jsonFormat: JsonFormat.Parser = JsonFormat.parser().usingTypeRegistry(jsonTypeRegistry)
    }

    /**
     * Declares the flags used by the java builder.
     */
    private enum class JavaBuilderFlags(val flag: String) {
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
        TEST_ONLY("--testonly")
    }

    override fun fromInput(argMap: ArgMap): BuilderCommand =
        BuilderCommand.newBuilder().let { root ->
            with(root.outputsBuilder) {
                classDirectory = argMap.mandatorySingle(JavaBuilderFlags.CLASSDIR.flag)
                tempDirectory = argMap.mandatorySingle(JavaBuilderFlags.TEMPDIR.flag)
                sourceGenDir = argMap.mandatorySingle(JavaBuilderFlags.SOURCEGEN_DIR.flag)
                output = argMap.mandatorySingle(JavaBuilderFlags.OUTPUT.flag)
                outputJdeps = argMap.mandatorySingle("--output_jdeps")
            }

            with(root.inputsBuilder) {
                addAllClasspath(argMap.mandatory(JavaBuilderFlags.CLASSPATH.flag))
                putAllIndirectDependencies(argMap.labelDepMap(JavaBuilderFlags.DIRECT_DEPENDENCY.flag))
                putAllIndirectDependencies(argMap.labelDepMap(JavaBuilderFlags.INDIRECT_DEPENDENCY.flag))

                argMap.optional(JavaBuilderFlags.SOURCES.flag)?.iterator()?.partitionSources(
                    { addKotlinSources(it) },
                    { addJavaSources(it) }
                )
                argMap.optional(JavaBuilderFlags.SOURCE_JARS.flag)?.also {
                    addAllSourceJars(it)
                }


                joinedClasspath = classpathList.joinToString(":")
            }

            with(root.infoBuilder) {
                label = argMap.mandatorySingle(JavaBuilderFlags.TARGET_LABEL.flag)
                ruleKind = argMap.mandatorySingle(JavaBuilderFlags.RULE_KIND.flag)
                kotlinModuleName = argMap.optionalSingle("--kotlin_module_name")
                passthroughFlags = argMap.optionalSingle("--kotlin_passthrough_flags")
                toolchainInfoBuilder.commonBuilder.apiVersion = argMap.mandatorySingle("--kotlin_api_version")
                toolchainInfoBuilder.commonBuilder.languageVersion = argMap.mandatorySingle("--kotlin_language_version")
                toolchainInfoBuilder.jvmBuilder.jvmTarget = argMap.mandatorySingle("--kotlin_jvm_target")

                argMap.optionalSingle("--kt-plugins")?.let { input ->
                    plugins = KotlinModel.CompilerPlugins.newBuilder().let {
                        jsonFormat.merge(input, it)
                        it.build()
                    }
                }

                if (plugins.annotationProcessorsList.isNotEmpty()) {
                    addAllEncodedPluginDescriptors(pluginEncoder.encode(root.outputs, root.info.plugins))
                }

                label.split(":").also {
                    check(it.size == 2) { "the label ${root.info.label} is invalid" }
                    `package` = it[0]
                    target = it[1]
                }

                kotlinModuleName = kotlinModuleName.supplyIfNullOrBlank {
                    "${`package`.trimStart { it == '/' }.replace('/', '_')}-$target"
                }
            }
            root.build()
        }

    override fun withSources(command: BuilderCommand, sources: Iterator<String>): BuilderCommand =
        command.updateBuilder { builder ->
            sources.partitionSources(
                { builder.inputsBuilder.addGeneratedKotlinSources(it) },
                { builder.inputsBuilder.addGeneratedJavaSources(it) })
        }


    override fun withGeneratedSources(command: BuilderCommand, sources: Iterator<String>): BuilderCommand =
        command.updateBuilder { builder ->
            sources.partitionSources(
                { builder.inputsBuilder.addGeneratedKotlinSources(it) },
                { builder.inputsBuilder.addGeneratedJavaSources(it) })
        }

    private fun BuilderCommand.updateBuilder(init: (BuilderCommand.Builder) -> Unit): BuilderCommand =
        toBuilder().let {
            init(it)
            it.build()
        }


    private fun Iterator<String>.partitionSources(kt: (String) -> Unit, java: (String) -> Unit) {
        forEach {
            when {
                it.endsWith(".kt") -> kt(it)
                it.endsWith(".java") -> java(it)
                else -> throw IllegalStateException("invalid source file type $it")
            }
        }
    }

    private fun String?.supplyIfNullOrBlank(s: () -> String): String = this?.takeIf { it.isNotBlank() } ?: s()
}

