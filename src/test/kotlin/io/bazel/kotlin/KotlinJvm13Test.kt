package io.bazel.kotlin

import org.junit.Test

class KotlinJvm13Test {
    @Test fun testFoo() {
        when (val foo = "TryIng Mixed Case".toLowerCase()) {
            "trying mixed case" -> { println(foo) }
            else -> {}
        }
    }
}
