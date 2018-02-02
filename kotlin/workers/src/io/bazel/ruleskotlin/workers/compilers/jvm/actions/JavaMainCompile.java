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

import io.bazel.ruleskotlin.workers.BuildAction;
import io.bazel.ruleskotlin.workers.Context;
import io.bazel.ruleskotlin.workers.Flags;
import io.bazel.ruleskotlin.workers.compilers.jvm.*;
import io.bazel.ruleskotlin.workers.utils.IOUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple java compile action that invokes javac directly and simply.
 */
public final class JavaMainCompile implements BuildAction {
    private static final String JAVAC_PATH = Locations.JAVA_HOME.resolveVerified("bin", "javac").toString();

    public JavaMainCompile() {}

    @Override
    public Integer apply(Context ctx) {
        List<String> javaSources = Metas.JAVA_SOURCES.mustGet(ctx);
        if (!javaSources.isEmpty()) {
            List<String> args = new ArrayList<>();
            String classesDirectory = Metas.CLASSES_DIRECTORY.mustGet(ctx).toString();
            Collections.addAll(args,
                    JAVAC_PATH, "-cp", classesDirectory + "/:" + Flags.CLASSPATH.get(ctx),
                    "-d", classesDirectory
            );
            args.addAll(javaSources);
            Metas.JAVAC_RESULT.runAndBind(ctx, () -> IOUtils.executeAndAwait(30, args));
        }
        return 0;
    }
}

