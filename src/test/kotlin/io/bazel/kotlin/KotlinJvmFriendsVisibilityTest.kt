package io.bazel.kotlin

import test.Service
import test.DEFAULT_FRIEND

/**
 * This test validates that friend visibility is working. Services and DEFAULT_FRIEND are internal another compilation
 * unit.
 */
class KotlinJvmFriendsVisibilityTest {
    val service: Service = Service()

    @org.junit.Test
    fun testCanAccessFriendMembers() {
        println(service.value)
        println(service.iSayHolla(DEFAULT_FRIEND))
    }
}