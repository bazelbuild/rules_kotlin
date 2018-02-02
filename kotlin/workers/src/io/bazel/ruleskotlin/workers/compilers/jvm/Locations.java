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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Locations {
        KOTLIN_REPO(Paths.get("external", "com_github_jetbrains_kotlin")),
        JAVA_HOME(Paths.get("external", "local_jdk"));

        private final Path path;

        Locations(Path path) {
            this.path = path;
        }

        public final File resolveVerified(String... parts) {
            return verified(path.resolve(Paths.get(parts[0], Arrays.copyOfRange(parts, 1, parts.length))));
        }

        /**
         * Return a stream of paths that are known to exists relative to this location.
         */
        public final List<File> verifiedRelativeFiles(Path... paths) {
            return Stream.of(paths).map(relative -> verified(path.resolve(relative))).collect(Collectors.toList());
        }

        private File verified(Path target) {
            File asFile = target.toFile();
            if (!asFile.exists()) {
                throw new RuntimeException("location " + this.name() + " did not have relative path file " + target);
            }
            return asFile;
        }
    }