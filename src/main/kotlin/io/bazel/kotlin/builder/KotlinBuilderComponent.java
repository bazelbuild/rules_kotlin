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

import dagger.BindsInstance;
import dagger.Component;
import dagger.Provides;
import io.bazel.kotlin.builder.tasks.CompileKotlin;
import io.bazel.kotlin.builder.tasks.js.Kotlin2JsTaskExecutor;
import io.bazel.kotlin.builder.tasks.jvm.InternalCompilerPlugins;
import io.bazel.kotlin.builder.tasks.jvm.KotlinJvmTaskExecutor;
import io.bazel.kotlin.builder.toolchain.KotlinToolchain;

import javax.inject.Singleton;

@Singleton
@dagger.Component(modules = {KotlinBuilderComponent.Module.class})
public interface KotlinBuilderComponent {
    KotlinToolchain toolchain();

    KotlinJvmTaskExecutor jvmTaskExecutor();

    Kotlin2JsTaskExecutor jsTaskExecutor();

    CompileKotlin work();

    @Component.Builder
    interface Builder {
        @BindsInstance
        KotlinBuilderComponent.Builder toolchain(KotlinToolchain toolchain);

        KotlinBuilderComponent build();
    }

    @dagger.Module
    class Module {
        @Provides
        public InternalCompilerPlugins provideInternalPlugins(KotlinToolchain toolchain) {
            return new InternalCompilerPlugins(
                    toolchain.getJvmAbiGen(), toolchain.getSkipCodeGen(), toolchain.getKapt3Plugin(), toolchain.getJdepsGen());
        }
    }
}
