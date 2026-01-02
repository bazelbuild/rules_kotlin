/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
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
 */

package jvm_compat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test class for verifying the compat implementation of kt_jvm_test.
 * This test is built using kt_jvm_library + java_test composition.
 */
class JvmCompatTest {
    @Test
    fun testBasicAssertion() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testKotlinFeatures() {
        val list = listOf(1, 2, 3)
        assertTrue(list.contains(2))
    }

    @Test
    fun testStringInterpolation() {
        val name = "Kotlin"
        assertEquals("Hello, Kotlin!", "Hello, $name!")
    }
}
