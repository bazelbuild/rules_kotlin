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
package io.bazel.kotlin.builder;

import io.bazel.kotlin.builder.toolchain.KotlinToolchain;

import javax.inject.Provider;
import java.io.PrintStream;
import java.util.Arrays;

public class KotlinBuilderMain {
  public static void main(String[] args) {
    KotlinBuilderComponent component =
        DaggerKotlinBuilderComponent.builder()
            .toolchain(KotlinToolchain.createToolchain())
            .build();
    System.exit(component.worker().apply(Arrays.asList(args)));
  }
}
