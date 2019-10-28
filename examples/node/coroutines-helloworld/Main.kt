package trivial

import kotlinx.coroutines.*

val scope = CoroutineScope(Dispatchers.Default)

suspend fun main(vararg args: String) {
    val job = scope.launch {
        delay(1000)
        println("Hello world!")
    }

    job.join()
}
