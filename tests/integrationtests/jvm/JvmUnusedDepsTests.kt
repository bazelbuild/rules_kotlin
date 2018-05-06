package io.bazel.ruleskotlin.integrationtests.jvm

import io.bazel.kotlin.builder.model.Flags
import io.bazel.ruleskotlin.integrationtests.lib.AssertionTestCase
import org.junit.Test
import kotlin.test.assertTrue

class JvmUnusedDepsTests : AssertionTestCase("tests/integrationtests/jvm/unused_deps") {
    private companion object {
        const val B_JAR = "bazel-out/darwin-fastbuild/bin/tests/integrationtests/jvm/unused_deps/b.jar"
        const val B_DEPS_LABEL = "//tests/integrationtests/jvm/unused_deps:b"
        const val C_JAR = "bazel-out/darwin-fastbuild/bin/tests/integrationtests/jvm/unused_deps/c.jar"
        const val C_JAR_LABEL = "//tests/integrationtests/jvm/unused_deps:c"
    }

    @Test
    fun testUseAndLink() {
        argMapTestCase("a-use-b-link-b.jar-2.params", "test params file") {
            val flags = Flags(this)

            flags.directDependencies
        }
    }
}