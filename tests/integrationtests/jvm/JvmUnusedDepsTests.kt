package io.bazel.kotlin.testing.jvm

import com.google.common.truth.FailureMetadata
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.bazel.kotlin.builder.model.Flags
import io.bazel.kotlin.testing.AssertionTestCase
import org.junit.Test

class JvmUnusedDepsTests : AssertionTestCase("tests/integrationtests/jvm/unused_deps") {
    private companion object {
        const val B_JAR = "bazel-out/darwin-fastbuild/bin/tests/integrationtests/jvm/unused_deps/b.jar"
        const val B_DEPS_LABEL = "//tests/integrationtests/jvm/unused_deps:b"
        const val C_JAR = "bazel-out/darwin-fastbuild/bin/tests/integrationtests/jvm/unused_deps/c.jar"
        const val C_JAR_LABEL = "//tests/integrationtests/jvm/unused_deps:c"
        const val RT_JAR = "bazel-out/darwin-fastbuild/bin/tests/integrationtests/jvm/unused_deps/rt-dep.jar"
        const val RT_JAR_LABEL = "//tests/integrationtests/jvm/unused_deps:rt-dep"
        const val RT_JAR_2 = "bazel-out/darwin-fastbuild/bin/tests/integrationtests/jvm/unused_deps/rt-dep.jar"
        const val RT_JAR_LABEL_2 = "//tests/integrationtests/jvm/unused_deps:rt-dep"

        val Flags.debugInfoClassPath: String
            get() = """classpath:
    ${classpath.joinToString("\n\t")}
direct_dependencies:
    ${directDependencies.map { "${it.key}: ${it.value}" }.joinToString("\n\t")}
indirect_dependencies:
    ${indirectDependencies.map { "${it.key}: ${it.value}" }.joinToString("\n\t")}
"""

        class FlagsSubject(metadata: FailureMetadata, private val flags: Flags) : Subject<FlagsSubject, Flags>(metadata, flags) {
            companion object : Subject.Factory<FlagsSubject, Flags> {
                override fun createSubject(metadata: FailureMetadata, actual: Flags): FlagsSubject = FlagsSubject(metadata, actual)
            }

            override fun actualCustomStringRepresentation(): String {
                return flags.debugInfoClassPath
            }

            fun checkAll(block: FlagsSubject.() -> Unit) {
                this.block()
            }

            private fun checkClasspath() = check().withMessage(flags.debugInfoClassPath)

            val indirectDependencies get() = checkClasspath().that(flags.indirectDependencies)
            val directDependencies get() = checkClasspath().that(flags.directDependencies)
            val thatClasspath get() = checkClasspath().that(flags.classpath.asList())
        }
    }

    fun Flags.assertClasspathEntries(msg: String, block: StandardSubjectBuilder.(Flags) -> Unit) {
        assertWithMessage("$msg\n$debugInfoClassPath").also { it.block(this) }
    }

    @Test
    fun testUseAndLink() {
        argMapTestCase("a-use-b-link-b.jar-2.params", "test params file") {
            val flags = Flags(this)
            assertWithMessage("validate regression that duplicate sources are not somehow getting in")
                .that(flags.source!!.toList()).containsNoDuplicates()

            assertThat(flags.classpath.toList()).containsNoDuplicates()

            assertWithMessage("unused deps support should include indirect dependencies on the classpath")
                .that(flags.classpath.toList()).containsAllIn(arrayOf(C_JAR, B_JAR))

            assertThat(flags.directDependencies).containsExactly(B_JAR, B_DEPS_LABEL)

            flags.assertClasspathEntries("indirect dependencies should be present in the classpath") {
                that(it.indirectDependencies).containsEntry(C_JAR, C_JAR_LABEL)
            }

            flags.assertClasspathEntries(
                "direct and transitive runtime dependencies and should be present in the classpath"
            ) {
                that(it.indirectDependencies).containsEntry(RT_JAR, RT_JAR_LABEL)
                that(it.indirectDependencies).containsEntry(RT_JAR_2, RT_JAR_LABEL_2)
            }

//                assertThat(flags.indirectDependencies)

//            assertWithMessage("").about(FlagsSubject).that(flags).checkAll {
//                indirectDependencies.containsEntry(C_JAR, C_JAR_LABEL)
//
//            }


//            assertAbout(FlagsSubject).that(flags).indirectDependencies.


//            flags.validateClasspath("transitive compile dependencies are indirect dependencies") {
//                that(it.indirectDependencies).containsEntry(C_JAR, C_JAR_LABEL)
//            }
//            flags.assertThat(", {indirectDependencies})
//            assertWithMessage(
//
//            ).that(flags.indirectDependencies).containsEntry(C_JAR, C_JAR_LABEL)

//            assertWithMessage(
//                "unused deps support should consider runtime dependencies as an indirect dependency"
//            ).that(flags.indirectDependencies).containsEntry(RT_JAR, RT_JAR_LABEL)
        }
    }
}