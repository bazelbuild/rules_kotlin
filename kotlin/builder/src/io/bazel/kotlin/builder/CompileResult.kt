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
package io.bazel.kotlin.builder

import java.util.*

interface CompileResult {
    /**
     * The status of this operation.
     */
    fun status(): Int {
        return 0
    }

    fun error(): Optional<Exception> {
        return Optional.empty()
    }

    @Throws(RuntimeException::class)
    fun propogateError(message: String) {
        error().ifPresent { e -> throw RuntimeException(message, e) }
    }

    class Meta(id: String) : io.bazel.kotlin.builder.Meta<CompileResult> {
        override val id: String = id

        fun run(ctx: Context, op: (Context) -> Int): CompileResult {
            var result: CompileResult
            try {
                result = CompileResult.just(op(ctx))
            } catch (e: Exception) {
                result = CompileResult.error(e)
            }

            return result
        }

        fun runAndBind(ctx: Context, op: (Context) -> Int): CompileResult {
            val res = run(ctx, op)
            set(ctx,res)
            return res
        }

        //        public CompileResult runAndBind(final Context ctx, Supplier<Integer> op) {
        //            return runAndBind(ctx, (c) -> op.get());
        //        }
    }

    /**
     * Materialise the output of the compile result.
     *
     * @return the new status of the compile operation, this shouldn't make a failing status pass, but it could fail a compile operation.
     */
    fun render(ctx: Context): Int

    companion object {

        fun just(status: Int): CompileResult {
            return object : CompileResult {
                override fun status(): Int {
                    return status
                }

                override fun render(ctx: Context): Int {
                    return status
                }
            }
        }

        fun error(error: Exception): CompileResult {
            return object : CompileResult {
                override fun status(): Int {
                    return -1
                }

                override fun error(): Optional<Exception> {
                    return Optional.of(error)
                }

                override fun render(ctx: Context): Int {
                    throw RuntimeException(error)
                }
            }
        }

        fun deferred(status: Int, renderer: (Context) -> Int): CompileResult {
            return object : CompileResult {
                override fun status(): Int {
                    return status
                }

                override fun render(ctx: Context): Int {
                    return renderer(ctx)
                }
            }
        }
    }
}
