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
package io.bazel.kotlin.builder.model

import io.bazel.kotlin.builder.MandatoryMeta
import io.bazel.kotlin.builder.Meta

/**
 * Listin of Meta keys that don't make sense as companion objects.
 */
object Metas {
    // the package part of the label.
    val PKG = MandatoryMeta<String>("package")
    // The target part of the label.
    val TARGET = MandatoryMeta<String>("target")

    // If this is non empty then it is a mixed mode operation.
    val JAVA_SOURCES = MandatoryMeta<List<String>>("java_sources")

    // .kt and .java source files unpacked from .srcjar files
    val UNPACKED_SOURCES = Meta<List<String>>("unpacked_sources")

    val ALL_SOURCES = MandatoryMeta<List<String>>("all_sources")

    // memoized classpath string
    val CLASSPATH_STRING = MandatoryMeta<String>("joined_classpath")
}