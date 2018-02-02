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
import io.bazel.ruleskotlin.workers.Meta;
import io.bazel.ruleskotlin.workers.compilers.jvm.Metas;
import io.bazel.ruleskotlin.workers.utils.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Should be the first step, does mandatory pre-processing.
 */
public final class Initialize implements BuildAction {
    public static final Initialize INSTANCE = new Initialize();

    private Initialize() {
    }

    @Override
    public Integer apply(Context ctx) {
        ctx.apply(
                Initialize::initializeAndBindBindDirectories,
                Initialize::bindLabelComponents,
                Initialize::bindSources
        );
        return 0;
    }

    private static void bindSources(Context ctx) {
        List<String> javaSources = new ArrayList<>();
        List<String> allSources = new ArrayList<>();
        for (String src : Flags.SOURCES.get(ctx).split(":")) {
            if (src.endsWith(".java")) {
                javaSources.add(src);
                allSources.add(src);
            } else if (src.endsWith(".kt")) {
                allSources.add(src);
            } else {
                throw new RuntimeException("unrecognised file type: " + src);
            }
        }
        Metas.JAVA_SOURCES.bind(ctx, Collections.unmodifiableList(javaSources));
        Metas.ALL_SOURCES.bind(ctx, Collections.unmodifiableList(allSources));
    }

    private static void initializeAndBindBindDirectories(Context ctx) {
        Path outputBase;

        try {
            outputBase = Files.createDirectories(Paths.get(Flags.COMPILER_OUTPUT_BASE.get(ctx)));
        } catch (IOException e) {
            throw new RuntimeException("could not create compiler output base", e);
        }

        try {
            IOUtils.purgeDirectory(outputBase);
        } catch (IOException e) {
            throw new RuntimeException("could not purge output directory", e);
        }

        createAndBindComponentDirectory(ctx, outputBase, Metas.CLASSES_DIRECTORY, "_classes");
    }

    private static void createAndBindComponentDirectory(Context ctx, Path outputBase, Meta<Path> key, String component) {
        try {
            key.bind(ctx, Files.createDirectories(outputBase.resolve(component)));
        } catch (IOException e) {
            throw new RuntimeException("could not create subdirectory for component " + component, e);
        }
    }

    /**
     * parses the label, sets up the meta elements and returns the target part.
     */
    private static void bindLabelComponents(Context ctx) {
        String label = Flags.LABEL.get(ctx);
        String[] parts = label.split(":");
        if (parts.length != 2) {
            throw new RuntimeException("the label " + label + " is invalid");
        }
        Metas.PKG.bind(ctx, parts[0]);
        Metas.TARGET.bind(ctx, parts[1]);
    }
}
