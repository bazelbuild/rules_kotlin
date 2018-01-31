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

import java.io.File;
import java.util.Optional;

/**
 * Meta is a key to some compilation state, it is stored in a {@link Context}. A meta is meant for setting up state for other actions.
 */
public final class Meta<T> {
    // if present contains the directory that classes were compiled to.
    public static final Meta<File> COMPILE_TO_DIRECTORY = new Meta<>("compile_to_jar");

    private final String id;

    private Meta(String id) {
        this.id = id;
    }

    public Optional<T> get(Context ctx) {
        return Optional.ofNullable(ctx.get(this));
    }

    public void bind(Context ctx, T value) {
        if (ctx.putIfAbsent(this, value) != null) {
            throw new RuntimeException("attempting to change bound meta variable " + id);
        }
    }
}