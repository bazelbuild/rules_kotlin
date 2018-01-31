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
package io.bazel.ruleskotlin.workers.compilers.jvm.actions;

import io.bazel.ruleskotlin.workers.compilers.jvm.Context;
import io.bazel.ruleskotlin.workers.compilers.jvm.Flag;
import io.bazel.ruleskotlin.workers.compilers.jvm.Locations;
import io.bazel.ruleskotlin.workers.compilers.jvm.Meta;
import io.bazel.ruleskotlin.workers.compilers.jvm.utils.Utils;

/**
 * If classes for the main artifact were compiled to an intermediate temp directory turn them into a jar and clean up.
 */
public final class KotlinCreateClassJar implements BuildAction {
    public static final KotlinCreateClassJar INSTANCE = new KotlinCreateClassJar();
    private static final String JAR_TOOL_PATH = Locations.JAVA_HOME.resolveVerified("bin", "jar").toString();

    private KotlinCreateClassJar() {}

    @Override
    public Integer apply(Context ctx) {
        Meta.COMPILE_TO_DIRECTORY.get(ctx).ifPresent((classDirectory) -> {
            try {
                String classJarPath = Flag.OUTPUT_CLASSJAR.get(ctx);
                Utils.waitForSuccess(new String[]{JAR_TOOL_PATH, "cf", classJarPath, "-C", classDirectory.toString(), "."}, System.err);
                Utils.deleteDirectory(classDirectory.toPath());
            } catch (Exception e) {
                throw new RuntimeException("unable to create class jar", e);
            }
        });
        return 0;
    }
}
