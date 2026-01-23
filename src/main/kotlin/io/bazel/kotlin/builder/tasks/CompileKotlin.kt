/*
 * Copyright 2021 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.bazel.kotlin.builder.tasks

import io.bazel.worker.Status
import io.bazel.worker.Work
import io.bazel.worker.WorkerContext

class CompileKotlin(
  private val builder: KotlinBuilder,
) : Work {
  override fun invoke(
    ctx: WorkerContext.TaskContext,
    args: Iterable<String>,
  ): Status =
    if (builder.build(ctx, args.toList()) != 0) {
      Status.ERROR
    } else {
      Status.SUCCESS
    }
}
