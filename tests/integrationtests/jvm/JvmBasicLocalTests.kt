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
     */
    @Test
    fun testJarNormalization() {
        jarTestCase(
            name = "test_module_name_lib.jar",
            description = "Builder jars should be normalized with the same timestamps as singlejar and including stamp data"
        ) {
            validateFileSha256("513d14b29eb1b95b97bf7d34e2126a716c7d1012b259b5021c16b99ca82feeb5")
            assertManifestStamped()
            assertEntryCompressedAndNormalizedTimestampYear("helloworld/Main.class")
        }
        jarTestCase(
            name = "test_embed_resources.jar",
            description = "Merging resources into the main output jar should still result in a normalized jar"
        ) {
            validateFileSha256("2d9175e9ecc6b9bc62f59ce861e9b67c6f64dd581f6cbd986c0a694b89e310b1")
            assertManifestStamped()
            assertEntryCompressedAndNormalizedTimestampYear("testresources/AClass.class")
            assertEntryCompressedAndNormalizedTimestampYear("tests/integrationtests/jvm/basic/testresources/resources/one/two/aFile.txt")
        }
    }
}