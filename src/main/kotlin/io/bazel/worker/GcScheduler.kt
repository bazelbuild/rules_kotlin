package src.main.kotlin.io.bazel.worker

/** GcScheduler for invoking garbage collection in a persistent worker. */
fun interface GcScheduler {
  fun maybePerformGc()
}
