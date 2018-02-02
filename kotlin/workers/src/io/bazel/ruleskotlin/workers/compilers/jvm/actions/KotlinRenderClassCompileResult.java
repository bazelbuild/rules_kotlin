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
import io.bazel.ruleskotlin.workers.CompileResult;
import io.bazel.ruleskotlin.workers.Context;
import io.bazel.ruleskotlin.workers.compilers.jvm.Metas;

import java.util.Optional;


/**
 * Render the result of class compilation. This is a separate step at the moment for mixed mode compilation scenarios. If there is an error in Java sources in
 * a large mixed mode package the Kotlin errors don't make any sense and overwhelm the console and intellij. The {@link KotlinMainCompile} step binds a deferred
 * renderer and proceeds to lets javac compile the java sources. The step below merges the result of the two actions.
 */
public final class KotlinRenderClassCompileResult implements BuildAction {
    public static final KotlinRenderClassCompileResult INSTANCE = new KotlinRenderClassCompileResult();

    private KotlinRenderClassCompileResult() {
    }

    @Override
    public Integer apply(Context ctx) {
        CompileResult kotlincResult = Metas.KOTLINC_RESULT.mustGet(ctx);
        Optional<CompileResult> javacResult = Metas.JAVAC_RESULT.get(ctx);
        if (!javacResult.isPresent()) {
            return kotlincResult.render(ctx);
        } else {
            try {
                javacResult.get().propogateError("javac failed");
                if (kotlincResult.status() != 0) {
                    return kotlincResult.status();
                } else if (javacResult.get().status() != 0) {
                    // treat all javac statuses as non terminal compile errors.
                    return 1;
                }
                return 0;
            } finally {
                kotlincResult.render(ctx);
            }
        }
    }
}
