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
package io.bazel.ruleskotlin.workers


/**
 * Interface for command line programs.
 *
 * This is the same thing as a main function, except not static.
 */
interface CommandLineProgram {

    /**
     * Runs blocking program start to finish.
     *
     *
     *
     * This function might be called multiple times throughout the life of this object. Output
     * must be sent to [System.out] and [System.err].
     *
     * @param args command line arguments
     * @return program exit code, i.e. 0 for success, non-zero for failure
     */
    fun apply(args: List<String>): Int
}
