package x

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking

/**
 * Example using kotlin coroutines (an external library imported via bazel-deps)
 */
fun main(args: Array<String>) {
  runBlocking {
    val msg = async {
      greet()
    }
    println(msg.await())
  }
}

suspend fun greet(): String {
  delay(100)
  return "a greeting"
}
