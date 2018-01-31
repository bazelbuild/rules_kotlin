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


import io.bazel.ruleskotlin.workers.BazelWorker;
import io.bazel.ruleskotlin.workers.CommandLineProgram;
import io.bazel.ruleskotlin.workers.compilers.jvm.actions.BuildAction;
import io.bazel.ruleskotlin.workers.compilers.jvm.actions.GenerateJdepsFile;
import io.bazel.ruleskotlin.workers.compilers.jvm.actions.KotlinCreateClassJar;
import io.bazel.ruleskotlin.workers.compilers.jvm.actions.KotlinMainCompile;

import java.util.List;

/**
 * Bazel Kotlin Compiler worker.
 */
public final class KotlinJvmBuilder implements CommandLineProgram {
    private static final BuildAction[] compileActions = new BuildAction[] {
            KotlinMainCompile.INSTANCE,
            KotlinCreateClassJar.INSTANCE,
            GenerateJdepsFile.INSTANCE,
    };

    @Override
    public Integer apply(List<String> args) {
        Context context = Context.from(args);
        Integer exitCode = 0;
        for (BuildAction action : compileActions) {
            exitCode = action.apply(context);
            if(exitCode != 0)
                break;
        }
        return exitCode;
    }

    public static void main(String[] args) {
        KotlinJvmBuilder kotlinBuilder = new KotlinJvmBuilder();
        BazelWorker<KotlinJvmBuilder> kotlinCompilerBazelWorker = new BazelWorker<>(
                kotlinBuilder,
                System.err,
                "KotlinCompile"
        );
        System.exit(kotlinCompilerBazelWorker.apply(args));
    }
}
