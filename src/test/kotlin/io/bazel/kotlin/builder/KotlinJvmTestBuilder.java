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
package io.bazel.kotlin.builder;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.bazel.kotlin.builder.Deps.AnnotationProcessor;
import io.bazel.kotlin.builder.Deps.Dep;
import io.bazel.kotlin.builder.toolchain.CompilationTaskContext;
import io.bazel.kotlin.builder.toolchain.KotlinToolchain;
import io.bazel.kotlin.model.CompilationTaskInfo;
import io.bazel.kotlin.model.JvmCompilationTask;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class KotlinJvmTestBuilder extends KotlinAbstractTestBuilder<JvmCompilationTask> {

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static Dep
            KOTLIN_ANNOTATIONS = Dep.fromLabel("@com_github_jetbrains_kotlin//:annotations"),
            KOTLIN_STDLIB = Dep.fromLabel("@com_github_jetbrains_kotlin//:kotlin-stdlib"),
            KOTLIN_STDLIB_JDK7 = Dep.fromLabel("@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk7"),
            KOTLIN_STDLIB_JDK8 = Dep.fromLabel("@com_github_jetbrains_kotlin//:kotlin-stdlib-jdk8"),
            JVM_ABI_GEN = Dep.fromLabel("@com_github_jetbrains_kotlin//:jvm-abi-gen");

    private static final JvmCompilationTask.Builder taskBuilder = JvmCompilationTask.newBuilder();
    private static final EnumSet<DirectoryType> ALL_DIRECTORY_TYPES =
            EnumSet.of(
                    DirectoryType.SOURCES,
                    DirectoryType.CLASSES,
                    DirectoryType.SOURCE_GEN,
                    DirectoryType.JAVA_SOURCE_GEN,
                    DirectoryType.GENERATED_CLASSES,
                    DirectoryType.TEMP);

    private TaskBuilder taskBuilderInstance = new TaskBuilder();

    @Override
    void setupForNext(CompilationTaskInfo.Builder taskInfo) {
        taskBuilder.clear().setInfo(taskInfo);

        DirectoryType.createAll(instanceRoot(), ALL_DIRECTORY_TYPES);

        taskBuilder
                .getDirectoriesBuilder()
                .setClasses(directory(DirectoryType.CLASSES).toAbsolutePath().toString())
                .setGeneratedSources(directory(DirectoryType.SOURCE_GEN).toAbsolutePath().toString())
                .setGeneratedJavaSources(directory(DirectoryType.JAVA_SOURCE_GEN).toAbsolutePath().toString())
                .setGeneratedStubClasses(directory(DirectoryType.GENERATED_STUBS).toAbsolutePath().toString())
                .setTemp(directory(DirectoryType.TEMP).toAbsolutePath().toString())
                .setGeneratedClasses(
                        directory(DirectoryType.GENERATED_CLASSES).toAbsolutePath().toString());
    }

    @Override
    public JvmCompilationTask buildTask() {
        return taskBuilder.build();
    }

    @SafeVarargs
    public final Dep runCompileTask(Consumer<TaskBuilder>... setup) {
        return executeTask(component().jvmTaskExecutor()::execute, setup);
    }

    public static KotlinBuilderTestComponent component() {
        return DaggerKotlinBuilderTestComponent.builder()
                .toolchain(KotlinToolchain.createToolchain())
                .build();
    }

    private Dep executeTask(
            BiConsumer<CompilationTaskContext, JvmCompilationTask> executor,
            Consumer<TaskBuilder>[] setup) {
        resetForNext();
        Stream.of(setup).forEach(it -> it.accept(taskBuilderInstance));
        return runCompileTask(
                (taskContext, task) -> {
                    executor.accept(taskContext, task);

                    JvmCompilationTask.Outputs outputs = task.getOutputs();
                    assertFilesExist(
                            Stream.of(
                                    outputs.getAbijar(),
                                    outputs.getJar(),
                                    outputs.getJdeps(),
                                    outputs.getSrcjar())
                                    .filter(p -> !p.isEmpty())
                                    .toArray(String[]::new)
                    );

                    return Dep.builder()
                            .label(taskBuilder.getInfo().getLabel())
                            .compileJars(ImmutableList.of(
                                    outputs.getAbijar().isEmpty() ? outputs.getJar() : outputs.getAbijar()
                            ))
                            .runtimeDeps(ImmutableList.copyOf(taskBuilder.getInputs().getClasspathList()))
                            .sourceJar(taskBuilder.getOutputs().getSrcjar())
                            .build();
                });
    }

    public class TaskBuilder {
        TaskBuilder() {
        }

        public void setLabel(String label) {
            taskBuilder.getInfoBuilder().setLabel(label);
        }

        public void addSource(String filename, String... lines) {
            String pathAsString = writeSourceFile(filename, lines).toString();
            if (pathAsString.endsWith(".kt")) {
                taskBuilder.getInputsBuilder().addKotlinSources(pathAsString);
            } else if (pathAsString.endsWith(".java")) {
                taskBuilder.getInputsBuilder().addJavaSources(pathAsString);
            } else {
                throw new RuntimeException("unhandled file type: " + pathAsString);
            }
        }

        public TaskBuilder compileJava() {
            taskBuilder.setCompileJava(true);
            return this;
        }

        public TaskBuilder compileKotlin() {
            taskBuilder.setCompileKotlin(true);
            return this;
        }

        public void addAnnotationProcessors(AnnotationProcessor... annotationProcessors) {
            Preconditions.checkState(
                    taskBuilder.getInputs().getProcessorsList().isEmpty(), "processors already set");
            HashSet<String> processorClasses = new HashSet<>();
            taskBuilder
                    .getInputsBuilder()
                    .addAllProcessorpaths(
                            Stream.of(annotationProcessors)
                                    .peek(it -> processorClasses.add(it.processClass()))
                                    .flatMap(it -> it.processorPath().stream())
                                    .distinct()
                                    .collect(Collectors.toList()))
                    .addAllProcessors(processorClasses);
        }

        public void addDirectDependencies(Dep... dependencies) {
            Dep.classpathOf(dependencies)
                    .forEach((dependency) -> taskBuilder.getInputsBuilder().addClasspath(dependency));
        }

        public TaskBuilder outputSrcJar() {
            taskBuilder.getOutputsBuilder()
                    .setSrcjar(instanceRoot().resolve("jar_file-sources.jar").toAbsolutePath().toString());
            return this;
        }

        public TaskBuilder outputJar() {
            taskBuilder.getOutputsBuilder()
                    .setJar(instanceRoot().resolve("jar_file.jar").toAbsolutePath().toString());
            return this;
        }

        public TaskBuilder outputJdeps() {
            taskBuilder.getOutputsBuilder()
                    .setJdeps(instanceRoot().resolve("jdeps_file.jdeps").toAbsolutePath().toString());
            return this;
        }

        public TaskBuilder outputAbiJar() {
            taskBuilder.getOutputsBuilder()
                    .setAbijar(instanceRoot().resolve("abi.jar").toAbsolutePath().toString());
            return this;
        }

        public TaskBuilder generatedSourceJar() {
            taskBuilder.getOutputsBuilder()
                .setGeneratedJavaSrcJar(instanceRoot().resolve("gen-src.jar").toAbsolutePath().toString());
            return this;
        }

        public TaskBuilder ktStubsJar() {
            taskBuilder.getOutputsBuilder()
                .setGeneratedJavaStubJar(instanceRoot().resolve("kt-stubs.jar").toAbsolutePath().toString());
            return this;
        }

        public TaskBuilder incrementalData() {
            taskBuilder.getOutputsBuilder()
                .setGeneratedClassJar(instanceRoot().resolve("incremental.jar").toAbsolutePath().toString());
            return this;
        }
    }
}
