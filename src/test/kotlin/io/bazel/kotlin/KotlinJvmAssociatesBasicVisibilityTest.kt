package io.bazel.kotlin

import test.DEFAULT_FRIEND
import test.Service

/**
 * This test validates that internal visibility is working. Services and DEFAULT_FRIEND are
 * internal another compilation unit.
 */
class KotlinJvmAssociatesBasicVisibilityTest {
  val service: Service = Service()

  @org.junit.Test
  fun testCanAccessFriendMembers() {
    println(service.value)
    println(service.iSayHolla(DEFAULT_FRIEND))
  }
}
