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
package io.bazel.ruleskotlin.workers;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface CompileResult {
    /**
     * The status of this operation.
     */
    default int status() {
        return 0;
    }

    default Optional<Exception> error() {
        return Optional.empty();
    }

    default void propogateError(String message) throws RuntimeException {
        error().ifPresent(e -> {
            throw new RuntimeException(message, e);
        });
    }

    static CompileResult just(final int status) {
        return new CompileResult() {
            @Override
            public int status() {
                return status;
            }

            @Override
            public Integer render(Context ctx) {
                return status;
            }
        };
    }

    static CompileResult error(final Exception error) {
        return new CompileResult() {
            @Override
            public int status() {
                return -1;
            }

            @Override
            public Optional<Exception> error() {
                return Optional.of(error);
            }

            @Override
            public Integer render(Context ctx) {
                throw new RuntimeException(error);
            }
        };
    }

    static CompileResult deferred(final int status, Function<Context, Integer> renderer) {
        return new CompileResult() {
            @Override
            public int status() {
                return status;
            }

            @Override
            public Integer render(Context ctx) {
                return renderer.apply(ctx);
            }
        };
    }

    final class Meta extends io.bazel.ruleskotlin.workers.Meta<CompileResult> {
        public Meta(String id) {
            super(id);
        }

        public CompileResult run(final Context ctx, Function<Context, Integer> op) {
            CompileResult result;
            try {
                result = CompileResult.just(op.apply(ctx));
            } catch (Exception e) {
                result = CompileResult.error(e);
            }
            return result;
        }

        public CompileResult runAndBind(final Context ctx, Function<Context, Integer> op) {
            CompileResult res = run(ctx, op);
            bind(ctx, res);
            return res;
        }

        public CompileResult runAndBind(final Context ctx, Supplier<Integer> op) {
            return runAndBind(ctx, (c) -> op.get());
        }
    }

    /**
     * Materialise the output of the compile result.
     *
     * @return the new status of the compile operation, this shouldn't make a failing status pass, but it could fail a compile operation.
     */
    Integer render(Context ctx);
}
