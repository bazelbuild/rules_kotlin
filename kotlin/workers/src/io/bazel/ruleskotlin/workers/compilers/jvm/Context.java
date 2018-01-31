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

import java.util.*;
import java.util.stream.Collectors;

public class Context {
    private final EnumMap<Flag, String> args = new EnumMap<>(Flag.class);
    private final Map<Meta<?>, Object> meta = new HashMap<>();

    private static final Map<String, Flag> ALL_FIELDS_MAP = Arrays.stream(Flag.values()).collect(Collectors.toMap(x -> x.name, x -> x));
    private static final Flag[] MANDATORY_FIELDS = Arrays.stream(Flag.values()).filter(x -> x.mandatory).toArray(Flag[]::new);

    private Context(List<String> args) {
        if (args.size() % 2 != 0) {
            throw new RuntimeException("args should be k,v pairs");
        }

        for (int i = 0; i < args.size() / 2; i++) {
            String flag = args.get(i * 2);
            String value = args.get((i * 2) + 1);
            Flag field = ALL_FIELDS_MAP.get(flag);
            if (field == null) {
                throw new RuntimeException("unrecognised arg: " + flag);
            }
            this.args.put(field, value);
        }

        for (Flag mandatoryField : MANDATORY_FIELDS) {
            if (!this.args.containsKey(mandatoryField)) {
                throw new RuntimeException("mandatory arg missing: " + mandatoryField.name);
            }
        }
    }

    static Context from(List<String> args) {
        return new Context(args);
    }

    public EnumMap<Flag, String> copyOfArgsContaining(Flag... fields) {
        EnumMap<Flag, String> result = new EnumMap<>(Flag.class);
        for (Flag field : fields) {
            String value = args.get(field);
            if (value != null) {
                result.put(field, value);
            }
        }
        return result;
    }

    String get(Flag field) {
        return args.get(field);
    }

    @SuppressWarnings("unchecked")
    <T> T get(Meta<T> key) {
        return (T) meta.get(key);
    }
    @SuppressWarnings("unchecked")
    <T> T putIfAbsent(Meta<T> key, T value) {
        return (T) meta.putIfAbsent(key, value);
    }
}
