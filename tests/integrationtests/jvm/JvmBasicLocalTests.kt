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
package io.bazel.kotlin.testing.jvm

import io.bazel.kotlin.testing.AssertionTestCase
import org.junit.Test


class JvmBasicLocalTests : AssertionTestCase("tests/integrationtests/jvm/basic") {
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
            validateFileSha256("f26827dd09160adb436b31a12aa3b86f6da8ae1c317150c264ed8d1427a68dea")
            assertManifestStamped()
            assertEntryCompressedAndNormalizedTimestampYear("helloworld/Main.class")
        }
        jarTestCase(
            name = "test_embed_resources.jar",
            description = "Merging resources into the main output jar should still result in a normalized jar"
        ) {
            validateFileSha256("270ea8758f38d9ef15013aea222cc8f609affaf9acc2f2cd206dbd73a4872082")
            assertManifestStamped()
            assertEntryCompressedAndNormalizedTimestampYear("testresources/AClass.class")
            assertEntryCompressedAndNormalizedTimestampYear(
                "tests/integrationtests/jvm/basic/testresources/resources/one/two/aFile.txt"
            )
        }
    }
}