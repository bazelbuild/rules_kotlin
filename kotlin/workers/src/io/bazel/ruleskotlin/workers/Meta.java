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

public class Meta<T> {
    private final String id;

    private final T defaultValue;

    public Meta(String id) {
        this.id = id;
        this.defaultValue = null;
    }

    @SuppressWarnings("unused")
    private Meta(String id, T defaultValue) {
        this.id = id;
        this.defaultValue = defaultValue;
    }

    /**
     * Gets a mandatory value.
     */
    public T mustGet(Context ctx) {
        T res = ctx.get(this);
        if(res == null) {
            assert defaultValue != null : "mandatory meta parameter missing in context and does not have a default value";
            return defaultValue;
        }
        return res;
    }

    /**
     * Gets an optional value, if it has not been bound the default value is used.
     */
    public Optional<T> get(Context ctx) {
        T res = ctx.get(this);
        if( res != null) {
            return Optional.of(res);
        } else if(defaultValue != null) {
            return Optional.of(defaultValue);
        } else {
            return Optional.empty();
        }
    }

    public void bind(Context ctx, T value) {
        if (ctx.putIfAbsent(this, value) != null) {
            throw new RuntimeException("attempting to change bound meta variable " + id);
        }
    }
}
