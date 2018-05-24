package io.bazel.kotlin.testing.jvm

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.bazel.kotlin.model.KotlinModel
import io.bazel.kotlin.testing.AssertionTestCase
import org.junit.Test

class JvmUnusedDepsTests : AssertionTestCase("tests/integrationtests/jvm/unused_deps") {
    private companion object {
        const val B_JAR = "bazel-out/darwin-fastbuild/bin/tests/integrationtests/jvm/unused_deps/b.jar"
        const val B_JAR_LABEL = "//tests/integrationtests/jvm/unused_deps:b"
        const val C_JAR = "bazel-out/darwin-fastbuild/bin/tests/integrationtests/jvm/unused_deps/c.jar"
        const val C_JAR_LABEL = "//tests/integrationtests/jvm/unused_deps:c"
        const val RT_JAR = "bazel-out/darwin-fastbuild/bin/tests/integrationtests/jvm/unused_deps/rt-dep.jar"
        const val RT_JAR_LABEL = "//tests/integrationtests/jvm/unused_deps:rt-dep"
        const val RT_JAR_2 = "bazel-out/darwin-fastbuild/bin/tests/integrationtests/jvm/unused_deps/rt-dep-2.jar"
        const val RT_JAR_LABEL_2 = "//tests/integrationtests/jvm/unused_deps:rt-dep"

        val kotlinImplicits = listOf(
            "external/com_github_jetbrains_kotlin/lib/kotlin-stdlib.jar",
            "external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk7.jar",
            "external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk8.jar",
            "external/com_github_jetbrains_kotlin/lib/kotlin-runtime.jar"
        )
    }

    private fun KotlinModel.BuilderCommand.assertDepAnalysis(
        directDeps: Map<String, String>,
        indirect_deps: Map<String, String>
    ) {
        assertWithMessage("implicits should not be in indirect deps")
            .that(inputs.indirectDependenciesMap.keys).containsNoneIn(kotlinImplicits)
        assertWithMessage("implicits should not be in direct deps")
            .that(inputs.directDependenciesMap.keys).containsNoneIn(kotlinImplicits)
        assertWithMessage("direct dependencies should be correct")
            .that(inputs.directDependenciesMap).containsExactlyEntriesIn(directDeps)
        assertWithMessage("indirect dependencies should be correct")
            .that(inputs.indirectDependenciesMap).containsExactlyEntriesIn(indirect_deps)
        assertWithMessage("the classpath should be the transitive closure of the direct, indirect and implicit jars")
            .that(inputs.classpathList).containsExactlyElementsIn(directDeps.keys + indirect_deps.keys + kotlinImplicits)
    }

    @Test
    fun verifyRandomRegressions() {
        argMapTestCase("a-use-b-link-b.jar-2.params", "test params file") {
            commandBuilder.fromInput(this, mapOf("kotlin_jvm_strict_deps" to "WARN")).also {
                assertWithMessage("validate duplicate source regression that duplicate sources are not somehow getting in")
                    .that(mutableListOf(it.inputs.kotlinSourcesList) + it.inputs.javaSourcesList).containsNoDuplicates()
                assertThat(it.inputs.classpathList).containsNoDuplicates()
            }
        }
    }

    @Test
    fun testUseAndLink() {
        // a has rt-dep on rt-dep which has an rt-dep on rt-dep2
        // b has: dep c
        argMapTestCase("a-use-b-link-b.jar-2.params", "test params file") {
            commandBuilder.fromInput(this, mapOf("kotlin_jvm_strict_deps" to "WARN")).also {
                it.assertDepAnalysis(
                    directDeps = mapOf(
                        B_JAR to B_JAR_LABEL
                    ),
                    indirect_deps = mapOf(
                        // TODO fix labelling bug
                        C_JAR to B_JAR_LABEL,
                        RT_JAR to RT_JAR_LABEL,
                        // TODO fix labelling bug
                        RT_JAR_2 to RT_JAR_LABEL
                    )
                )
                assertWithMessage("unused deps support should consider runtime dependencies as an indirect dependency")
                    .that(it.inputs.indirectDependenciesMap).containsEntry(RT_JAR, RT_JAR_LABEL)
            }
        }
    }
}

