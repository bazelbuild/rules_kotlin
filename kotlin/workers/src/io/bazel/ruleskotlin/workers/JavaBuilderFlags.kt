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
package io.bazel.ruleskotlin.workers

/**
 * Flags used by the java builder.
 */
enum class JavaBuilderFlags(val flag: String) {
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
