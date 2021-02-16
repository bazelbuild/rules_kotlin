/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package io.bazel.kotlin.builder;

import dagger.Component;
import dagger.Provides;
import io.bazel.kotlin.builder.tasks.MergeJdeps;
import io.bazel.kotlin.builder.tasks.jvm.JdepsMerger;

import javax.inject.Singleton;
import java.io.PrintStream;

@Singleton
@dagger.Component(modules = {JdepsMergerComponent.Module.class})
public interface JdepsMergerComponent {

    MergeJdeps work();

    @Component.Builder
    interface Builder {
        JdepsMergerComponent build();
    }

    @dagger.Module
    abstract class Module {

        @Provides
        public static PrintStream provideDebugPrintStream() {
            return System.err;
        }

        @Provides
        public static JdepsMerger provideJdepsMerger() {
            return new JdepsMerger();
        }

    }
}
