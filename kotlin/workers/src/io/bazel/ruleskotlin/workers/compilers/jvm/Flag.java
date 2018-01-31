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

public enum Flag {
    LABEL("--label", null, true),
    OUTPUT_CLASSJAR("--output_classjar", "-d", true),
    OUTPUT_JDEPS("--output_jdeps", null, true),
    CLASSPATH("--classpath", "-cp", true),
    SOURCES("--sources", null, true),
    KOTLIN_API_VERSION("--kotlin_api_version", "-api-version", false),
    KOTLIN_LANGUAGE_VERSION("--kotlin_language_version", "-language-version", false),
    KOTLIN_JVM_TARGET("--kotlin_jvm_target", "-jvm-target", false);

    public final String name;
    public final String kotlinFlag;
    final boolean mandatory;

    Flag(String name, String kotlinName, boolean mandatory) {
        this.name = name;
        this.kotlinFlag = kotlinName;
        this.mandatory = mandatory;
    }

    public String get(Context context) {
        return context.get(this);
    }
}