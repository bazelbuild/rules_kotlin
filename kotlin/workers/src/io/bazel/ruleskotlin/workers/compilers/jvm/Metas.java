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
package io.bazel.ruleskotlin.workers.compilers.jvm;

import io.bazel.ruleskotlin.workers.CompileResult;
import io.bazel.ruleskotlin.workers.Meta;

import java.nio.file.Path;
import java.util.List;

/**
 * Meta is a key to some compilation state,.
 */
public class Metas {
    // mandatory: the package part of the label.
    public static final Meta<String> PKG = new Meta<>("package");
    // mandatory: The target part of the label.
    public static final Meta<String> TARGET = new Meta<>("target");
    // mandatory: the class staging directory.
    public static final Meta<Path> CLASSES_DIRECTORY = new Meta<>("class_directory");
    // mandatory: If this is non empty then it is a mixed mode operation.
    public static final Meta<List<String>> JAVA_SOURCES = new Meta<>("java_sources");
    // mandatory:
    public static final Meta<List<String>> ALL_SOURCES = new Meta<>("all_sources");
    // mandatory:
    public static final CompileResult.Meta KOTLINC_RESULT = new CompileResult.Meta("kotlin_compile_result");
    // optional: when not a mixed mode operation.
    public static final CompileResult.Meta JAVAC_RESULT = new CompileResult.Meta("javac_compile_result");
}