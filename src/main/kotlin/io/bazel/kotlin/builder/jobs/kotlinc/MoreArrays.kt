package io.bazel.kotlin.builder.jobs.jvm

object MoreArrays {
  private const val EMPTY = ""
  fun concatenate(thingOne : Array<String>, thingTwo: Array<String>) : Array<String> {
    val all = Array(thingOne.size + thingTwo.size) { EMPTY }
    System.arraycopy(thingOne, 0, all, 0, thingOne.size)
    System.arraycopy(thingTwo, 0, all, thingOne.size, thingTwo.size)
    return all
  }
}
