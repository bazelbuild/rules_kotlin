package io.bazel.ruleskotlin.integrationtests.jvm.unused_deps

class `A-use-B-and-C-link-b-no-link-c.kt` {
    fun doWork() {
        println("I use B and C, I don't link against C directly")
    }
}

