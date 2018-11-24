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
package io.bazel.kotlin

import org.junit.Test


class KotlinNormalizationAssertionTest : KotlinAssertionTestCase("src/test/data/jvm/basic") {
    /*
     * (hsyed) This test is running locally because things hash differently on the ci servers. Don't have the time to
     * look into it.
     *
     * The hashes can change between kotlin compiler versions so this approach isn't sustainable.
     */
    @Test
    fun testJarNormalization() {
        jarTestCase(
            name = "test_module_name_lib.jar",
            description = "Builder jars should be normalized with and include stamp data"
        ) {
            validateFileSha256("a6eca5f9db22d5fb2914efa821cb553c213cdc05df5d0b7cbe1e58e6d308b513")
            assertManifestStamped()
            assertEntryCompressedAndNormalizedTimestampYear("helloworld/Main.class")
        }
        jarTestCase(
            name = "test_embed_resources.jar",
            description = "Merging resources into the main output jar should still result in a normalized jar"
        ) {
            validateFileSha256("e3fff23417b6624a5a6445e367456f8fea6a47ed4a529626b8723a36475ba58a")
            assertManifestStamped()
            assertEntryCompressedAndNormalizedTimestampYear("testresources/AClass.class")
            assertEntryCompressedAndNormalizedTimestampYear(
                "src/test/data/jvm/basic/testresources/resources/one/two/aFile.txt"
            )
        }
    }
}
